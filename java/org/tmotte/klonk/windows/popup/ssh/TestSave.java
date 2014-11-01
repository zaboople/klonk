package org.tmotte.klonk.windows.popup.ssh;
import java.io.File;
import org.tmotte.klonk.config.PopupTestContext;
import org.tmotte.klonk.ssh.SSH;
import org.tmotte.klonk.ssh.SSHConnections;
import org.tmotte.klonk.ssh.SSHExec;
import org.tmotte.klonk.ssh.SSHFile;

class TestSave {

  private static void testSimple(String[] args) throws Exception {
    final SSH ssh=SSH.cmdLine(args);
    final SSHConnections conns=new SSHConnections();
    conns.put(ssh.getUser(), ssh.getHost(), ssh);
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          org.tmotte.klonk.windows.popup.FileDialogWrapper fdw=
            new org.tmotte.klonk.windows.popup.FileDialogWrapper(
              PopupTestContext.makeMainFrame(),
              new SSHFileSystemView(conns),
              new SSHFileView()
            );
          SSHFile file=
            new SSHFile(
              ssh, 
              new SSHFile(
                ssh, 
                new SSHFile(
                  ssh, null, "/", true
                ), 
                "home", 
                true
              ), 
              "zaboople", 
              true
            );
          File picked=fdw.show(true, null, file);
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