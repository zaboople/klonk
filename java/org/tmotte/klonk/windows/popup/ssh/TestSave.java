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
    
    //This is obnoxious but we have to do it when we get a null file:
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

    final PopupTestContext ptc=new PopupTestContext();
    final SSHOptions options=ptc.getPersist().getSSHOptions();
    final String path=args.length>0
      ?args[0]
      :null;
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          JFrame m=ptc.getMainFrame();
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
          {
            File dir=null, file=null;
            if (path!=null) {
              SSHFile temp=conns.getFile(path);
              if (temp.isDirectory())
                dir=temp;
              else
                file=temp;
            }
            File picked=fdw.show(true, file, dir);
            System.out.println("RESULT: "+
              picked+" "+(picked==null ?"" :picked.getClass())
            );
          }
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