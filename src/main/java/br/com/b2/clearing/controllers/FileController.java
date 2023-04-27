package br.com.b2.clearing.controllers;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

		Set<String> arquivos = new HashSet<>();
		//arquivos.add("Pagamento Nacional.ipm");

		try (Stream<Path> stream = Files.list(Paths.get("files"))) {
			arquivos = stream.filter(file -> !Files.isDirectory(file) && file.getFileName().toString().endsWith("ipm")).map(Path::getFileName).map(Path::toString)
					.collect(Collectors.toSet());
		}

		for (String arquivo : arquivos) {
			
			System.out.println("ARQUIVO PROCESSANDO - INÍCIO : "+arquivo);
			
			RandomAccessFile file = new RandomAccessFile("files/"+arquivo, "r");
			B2GenericPackager packager = new B2GenericPackager("files/ISO8583_format.xml");
			packager.setHeaderLength(4);
			ISOMsg msg;
			
			int fileSize = (int) file.length();

			int numOfReadBytes = 0;
			int totalRemovido = 0;

			byte[] byteArray = new byte[fileSize];

			Path path = Paths.get("files/json/resultado_" + arquivo.split(".ipm")[0] + "_" + System.currentTimeMillis() + ".json");
			JsonArray json = new JsonArray();
			
			
			/*
			 * O bloco abaixo retira os bytes de complemento, uma vez que a Mastercard usa 1014 blocked file format
			 * */
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			
			for(int i=1; i<=fileSize; i++) {
				byte b = file.readByte();
				int mod = i%1014;
				if(i<1000 || (mod != 0 && mod != 1013)) {
					baos.write(b);
				} 
			}
			
			byteArray = baos.toByteArray();
			
			while (numOfReadBytes < fileSize) {

				msg = new ISOMsg(); 
				msg.setPackager(packager);

				int tamanho = msg.unpack(Arrays.copyOfRange(byteArray, numOfReadBytes + totalRemovido, byteArray.length));
				totalRemovido += tamanho;

			   // logISOMsg(msg);

				JsonObject mensagem = createIsoJson(msg);
				json.add(mensagem);

				numOfReadBytes += msg.pack().length;

				if (tamanho == 2147483647) {
					break;
				}
			}

			Files.write(path, json.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
			file.close();
			
			System.out.println("ARQUIVO PROCESSANDO - FIM : "+arquivo);
			System.out.println();
		}
	}

	/*
	 * Método acessório para print de transação, não irá ser disponibilizado
	 * */
	private static void logISOMsg(ISOMsg msg) {
		System.out.println("----ISO MESSAGE-----");

		try {
			System.out.println("MTI : " + msg.getMTI());
			System.out.println("BitMap value: " + msg.getComponent(-1).getValue());
			for (int i = 1; i <= msg.getMaxField(); i++) {
				if (msg.hasField(i)) {
					if(msg.getComponent(i) instanceof B2IsoMsg) {
						System.out.println(((B2IsoMsg)msg.getComponent(i)).getB2FieldDescription());
						Map<String, String> campos = ((B2IsoMsg)msg.getComponent(i)).getB2Campos();
						
						for(String key : campos.keySet()) {
							System.out.println("\t" + key + " : " + campos.get(key));
						}
						
					}					
					else if (msg.getComponent(i).getChildren().size() > 0) {
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
			mensagem.addProperty("mti", msg.getMTI());
			mensagem.addProperty("bitmap",
					msg.getComponent(-1).getValue().toString().replace("{", "[").replace("}", "]"));
			
			JsonArray deArray = new JsonArray();
			
			for (int i = 1; i <= msg.getMaxField(); i++) {
				JsonObject deItem = new JsonObject();
				if (msg.hasField(i)) {
					if (msg.getComponent(i).getChildren().size() > 0) {
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
						deItem.add(msg.getPackager().getFieldDescription(msg, i), array);
					} else {
						deItem.addProperty(msg.getPackager().getFieldDescription(msg, i), msg.getString(i));
					}
					deArray.add(deItem);
				}
			}
			
			mensagem.add("de", deArray);

		} catch (ISOException e) {
			e.printStackTrace();
		} 
		
		return mensagem;
	}
}
