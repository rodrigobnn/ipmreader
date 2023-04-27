package br.com.b2.clearing.controllers;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Map;

import org.jpos.iso.ISOComponent;
import org.jpos.iso.ISOException;

public class B2IsoMsg extends ISOComponent {

	private String b2FieldDescription;
	private int fieldNumber;
	private Map<String, String> b2Campos;
	private Object value;
	private byte[] b;

	public String getB2FieldDescription() {
		return b2FieldDescription;
	}

	public void setB2FieldDescription(String b2FieldDescription) {
		this.b2FieldDescription = b2FieldDescription;
	}

	public Map<String, String> getB2Campos() {
		return b2Campos;
	}

	public void setB2Campos(Map<String, String> campos) {
		this.b2Campos = campos;
	}

	@Override
	public Object getKey() {
		return fieldNumber;
	}
	
	@Override
    public void setFieldNumber (int fieldNumber) {
        this.fieldNumber = fieldNumber;
    }
	
	/**
     * valid on Leafs only.
     * @return object representing the field value
     * @exception ISOException
     */
	@Override
    public void setValue(Object value) throws ISOException {
        this.value = value;
    }
    /**
     * get Value as bytes (when possible)
     * @return byte[] representing this field
     * @exception ISOException
     */
    public void setBytes(byte[] x) throws ISOException {
        this.b = x;
    }
    
    @Override
    public byte[] getBytes() throws ISOException {
        return b;
    }
    
    @Override
    public Object getValue() throws ISOException {
        return value;
    }

	@Override
	public int getFieldNumber() {
		// TODO Auto-generated method stub
		return 0;
	}

	

	@Override
	public byte[] pack() throws ISOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int unpack(byte[] b) throws ISOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void dump(PrintStream p, String indent) {
		// TODO Auto-generated method stub

	}

	@Override
	public void unpack(InputStream in) throws IOException, ISOException {
		// TODO Auto-generated method stub

	}

}
