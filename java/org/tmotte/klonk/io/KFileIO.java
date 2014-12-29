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
import org.tmotte.klonk.config.option.LineDelimiterOptions;
import org.tmotte.klonk.ssh.SSHFile;

public class KFileIO {
  private final static int bufSize=1024*64;
  private final static LineDelimiterOptions delimOpt=new LineDelimiterOptions();
  private final static byte[] 
    utf8BOM   =makeByteArray(0xEF, 0xBB, 0xBF),
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
  
  
  private static InputStream getInputStream(File file) throws Exception {
    final SSHFile ssh=SSHFile.cast(file);
    return ssh==null
      ?new FileInputStream(file)
      :ssh.getInputStream();
  }

  public static FileMetaData load(JTextArea jta, final File file) throws Exception {
    return load(jta.getDocument(), file);
  }

  public static FileMetaData load(Document doc, File file) throws Exception {  

    //Initialize:
    doc.remove(0, doc.getLength());
    char[] readBuffer=new char[bufSize];
    String delimiter=null;
    StringChunker delimChunker=new StringChunker().setRegex(delimOpt.pattern);
    StringBuilder buffBuffer=new StringBuilder(bufSize+1024);
    int charsRead=0,
        docPos=0;
    boolean endsWithCR=false,
            isCRLF=false,
            tabFound=false, 
            tabSearchOn=true;//because first line of file might start with tab

    //Read file and try to get delimiter:
    FileMetaData fmData=null;
    try (
      InputStream istr=getInputStream(file);
      ) {
      fmData=getEncoding(istr);
    }

    try (
        InputStream istrm=getInputStream(file);
        InputStreamReader br=new InputStreamReader(istrm, fmData.encoding);
      ) {
      if (fmData.readOffset>0)
        istrm.skip(fmData.readOffset);
      while ((charsRead=br.read(readBuffer, 0, readBuffer.length))>0){
        String s=new String(readBuffer, 0, charsRead);

        //See if we started in between a CR & LF, and also
        //if we can determine our delimiter:
        boolean badBreak=endsWithCR && s.startsWith(delimOpt.LFs);
        if (delimiter==null || isCRLF)
          endsWithCR=s.endsWith(delimOpt.CRs);
        if (delimiter==null){
          if (badBreak){
            delimiter=delimOpt.CRLFs;
            isCRLF=true;
          }
          else
          if (endsWithCR)
            //Yes if we have an exactly readBuffer.length() block that ends with CR but is really
            //CRLF this will make a mistake but I don't care. Nobody goes that far without a line break.
            delimiter=LineDelimiterOptions.CRs;
          else
            delimiter=LineDelimiterOptions.detect(s);
        }
        if (badBreak)
          s=s.substring(1);

        //Now loop thru lines, inserting standard LF between,
        //and see if any line starts with a tab character:
        delimChunker.reset(s);
        while (delimChunker.find()){
          String toInsert=delimChunker.getUpTo();
          tabFound|=tabSearchOn && toInsert.startsWith("\t");
          if (!"".equals(toInsert))
            docPos=insertString(doc, buffBuffer, docPos, toInsert);
          buffBuffer.append(delimOpt.LFs);
          tabSearchOn=!tabFound;
        }
        if (!delimChunker.finished()){
          String rest=delimChunker.getRest();
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
  
  private static FileMetaData getEncoding(InputStream istrm) throws Exception {
    FileMetaData fmd=new FileMetaData();
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
    return fmd;
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
    final boolean isCRLF=lineBreaker.equals(delimOpt.CRLFs);
    int docLen=doc.getLength();
    StringChunker ch=new StringChunker().setRegex(delimOpt.pattern);
    try (
        OutputStream os=getOutputStream(file);
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
          if (endsWithCR && s.startsWith(delimOpt.LFs)) 
            s=s.substring(1);
          endsWithCR=s.endsWith(delimOpt.CRs);
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
  private static OutputStream getOutputStream(File file) throws Exception {
    SSHFile sshFile=SSHFile.cast(file);
    return sshFile==null
      ?new FileOutputStream(file)
      :sshFile.getOutputStream();
  }
  
}
