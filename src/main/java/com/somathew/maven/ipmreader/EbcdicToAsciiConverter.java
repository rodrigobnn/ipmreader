package com.somathew.maven.ipmreader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class EbcdicToAsciiConverter {
	public static void main(String[] args) {
		try {
			// Cria um charset para EBCDIC
			Charset ebcdicCharset = Charset.forName("IBM1047");

			// Cria um charset para ASCII
			Charset asciiCharset = Charset.forName("US-ASCII");

			// Cria um decoder EBCDIC para converter bytes em caracteres
			CharsetDecoder ebcdicDecoder = ebcdicCharset.newDecoder();

			// Cria um encoder ASCII para converter caracteres em bytes
			CharsetEncoder asciiEncoder = asciiCharset.newEncoder();

			File file = new File("MarcusTeste.ipm");
			byte[] ebcdicText = new byte[(int) file.length()];
			InputStream in = new FileInputStream(file);
			in.read(ebcdicText);
			in.close();

			// Texto em EBCDIC que será convertido
//        byte[] ebcdicText = {(byte) 0xC1, (byte) 0xC2, (byte) 0xC3, (byte) 0xC4};

			// Decodifica o texto EBCDIC em caracteres
			CharBuffer ebcdicChars = ebcdicDecoder.decode(ByteBuffer.wrap(ebcdicText));

			// Codifica os caracteres em ASCII
			ByteBuffer asciiBytes = asciiEncoder.encode(ebcdicChars);

			Path path = Paths.get("resultado.ipm");
			Files.write(path, asciiBytes.array());

			// Imprime o resultado em ASCII
			System.out.println(new String(asciiBytes.array(), asciiCharset));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
