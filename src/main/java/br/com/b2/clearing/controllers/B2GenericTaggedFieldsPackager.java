package br.com.b2.clearing.controllers;

import java.util.Arrays;

import org.jpos.iso.ISOComponent;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOFieldPackager;
import org.jpos.iso.ISOUtil;
import org.jpos.iso.TaggedFieldPackagerBase;
import org.jpos.iso.packager.GenericTaggedFieldsPackager;
import org.jpos.util.LogEvent;
import org.jpos.util.Logger;

public class B2GenericTaggedFieldsPackager extends GenericTaggedFieldsPackager{

	public B2GenericTaggedFieldsPackager() throws ISOException {
		super();
	}
	
	
	@Override
    public int unpack(ISOComponent m, byte[] b) throws ISOException {
        LogEvent evt = new LogEvent(this, "unpack");
        ISOFieldPackager[] fields = Arrays.copyOf(fld, fld.length);
        try {
            if (m.getComposite() != m)
                throw new ISOException("Can't call packager on non Composite");
            if (b.length == 0)
                return 0; // nothing to do
            if (logger != null)  // save a few CPU cycle if no logger available
                evt.addMessage(ISOUtil.hexString(b));

            int consumed = 0;
            int maxField = fld.length;
            while (consumed < b.length) {
                for (int i = getFirstField(); i < maxField && consumed < b.length; i++) {
                    if (fields[i] != null) {
                        ISOComponent c = fields[i].createComponent(i);
                        int unpacked = fields[i].unpack(c, b, consumed);
                        consumed = consumed + unpacked;
                        if (unpacked > 0) {
                            if (!(fields[i] instanceof TaggedFieldPackagerBase))
                                fields[i] = null;
                            m.set(c);
                        }
                    }
                }
            }
            if (b.length != consumed) {
                evt.addMessage(
                        "WARNING: unpack len=" + b.length + " consumed=" + consumed);
            }
            return consumed;
        } catch (ISOException e) {
            evt.addMessage(e);
            throw e;
        } catch (Exception e) {
            evt.addMessage(e);
            throw new ISOException(e);
        } finally {
            Logger.log(evt);
        }
    }
	

}
