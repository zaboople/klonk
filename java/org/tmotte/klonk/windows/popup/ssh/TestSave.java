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
import org.tmotte.klonk.windows.popup.KAlert;

class TestSave {

  private static void testSimple(final String[] args) throws Exception {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          final SSHConnections conns=new SSHConnections();
          JFrame m=PopupTestContext.makeMainFrame();
          conns.withLogin(
            new SSHLogin(m, new KAlert(m))
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