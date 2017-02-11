package org.tmotte.klonk.io;
import java.io.Writer;
import java.nio.charset.Charset;
import java.security.AlgorithmParameters;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Writes
 * <p> 1) A plaintext flag indicating file encryption, plus newline
 * <p> 2) The key size
 * <p> 3) The cypher parameters, which includes a salt, plus newline
 * <p> 4) A series of lines: Each line is a buffered chunk of the file, no larger than
 *        our specified buffer length. The encryption bytes are base64-encoded.
 *        Thus each line should be decryptable by itself.
 * <p>
 * Note that we only generate our salted key once, instead of once per buffer chunk. As
 * far as we know this is secure enough.
 */
public class EncryptionStream implements AppendableFlushableCloseable {

  /////////////////////
  // True constants: //
  /////////////////////


  final static String pbeAlgorithm="PBEWithHmacSHA256AndAES_";
  final static Charset utf8=Charset.forName("UTF-8");
  final static int paramsLength=104;
  final static Base64.Decoder base64Decoder=Base64.getDecoder();

  public final static String encryptionFlag=
    "<<[*******]>> // ENCRYPTED BY KLONK // <<[*******]>>";
  private final static int iterations=1024*1024;
  private final static int bufferSize=1024;
  private final static Base64.Encoder base64Encoder=Base64.getEncoder();

  /////////////////////////
  // Instance variables: //
  /////////////////////////

  private final Writer writer;
  private final Cipher cipher;
  private final StringBuilder buffer=new StringBuilder();

  public static SecretKey getSecretKey(char[] pass, int keySize, String algorithm) throws Exception {
    byte[] salt=new byte[8];
    new SecureRandom().nextBytes(salt);
    return SecretKeyFactory.getInstance(algorithm)
      .generateSecret(
        new PBEKeySpec(pass, salt, iterations, keySize)
      );
  }

  public static boolean matches(String firstLine) {
    return firstLine.trim().equals(encryptionFlag);
  }

  public EncryptionStream(Writer writer, int keySize, char[] pass) throws Exception {
    this.writer=writer;
    String algorithm=pbeAlgorithm+keySize;
    this.cipher = Cipher.getInstance(algorithm);
    cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(pass, keySize, algorithm));
    byte[] params = cipher.getParameters().getEncoded();
    if (params.length != paramsLength)
      throw new Exception("Cannot encrypt because params are wrong "+params.length);
    writer
      .append(encryptionFlag).append("\n")
      .append(String.valueOf(keySize)).append("\n")
      .append(base64Encoder.encodeToString(params)).append("\n")
      .flush();
  }

  public void append(CharSequence data) throws Exception {
    buffer.append(data);
    flush(false);
  }
  public void close() {
    try {
      flush(true);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  public void flush() throws java.io.IOException {
    flush(false);
  }

  private void flush(boolean force) {
    try {
      if (force || buffer.length()>bufferSize){
        writer.append(
          base64Encoder.encodeToString(
            cipher.doFinal(
              buffer.toString().getBytes(utf8)
            )
          )
        ).append("\n");
        buffer.setLength(0);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}