package com.somathew.maven.ipmreader;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Calendar;
import java.util.Map;

import org.jpos.iso.ISOBasePackager;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOField;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOMsgFieldPackager;
import org.jpos.iso.packager.GenericPackager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;


public class App {

//	public static void main(String[] args) throws IOException, ISOException {
//		File file = new File("24-E0087199.D20210813.T180627.INTERT120.A001");
//		byte[] myIPMFileBytes = new byte[(int) file.length()];
//		InputStream in = new FileInputStream(file);
//		in.read(myIPMFileBytes);
//		in.close();
//		
//		ISOMsg message = new ISOMsg();
//		message.setPackager(new GenericPackager("ISO8583_format.xml"));
//		message.unpack(myIPMFileBytes);
//
//		for (Entry<Integer, ISOComponent> transaction : ((Map<Integer, ISOComponent>) message.getChildren()).entrySet()) {
//		    System.out.println(MessageFormat.format("{0} : {1}", transaction.getKey(), transaction.getValue().getValue()));
//		}
//		
//	}

	public static void main(String[] args) throws IOException, ISOException {
		// RandomAccessFile file = new
		// RandomAccessFile("C:\\Users\\somathew\\Documents\\Sprint work\\Sprint
		// 124\\FirstPresentment-AllElements-1Txn-1LogicalFile.ipm", "r");
		RandomAccessFile file = new RandomAccessFile("adaptado.ipm", "r");
//		RandomAccessFile file = new RandomAccessFile("newResult.ipm", "r");
		GenericPackager packager = new GenericPackager("ISO8583_format.xml");
		packager.setHeaderLength(4);
		ISOMsg msg;

		int fileSize = (int) file.length();

		int numOfReadBytes = 0;

		byte[] byteArray = new byte[fileSize];
		
		Path path = Paths.get("resultado"+System.currentTimeMillis()+".json");
		JsonArray json = new JsonArray();

		while (numOfReadBytes < fileSize) {

			System.out.println("File size:" + fileSize + ", read:" + numOfReadBytes);
			msg = new ISOMsg();

			msg.setPackager(packager);

			file.seek(numOfReadBytes);

			file.read(byteArray);

//			for(int i=0; i < byteArray.length; i++) {
//				System.out.print((char)byteArray[i]);
//			}

			// byteArray =
			// "0200B2200000001000000000000000800000201234000000010000011072218012345606A5DFGR021ABCDEFGHIJ
			// 1234567890".getBytes();

//			for (int i = 8; i < 24; i++) {
//				System.out.println(String.format("%02x", byteArray[i]));
//	            // upper case
//	            // result.append(String.format("%02X", aByte));
//	        }

			msg.unpack(byteArray);

			logISOMsg(msg);

			
			
			JsonObject mensagem = createIsoJson(msg);
			json.add(mensagem);
			
			

			numOfReadBytes += msg.pack().length;

		}
		
		Files.write(path, json.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);

	}

	private static void logISOMsg(ISOMsg msg) {
		System.out.println("----ISO MESSAGE-----");

		try {
			System.out.println("MTI : " + msg.getMTI());
			System.out.println("BitMap value: " + msg.getComponent(-1).getValue());
			for (int i = 1; i <= msg.getMaxField(); i++) {
				if (msg.hasField(i)) {
					// if there are children
					if (msg.getComponent(i).getChildren().size() > 0) {

						System.out.println(msg.getPackager().getFieldDescription(msg, i));
						Map<Integer, ISOField> children = msg.getComponent(i).getChildren();
						for (Integer j : children.keySet()) {
							String description = ((ISOBasePackager) ((ISOMsgFieldPackager) ((GenericPackager) msg
									.getPackager()).getFieldPackager(i)).getISOMsgPackager()).getFieldPackager(j)
									.getDescription();
							System.out.println("\t" + description + " : " + children.get(j).getValue());
						}
					} else {
						System.out.println(msg.getPackager().getFieldDescription(msg, i) + " : " + msg.getString(i));
					}

				}
			}
		} catch (ISOException e) {
			e.printStackTrace();
		} finally {
			System.out.println("--------------------");
		}

	}

	private static JsonObject createIsoJson(ISOMsg msg) {

		JsonObject mensagem = new JsonObject();

		try {
			mensagem.addProperty("MTI", msg.getMTI());
			mensagem.addProperty("BitMap value",msg.getComponent(-1).getValue().toString().replace("{", "[").replace("}", "]"));
			for (int i = 1; i <= msg.getMaxField(); i++) {
				if (msg.hasField(i)) {
					// if there are children
					if (msg.getComponent(i).getChildren().size() > 0) {
						System.out.println();
						JsonArray array = new JsonArray();
						Map<Integer, ISOField> children = msg.getComponent(i).getChildren();
						for (Integer j : children.keySet()) {
							String description = ((ISOBasePackager) ((ISOMsgFieldPackager) ((GenericPackager) msg
									.getPackager()).getFieldPackager(i)).getISOMsgPackager()).getFieldPackager(j)
									.getDescription();
							JsonObject item = new JsonObject();
							item.addProperty(description, children.get(j).getValue().toString());
							array.add(item);
						}
						mensagem.add(msg.getPackager().getFieldDescription(msg, i), array);
					} else {
						mensagem.addProperty(msg.getPackager().getFieldDescription(msg, i),msg.getString(i));
					}

				}
			}
			
		} catch (ISOException e) {
			e.printStackTrace();
		} finally {

		}
		return mensagem;
	}

}
