package org.tmotte.klonk.windows.popup.ssh;
import java.io.File;
import javax.swing.JFrame;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.klonk.config.option.SSHOptions;
import org.tmotte.klonk.ssh.SSH;
import org.tmotte.klonk.ssh.SSHConnections;
import org.tmotte.klonk.ssh.SSHExec;
import org.tmotte.klonk.ssh.SSHFile;
import org.tmotte.klonk.windows.popup.KAlert;
import org.tmotte.klonk.windows.popup.PopupTestContext;

class TestSave {

  private static void testSimple(final String[] args) throws Exception {
    PopupTestContext ptc=new PopupTestContext();
    final SSHOptions options=ptc.getPersist().getSSHOptions();
    
    
    Thread.setDefaultUncaughtExceptionHandler( 
        new Thread.UncaughtExceptionHandler() {
          public void uncaughtException(Thread t, Throwable e){
            if (e instanceof SSHFileDialogNoFileException)
              System.err.println("OK no biggie "+e);
            else
              e.printStackTrace();
          }
        }
      );
  
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          JFrame m=PopupTestContext.makeMainFrame();
          KAlert alerter=new KAlert(m);
          SSHConnections conns=new SSHConnections(alerter)
            .withKnown(options.getKnownHostsFilename())
            .withPrivateKeys(options.getPrivateKeysFilename())
            .withLogin(
              new SSHLogin(m, alerter)
            );
          org.tmotte.klonk.windows.popup.FileDialogWrapper fdw=
            new org.tmotte.klonk.windows.popup.FileDialogWrapper(
              m,
              new SSHFileSystemView(conns),
              new SSHFileView()
            );
          File picked=fdw.show(true, null, null);
          System.out.println("RESULT: "+
            picked+" "+(picked==null ?"" :picked.getClass())
          );
        } catch (Exception e) {
          e.printStackTrace();
        }
      }//run()
    });
  }
  public static void main(String[] args) throws Exception {
    testSimple(args);
  }
}