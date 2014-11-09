package org.tmotte.klonk.windows.popup.ssh;
import java.io.File;
import javax.swing.JFrame;
import org.tmotte.klonk.config.PopupTestContext;
import org.tmotte.klonk.config.PopupTestContext;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.klonk.ssh.SSH;
import org.tmotte.klonk.ssh.SSHConnections;
import org.tmotte.klonk.ssh.SSHExec;
import org.tmotte.klonk.ssh.SSHFile;
import org.tmotte.klonk.windows.popup.SSHLogin;

class TestSave {

  private static void testSimple(String[] args) throws Exception {
    final SSHConnections conns=new SSHConnections();
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          JFrame m=PopupTestContext.makeMainFrame();
          conns.withLogin(
            new SSHLogin(
              m, 
              new Setter<String>(){
                public void set(String s) {System.err.println("Message: "+s);}
              }
            )
          );
          org.tmotte.klonk.windows.popup.FileDialogWrapper fdw=
            new org.tmotte.klonk.windows.popup.FileDialogWrapper(
              PopupTestContext.makeMainFrame(),
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