package org.tmotte.klonk.io;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import javax.swing.JTextArea;
import javax.swing.text.Document;
import org.tmotte.common.text.StringChunker;
import org.tmotte.klonk.config.LineDelimiterOptions;


public class KFileIO {
  private static int bufSize=1024*64;
  private static LineDelimiterOptions k=new LineDelimiterOptions();
  static byte[] utf8BOM   =makeByteArray(0xEF, 0xBB, 0xBF),
                utf16BEBOM=makeByteArray(0xFE, 0xFF),
                utf16LEBOM=makeByteArray(0xFF, 0xFE);
  private static byte[] makeByteArray(int... vals) {
    byte[] r=new byte[vals.length];
    for (int i=0; i<vals.length; i++)
      r[i]=(byte)vals[i];
    return r;
  }


  ///////////
  // LOAD: //
  ///////////
  
  public static FileMetaData load(JTextArea jta, File file) throws Exception {
    return load(jta.getDocument(), file);
  }
  
  public static FileMetaData load(Document doc, File file) throws Exception {

    //Initialize:
    doc.remove(0, doc.getLength());
    char[] readBuffer=new char[bufSize];
    String delimiter=null;
    StringChunker ch=new StringChunker().setRegex(k.pattern);
    StringBuilder buffBuffer=new StringBuilder(bufSize+1024);
    int charsRead=0,
        docPos=0;
    boolean endsWithCR=false,
            tabFound=false, 
            tabSearchOn=true;//because first line of file might start with tab
    FileMetaData fmData=new FileMetaData();

    //Read file and try to get delimiter:
    getEncoding(file, fmData);
    try (
        InputStream istrm=new FileInputStream(file);
        InputStreamReader br=new InputStreamReader(istrm, fmData.encoding);
      ) {
      if (fmData.readOffset>0)
        istrm.skip(fmData.readOffset);
      while ((charsRead=br.read(readBuffer, 0, readBuffer.length))>0){
        String s=new String(readBuffer, 0, charsRead);

        //See if we started in between a CR & LF, and also
        //if we can determine our delimiter:
        boolean badBreak=endsWithCR && s.startsWith(k.LFs);
        endsWithCR=s.endsWith(k.CRs);
        if (delimiter==null){
          if (badBreak)
            delimiter=k.CRLFs;
          else
          if (endsWithCR)
            delimiter=LineDelimiterOptions.CRs;
          else
            delimiter=LineDelimiterOptions.detect(s);
        }
        if (badBreak)
          s=s.substring(1);

        //Now loop thru lines, inserting standard LF between,
        //and see if any line starts with a tab character:
        ch.reset(s);
        while (ch.find()){
          String toInsert=ch.getUpTo();
          tabFound|=tabSearchOn && toInsert.startsWith("\t");
          if (!"".equals(toInsert))
            docPos=insertString(doc, buffBuffer, docPos, toInsert);
          buffBuffer.append(k.LFs);
          tabSearchOn=!tabFound;
        }
        if (!ch.finished()){
          String rest=ch.getRest();
          docPos=insertString(doc, buffBuffer, docPos, rest);
          tabFound|=tabSearchOn && rest.startsWith("\t");
        }
        tabSearchOn=!tabFound;
      }
    }  
    //Technically could happen:
    if (buffBuffer.length()>0)
      doc.insertString(docPos, buffBuffer.toString(), null);
    fmData.delimiter=delimiter;
    fmData.hasTabs=tabFound;  
    return fmData;
  }
  private static int insertString(Document doc, StringBuilder sb, int docPos, String str) throws Exception {
    sb.append(str);
    if (sb.length()>bufSize){
      doc.insertString(docPos, sb.toString(), null);
      int result=docPos+sb.length();
      sb.setLength(0);
      return result;
    }
    return docPos;
  }
  private static void getEncoding(File file, FileMetaData fmd) throws Exception {
    try (
        InputStream istrm=new FileInputStream(file);
      ){
      fmd.encodingNeedsBOM=true;//Default turned off at bottom
      byte[] bom=new byte[4];
      istrm.read(bom);
      if (checkBOM(bom, utf8BOM)) {
        fmd.encoding=fmd.UTF8;
        fmd.readOffset=3;
      }
      else
      if (checkBOM(bom, utf16BEBOM)) {
        fmd.encoding=fmd.UTF16BE;
        fmd.readOffset=2;
      }
      else
      if (checkBOM(bom, utf16LEBOM)) {
        fmd.encoding=fmd.UTF16LE;
        fmd.readOffset=2;
      }
      else {
        fmd.encoding=fmd.UTF8;
        fmd.readOffset=0;
        fmd.encodingNeedsBOM=false;
      }
    }
  }
  private static boolean checkBOM(byte[] input, byte[] check) {
    for (int i=0; i<check.length; i++)
      if (input[i]!=check[i])
        return false;
    return true;
  }
  
  private static boolean checkUnsigned(byte[] b, int index, int val) {
    //Leaving this around because it's interesting. When you use values like 0xABCD etc.,
    //those are treated as signed integer. So we do this to make a byte value comparable to int.
    return (b[index] & 0xFF)==val;
  }

  ///////////
  // SAVE: //
  ///////////
  
    
  public static void save(
      JTextArea jta, File file, 
      String lineBreaker, String encoding, boolean needsBOM
    ) throws Exception {
    save(jta.getDocument(), file, lineBreaker, encoding, needsBOM);
  }
  public static void save(
      Document doc, File file, String lineBreaker, String encoding, boolean needsBOM
    ) throws Exception {
    //Note that we do a lot of silliness with linebreaks even though normally
    //the editor will have LF's everywhere. Not sure however what it will do in
    //copy/paste operation, so we're being extra careful, and anyhow, it's fast enough.
    final boolean isCRLF=lineBreaker.equals(k.CRLFs);
    int docLen=doc.getLength();
    StringChunker ch=new StringChunker().setRegex(k.pattern);
    try (
        OutputStream os=new FileOutputStream(file);
        OutputStreamWriter fw=new OutputStreamWriter(os, encoding);
        PrintWriter pw=new PrintWriter(fw);
      ) {
      if (needsBOM) {
        if (encoding.equals(FileMetaData.UTF16BE))
          os.write(utf16BEBOM);
        else
        if (encoding.equals(FileMetaData.UTF16LE))
          os.write(utf16LEBOM);
        else
        if (encoding.equals(FileMetaData.UTF8))
          os.write(utf8BOM);
      }

      int i=0; 
      boolean endsWithCR=false;
      while (i<docLen){
        int nexty=Math.min(docLen-i, bufSize);
        String s=doc.getText(i, nexty);
        ch.reset(s);

        //Take care of CRLF across buffer boundaries:
        if (isCRLF){
          if (endsWithCR && s.startsWith(k.LFs)) 
            s=s.substring(1);
          endsWithCR=s.endsWith(k.CRs);
        }
        
        //Walk string from linebreak to linebreak and print in between:
        while (ch.find()){
          String upTo=ch.getUpTo();
          if (upTo!=null)
            pw.append(upTo);
          pw.append(lineBreaker);
        }
        if (!ch.finished())
          pw.append(ch.getRest());
        i+=nexty;
        pw.flush();  
        fw.flush();
      }
    } 
  }
  
}