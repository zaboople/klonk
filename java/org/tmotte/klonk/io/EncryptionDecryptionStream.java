package org.tmotte.klonk.io;
import java.io.BufferedReader;
import java.io.Writer;
import java.security.AlgorithmParameters;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;


public class EncryptionDecryptionStream {

  private final Cipher cipher;

  public EncryptionDecryptionStream(char[] pass, CharSequence keySize, CharSequence paramsBase64) throws Exception {
    this(pass, Integer.parseInt(keySize.toString()), paramsBase64);
  }

  public EncryptionDecryptionStream(char[] pass, int keySize, CharSequence paramsBase64) throws Exception {
    String algorithm=EncryptionStream.pbeAlgorithm+keySize;
    AlgorithmParameters algParams=AlgorithmParameters.getInstance(algorithm);
    {
      byte[] params=EncryptionStream.base64Decoder.decode(
        paramsBase64.toString().getBytes(EncryptionStream.utf8)
      );
      if (params.length != EncryptionStream.paramsLength)
        throw new Exception(
          "Cannot decrypt because params are wrong: "+params.length+" != "+EncryptionStream.paramsLength
        );
      algParams.init(params);
    }
    cipher=Cipher.getInstance(algorithm);
    cipher.init(
      Cipher.DECRYPT_MODE,
      EncryptionStream.getSecretKey(pass, keySize, algorithm),
      algParams
    );
  }

  public String decrypt(CharSequence base64Data) throws Exception {
    return new String(
      cipher.doFinal(
        EncryptionStream.base64Decoder.decode(
          base64Data.toString().getBytes(EncryptionStream.utf8)
        )
      ),
      EncryptionStream.utf8
    );
  }

  public static void decrypt(BufferedReader reader, char[] password, Appendable output) throws Exception {
    // Get inputs:
    final String
      marker=reader.readLine(),
      keySize=reader.readLine().trim(),
      params=reader.readLine();
    if (!EncryptionStream.matches(marker))
      throw new RuntimeException("Not marker: "+marker);

    // And decrypt:
    EncryptionDecryptionStream inStream=
      new EncryptionDecryptionStream(password, keySize, params);
    String line;
    while ((line=reader.readLine())!=null)
      output.append(inStream.decrypt(line));
  }

  public static void main(String[] args) throws Exception {
    test();
  }

  //////////////
  // TESTING: //
  //////////////

  private static void test() throws Exception {

    // BUILD DATA:
    Writer writer=new java.io.StringWriter();
    char[] password="HelloWorld".toCharArray();
    {
      int keySize=128;
      try (
        EncryptionStream es=new EncryptionStream(writer, keySize, password);
      ) {
        es.append("hi");
        for (int i=0; i<1000; i++)
          es.append((i%10==0 ?"\n" :"")+"yo"+i);
        es.append("DONE");
      }
      System.out.print(writer.toString());
    }
    decrypt(
      new java.io.BufferedReader(
        new java.io.StringReader(writer.toString())
      ),
      password, System.out
    );
    System.out.flush();

  }
}