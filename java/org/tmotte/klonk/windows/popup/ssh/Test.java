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

class Test {

  public static void main(String[] args) throws Exception {
    String path=null;
    boolean forSave=false;
    for (int i=0; i<args.length; i++) {
      String s=args[i];      
      if (s.startsWith("-h")){
        usage();
        return;
      }
      else
      if (s.equals("-p"))
        path=args[++i];
      else
      if (s.equals("-s"))
        forSave=true;
      else {
        System.err.println("Unexpected "+s);
        usage();
        System.exit(1);
        return;
      }        
    }
    test(path, forSave);
  }
  private static void usage() {
    System.err.println("Usage: -p <path> -s");
  }
  private static void test(final String path, final boolean forSave) throws Exception {
    
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
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          Setter<String> logger=new Setter<String>(){
            public void set(String s) {System.out.println(Thread.currentThread()+" "+s);}
          };
          JFrame m=ptc.getMainFrame();
          KAlert alerter=new KAlert(m);
          SSHConnections conns=new SSHConnections(logger, alerter)
            .withKnown(options.getKnownHostsFilename())
            .withPrivateKeys(options.getPrivateKeysFilename())
            .withLogin(
              new SSHLogin(m, alerter)
            );
          org.tmotte.klonk.windows.popup.FileDialogWrapper fdw=
            new org.tmotte.klonk.windows.popup.FileDialogWrapper(
              m,
              new SSHFileSystemView(conns, logger),
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
            File picked=fdw.show(forSave, file, dir);
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
}