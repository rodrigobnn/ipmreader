package br.com.b2.clearing.controllers;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import org.jpos.iso.ISOBasePackager;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOField;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOMsgFieldPackager;
import org.jpos.iso.packager.GenericPackager;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class FileController {
	public static void main(String[] args) throws IOException, ISOException {
		RandomAccessFile file = new RandomAccessFile("files/08_TransacaoQuasiCashAvista.ipm", "r");
		B2GenericPackager packager = new B2GenericPackager("files/ISO8583_format.xml");
		packager.setHeaderLength(4);
		ISOMsg msg;
		
		int fileSize = (int) file.length();

		int numOfReadBytes = 0;
		int totalRemovido = 0;
		
		byte[] byteArray = new byte[fileSize];

		Path path = Paths.get("files/resultado" + System.currentTimeMillis() + ".json");
		JsonArray json = new JsonArray();

		while (numOfReadBytes < fileSize) {

			System.out.println("File size:" + fileSize + ", read:" + numOfReadBytes);
			msg = new ISOMsg();

			msg.setPackager(packager);

			file.seek(numOfReadBytes+totalRemovido);

			file.read(byteArray);

			int tamanho = msg.unpack(byteArray);
			totalRemovido += tamanho;

			logISOMsg(msg);

			JsonObject mensagem = createIsoJson(msg);
			json.add(mensagem);

			numOfReadBytes += msg.pack().length;
			
			System.out.println(tamanho+" - "+numOfReadBytes);
			
			if(tamanho == 2147483647) {
				break;
			}
			
			
			

		}
		
		Files.write(path, json.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		file.close();

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
			mensagem.addProperty("BitMap value",
					msg.getComponent(-1).getValue().toString().replace("{", "[").replace("}", "]"));
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
						mensagem.addProperty(msg.getPackager().getFieldDescription(msg, i), msg.getString(i));
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
