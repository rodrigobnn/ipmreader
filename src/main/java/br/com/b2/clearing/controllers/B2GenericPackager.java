package br.com.b2.clearing.controllers;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.BitSet;

import org.jpos.iso.EbcdicPrefixer;
import org.jpos.iso.ISOBinaryField;
import org.jpos.iso.ISOBitMap;
import org.jpos.iso.ISOBitMapPackager;
import org.jpos.iso.ISOComponent;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOFieldPackager;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOMsgFieldPackager;
import org.jpos.iso.ISOPackager;
import org.jpos.iso.ISOSubFieldPackager;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.util.LogEvent;
import org.jpos.util.Logger;

public class B2GenericPackager extends GenericPackager{

	public B2GenericPackager(String filename) throws ISOException {
		super(filename);
	}
	
	@Override
    public int unpack (ISOComponent m, byte[] b) throws ISOException {
        LogEvent evt = logger != null ? new LogEvent (this, "unpack") : null;
        int consumed = 0;
        int removidosTotal = 0;
        boolean endOfFile = false;

        try {
            if (m.getComposite() != m)
                throw new ISOException ("Can't call packager on non Composite");
            if (evt != null)
                evt.addMessage (ISOUtil.hexString (b));


            
            if (m instanceof ISOMsg && headerLength>0)
            {
                byte[] h = new byte[headerLength];
                System.arraycopy(b, 0, h, 0, headerLength);
                ((ISOMsg) m).setHeader(h);
                consumed += headerLength;
            }

            if (!(fld[0] == null) && !(fld[0] instanceof ISOBitMapPackager))
            {
                ISOComponent mti = fld[0].createComponent(0);
                consumed  += fld[0].unpack(mti, b, consumed);
                m.set (mti);
            }

            BitSet bmap = null;
            int bmapBytes= 0;                                   // bitmap length in bytes (usually 8, 16, 24)
            int maxField= fld.length - 1;                       // array length counts position 0!

            if (emitBitMap()) {
                ISOBitMap bitmap = new ISOBitMap (-1);
                consumed += getBitMapfieldPackager().unpack(bitmap,b,consumed);
                bmap = (BitSet) bitmap.getValue();
                bmapBytes= (bmap.length()-1 + 63) >> 6 << 3;
                if (evt != null)
                    evt.addMessage ("<bitmap>"+bmap.toString()+"</bitmap>");
                m.set (bitmap);

                maxField = Math.min(maxField, bmap.length()-1); // bmap.length behaves similarly to fld.length
            }

            for (int i= getFirstField(); i <= maxField; i++) {
                try {
                    if (bmap == null && fld[i] == null)
                        continue;

                    // maxField is computed above as min(fld.length-1, bmap.length()-1), therefore
                    // "maxField > 128" means fld[] has packagers defined above 128, *and*
                    // the bitmap's length is greater than 128 (i.e., a contiguous tertiary bitmap exists).
                    // In this case, bit 65 simply indicates a 3rd bitmap contiguous to the 2nd one.
                    // Therefore, there MUST NOT be a DE-65 with data payload to read.
                    if (maxField > 128 && i==65)
                        continue;   // ignore extended bitmap

                    if (bmap == null || bmap.get(i)) {
                        if (fld[i] == null)
                            throw new ISOException ("field packager '" + i + "' is null");
                        
                        
                        if (fld[i] instanceof ISOMsgFieldPackager) {
                        	ISOPackager msgPackager = ((ISOMsgFieldPackager)fld[i]).getISOMsgPackager();
                        	
                        	ISOBinaryField f = new ISOBinaryField(0);
                            if(msgPackager instanceof ISOSubFieldPackager) {
                            	EbcdicPrefixer prefixer = EbcdicPrefixer.LLL;
                                
                                int len = prefixer.decodeLength(b, consumed);
                                int lenLen = prefixer.getPackedLength();
                                
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                
                                int removidos = 0;
                                
                                for(int x = 0; x < b.length; x++) {
                                	if(x<consumed+lenLen) {
                                		baos.write(b[x]);
                                	} else if(x>consumed+len+removidos+lenLen) {
                                		baos.write(b[x]);
                                	} else if(b[x] == 0) {
                                		removidos++;
                                		removidosTotal++;
                                	} else {
                                		baos.write(b[x]);
                                	}
                                }
                                
                                byte[] newB = baos.toByteArray();
                                b = newB;
                            
                            }
                        }
                        
                        ISOComponent c = fld[i].createComponent(i);
                        consumed += fld[i].unpack(c, b, consumed);
                        
                        if (evt != null)
                            fieldUnpackLogger(evt, i, c, fld);
                        m.set(c);

                        if (i == thirdBitmapField && fld.length > 129 &&          // fld[128] is at pos 129
                            bmapBytes == 16 &&
                            fld[thirdBitmapField] instanceof ISOBitMapPackager)
                        {   // We have a weird case of tertiary bitmap implemented inside a Data Element
                            // instead of being contiguous to the primary and secondary bitmaps.
                            // If enter this "if" it's because we have a proper 16-byte bitmap (1st & 2nd),
                            // but are expecting more than 128 Data Elements according to fld[].
                            // Normally, these kind of ISO8583 implementations have the tertiary bitmap in DE-65,
                            // but sometimes they specify some other DE (given by thirdBitmapField).
                            // We also double check that the DE has been specified as an ISOBitMapPackager in fld[].
                            // By now, the tertiary bitmap has already been unpacked into field `thirdBitmapField`.
                            BitSet bs3rd= (BitSet)((ISOComponent)m.getChildren().get(thirdBitmapField)).getValue();
                            maxField= 128 + (bs3rd.length() - 1);                 // update loop end condition
                            for (int bit= 1; bit <= 64; bit++)
                                bmap.set(bit+128, bs3rd.get(bit));                // extend bmap with new bits above 128
                        }
                        
                        if(i == 24 && c.getValue().toString().equals("695")) {
                        	endOfFile = true;
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
                            String.format("%s unpacking field=%d, consumed=%d",
                            e.getMessage(), i, consumed)
                        );
                    } else {
                        e = new ISOException(
                            String.format("%s (%s) unpacking field=%d, consumed=%d",
                            e.getMessage(), e.getNested().toString(), i, consumed)
                        );
                    }
                    throw e;
                }
                
            } // for each field

            if (evt != null && b.length != consumed) {
                evt.addMessage ("WARNING: unpack len=" +b.length +" consumed=" +consumed);
            }

            //o tamanho máximo de um array em java é 2,147,483,647
            if(endOfFile) {
            	return 2147483647;
            }
            return removidosTotal;
        } catch (ISOException e) {
            if (evt != null)
                evt.addMessage (e);
            throw e;
        } catch (Exception e) {
            if (evt != null)
                evt.addMessage (e);
            throw new ISOException (e.getMessage() + " consumed=" + consumed);
        } finally {
            if (evt != null)
                Logger.log (evt);
        }
    }


	/**
     * Internal helper logging function.
     * Assumes evt is not null.
     */
    private static void fieldUnpackLogger(LogEvent evt, int fldno, ISOComponent c, ISOFieldPackager fld[]) throws ISOException
    {
        evt.addMessage ("<unpack fld=\""+fldno
            +"\" packager=\""+fld[fldno].getClass().getName()+ "\">");
        if (c.getValue() instanceof ISOMsg)
            evt.addMessage (c.getValue());
        else if (c.getValue() instanceof byte[]) {
            evt.addMessage ("  <value type='binary'>"
                +ISOUtil.hexString((byte[]) c.getValue())
                + "</value>");
        }
        else {
            evt.addMessage ("  <value>"+c.getValue()+"</value>");
        }
        evt.addMessage ("</unpack>");
    }
	

}
