package org.tmotte.klonk.io;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.awt.print.Printable;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.Color;
import javax.swing.JTextArea;

public class Printing {
  
  private static Paper lastPaper;
  
  public static boolean print(JTextArea jta) {
    PrinterJob job=PrinterJob.getPrinterJob();
    
    //Switch colors so that we get pure black & white,
    //not greyscale or whatever. We'll switch them back 
    //in a moment:
    Color fore=jta.getForeground(),
          back=jta.getBackground();
    jta.setForeground(Color.BLACK);
    jta.setBackground(Color.WHITE);
    
    try {
      job.setPrintable(jta.getPrintable(null, null));
      PageFormat pf=job.defaultPage();
      
      //Mainly we want some decent margins, and preserve
      //them between prints. Not going to bother persisting
      //them to disk though:
      if (lastPaper==null) {
        lastPaper=pf.getPaper();
        int margin=72/4;
        int margin2=2*margin;
        lastPaper.setImageableArea(
          margin, margin, 
          lastPaper.getWidth()-margin2, 
          lastPaper.getHeight()-margin2
        );
      }
      pf.setPaper(lastPaper);
      
      //Now show dialog and print. Note where we
      //save the selected paper format:
      PageFormat pf2=job.pageDialog(pf);
      if (pf2!=pf) {
        lastPaper=pf2.getPaper();
        job.print();
        return true;
      }
      return false;
      
      //This also seems like a nice option, but it doesn't
      //allow for setting page format.
      //if (job.printDialog())
      //  job.print();
      
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      jta.setForeground(fore);
      jta.setBackground(back);
    }
  }
}