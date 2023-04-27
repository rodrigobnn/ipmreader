package br.com.b2.clearing.controllers;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import org.jpos.iso.EbcdicPrefixer;
import org.jpos.iso.ISOBitMap;
import org.jpos.iso.ISOBitMapPackager;
import org.jpos.iso.ISOComponent;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOFieldPackager;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.LiteralBinaryInterpreter;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.tlv.TLVList;
import org.jpos.tlv.TLVMsg;
import org.jpos.util.LogEvent;
import org.jpos.util.Logger;

public class B2GenericPackager extends GenericPackager {

	public B2GenericPackager(String filename) throws ISOException {
		super(filename);
	}

	@Override
	public int unpack(ISOComponent m, byte[] b) throws ISOException {
		LogEvent evt = logger != null ? new LogEvent(this, "unpack") : null;
		int consumed = 0;
		int removidosTotal = 0;
		boolean endOfFile = false;

		try {
			if (m.getComposite() != m)
				throw new ISOException("Can't call packager on non Composite");
			if (evt != null)
				evt.addMessage(ISOUtil.hexString(b));

			if (m instanceof ISOMsg && headerLength > 0) {
				byte[] h = new byte[headerLength];
				System.arraycopy(b, 0, h, 0, headerLength);
				((ISOMsg) m).setHeader(h);
				consumed += headerLength;
			}

			if (!(fld[0] == null) && !(fld[0] instanceof ISOBitMapPackager)) {
				ISOComponent mti = fld[0].createComponent(0);
				consumed += fld[0].unpack(mti, b, consumed);
				m.set(mti);
			}

			BitSet bmap = null;
			int bmapBytes = 0; // bitmap length in bytes (usually 8, 16, 24)
			int maxField = fld.length - 1; // array length counts position 0!

			if (emitBitMap()) {
				ISOBitMap bitmap = new ISOBitMap(-1);
				consumed += getBitMapfieldPackager().unpack(bitmap, b, consumed);
				bmap = (BitSet) bitmap.getValue();
				bmapBytes = (bmap.length() - 1 + 63) >> 6 << 3;
				if (evt != null)
					evt.addMessage("<bitmap>" + bmap.toString() + "</bitmap>");
				m.set(bitmap);

				maxField = Math.min(maxField, bmap.length() - 1); // bmap.length behaves similarly to fld.length
			}

			for (int i = getFirstField(); i <= maxField; i++) {
				try {
					if (bmap == null && fld[i] == null)
						continue;
					
					if (maxField > 128 && i == 65)
						continue; // ignore extended bitmap

					if (bmap == null || bmap.get(i)) {
						if (fld[i] == null)
							throw new ISOException("field packager '" + i + "' is null");

						if (i == 55) { // DE55
							
							int len = EbcdicPrefixer.LLL.decodeLength(b, consumed);
							int lenLen = EbcdicPrefixer.LLL.getPackedLength();
							byte[] unpacked = LiteralBinaryInterpreter.INSTANCE.uninterpret(b, consumed+lenLen, len );
							
							TLVList tlv = new TLVList();
							
							tlv.unpack(unpacked);
							
							B2IsoMsg c = new B2IsoMsg();
							Map<String, String> campos = new HashMap<String, String>();
							
							B2GenericPackager packager = new B2GenericPackager("files/ISO8583_format.xml");
							
							String fieldsDescription = ((GenericPackager) packager).getFieldPackager(i).getDescription();
							
							c.setB2FieldDescription(fieldsDescription);
							c.setFieldNumber(i);
							c.setValue(Arrays.copyOfRange(b, consumed+lenLen, consumed+lenLen+len));
							c.setBytes(Arrays.copyOfRange(b, consumed+lenLen, consumed+lenLen+len));
						
							for (TLVMsg msg : tlv.getTags()) {
								campos.put(Integer.toHexString(msg.getTag()), ISOUtil.hexString(msg.getValue()));
							}
							
							c.setB2Campos(campos);
							
							consumed+=len;
							consumed+=lenLen;
							
							m.set(c);
							
						} else {
							ISOComponent c = fld[i].createComponent(i);
							consumed += fld[i].unpack(c, b, consumed);
							
							if (evt != null)
								fieldUnpackLogger(evt, i, c, fld);
							m.set(c);

							if (i == thirdBitmapField && fld.length > 129 && // fld[128] is at pos 129
									bmapBytes == 16 && fld[thirdBitmapField] instanceof ISOBitMapPackager) {
																												
								BitSet bs3rd = (BitSet) ((ISOComponent) m.getChildren().get(thirdBitmapField)).getValue();
								maxField = 128 + (bs3rd.length() - 1); // update loop end condition
								for (int bit = 1; bit <= 64; bit++)
									bmap.set(bit + 128, bs3rd.get(bit)); // extend bmap with new bits above 128
							}

							if (i == 24 && c.getValue().toString().equals("695")) {
								endOfFile = true;
							}
						}
					}
				} catch (ISOException e) {
					if (evt != null) {
						evt.addMessage("error unpacking field " + i + " consumed=" + consumed);
						evt.addMessage(e);
					}
					// jPOS-3
					if (e.getNested() == null) {
						e = new ISOException(
								String.format("%s unpacking field=%d, consumed=%d", e.getMessage(), i, consumed));
					} else {
						e = new ISOException(String.format("%s (%s) unpacking field=%d, consumed=%d", e.getMessage(),
								e.getNested().toString(), i, consumed));
					}
					throw e;
				}

			} // for each field

			if (evt != null && b.length != consumed) {
				evt.addMessage("WARNING: unpack len=" + b.length + " consumed=" + consumed);
			}

			// o tamanho máximo de um array em java é 2,147,483,647
			if (endOfFile) {
				return 2147483647;
			}
			return removidosTotal;
		} catch (ISOException e) {
			if (evt != null)
				evt.addMessage(e);
			throw e;
		} catch (Exception e) {
			if (evt != null)
				evt.addMessage(e);
			throw new ISOException(e.getMessage() + " consumed=" + consumed);
		} finally {
			if (evt != null)
				Logger.log(evt);
		}
	}
	
	

	@Override
	public String toString() {
		return "B2GenericPackager [fld=" + Arrays.toString(fld) + ", thirdBitmapField=" + thirdBitmapField + ", logger="
				+ logger + ", realm=" + realm + ", headerLength=" + headerLength + "]";
	}

	/**
	 * Internal helper logging function. Assumes evt is not null.
	 */
	private static void fieldUnpackLogger(LogEvent evt, int fldno, ISOComponent c, ISOFieldPackager fld[])
			throws ISOException {
		evt.addMessage("<unpack fld=\"" + fldno + "\" packager=\"" + fld[fldno].getClass().getName() + "\">");
		if (c.getValue() instanceof ISOMsg)
			evt.addMessage(c.getValue());
		else if (c.getValue() instanceof byte[]) {
			evt.addMessage("  <value type='binary'>" + ISOUtil.hexString((byte[]) c.getValue()) + "</value>");
		} else {
			evt.addMessage("  <value>" + c.getValue() + "</value>");
		}
		evt.addMessage("</unpack>");
	}

}
