package com.somathew.maven.ipmreader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Teste {
	public static void main(String[] args) {

	 File file = new File("MarcusTeste.ipm");
	 File file2 = new File("newResult.ipm");
	  
	 FileConverter converter = new FileConverter(Charset.forName("Cp1047"), Charset.defaultCharset());
	 converter.setFixedLength(-1);
	 try {
		converter.convert(file, file2);
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

//		try {
//			File file = new File("MarcusTeste.ipm");
//			byte[] myIPMFileBytes = new byte[(int) file.length()];
//			InputStream in = new FileInputStream(file);
//			in.read(myIPMFileBytes);
//			in.close();
//
//			byte[] result = new String(myIPMFileBytes, Charset.forName("Cp1047")).getBytes("ASCII");
//			if (result.length != myIPMFileBytes.length) {
//				throw new AssertionError(result.length + "!=" + myIPMFileBytes.length);
//			}
//
//			Path path = Paths.get("resultado.ipm");
//			Files.write(path, result);
//		} catch (Exception e) {
//			System.out.println(e);
//		}
	}

//	  return result;

//	 //Console.WriteLine(Encoding.UTF8.GetString(buff, 0, buff.Length));
//	 Encoding.RegisterProvider(CodePagesEncodingProvider.Instance);
//	 Encoding ascii = Encoding.ASCII;
//	 Encoding ebcdic = Encoding.GetEncoding("IBM037"); Console.WriteLine(""); var x = ebcdic.GetString(buff, 0, buff.Length);
//	 
//	 
//	 
//	 
//	 //Console.WriteLine(ebcdic.GetString(buff, 0, buff.Length));
//	 using (StreamWriter writer = new StreamWriter(@"C:\Users\guilherme.silva\Downloads\IPM Test Files\ResultadoTesteIPMTOTXT.txt", true))
//	 {
//	     writer.WriteLine(x);
//	 } 
//	 //Console.WriteLine("");
//	 //var x = Encoding.Convert(ebcdic, ascii, buff);
//	 ////Retutn Ascii Data
//	 //foreach (var y in x)
//	 //    Console.WriteLine(y + " "); static byte[] ConverteStreamToByteArray(Stream stream)
//	 {
//	     byte[] byteArray = new byte[16 * 1024];
//	     using (MemoryStream mStream = new MemoryStream())
//	     {
//	         int bit;
//	         while ((bit = stream.Read(byteArray, 0, byteArray.Length)) > 0)
//	         {
//	             mStream.Write(byteArray, 0, bit);
//	         }
//	         return mStream.ToArray();
//	     }
//	 }
//}
}
