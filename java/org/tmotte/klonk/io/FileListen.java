package org.tmotte.klonk.io;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.LinkedList;
import java.util.List;
import org.tmotte.klonk.config.KHome;
import org.tmotte.klonk.config.msg.Setter;

public class FileListen implements LockInterface {

  private final static String DO_NOT_TOUCH="DO-NOT-TOUCH---";

  private Setter<List<String>> fileReceiver;
  private KLog log;
  private KHome homeWatch;
  private Locker locker;
  private File seizeFile;
  private String pidName;

  public FileListen(KLog klog, String pid, KHome home) {
    this.log=klog;
    this.pidName=pid;
    this.homeWatch=home.mkdir("watch");
    this.seizeFile=getSeizeFile(home);
  }
  public static File getSeizeFile(KHome home) {
    return home.nameFile("seize");
  }

  //////////////////////////
  // INTERFACE FUNCTIONS: //
  //////////////////////////

  public boolean lockOrSignal(String[] fileNames){
    //log.log("FileListen.lockOrSignal()...");
    locker=new Locker(seizeFile, log);
    return locker.lock() || handOff(fileNames);
  }
  public Runnable getLockRemover() {
    return ()-> removeLock();
  }
  public void startListener(Setter<List<String>> fileReceiver){
    this.fileReceiver=fileReceiver;
    log.log("FileListen.startListener()...");
    Thread thread=new Thread(new Listener());
    thread.setDaemon(true);
    thread.start();
  }

  ////////////////////////
  // PRIVATE FUNCTIONS: //
  ////////////////////////

  private void removeLock() {
    try {deleteOldPIDFiles();}
    catch (Exception e) {
      log.log(e, "FileListen.removeLock: Failed to delete old pid files.");
    }
    locker.unlock();
  }

  /** Always returns false */
  private boolean handOff(String[] fileNames) {
    if (fileNames==null || fileNames.length==0)
      return false;
    try {
      log.log("FileListen.handOff(): I am going to write the pid...");
      File pidFile=homeWatch.nameFile(DO_NOT_TOUCH+pidName);
      FileOutputStream os=new FileOutputStream(pidFile);
      FileLock flocker=os.getChannel().lock();
      if (flocker==null) {
        //log.log("FileListen.handOff(): Could not get lock");
        return false;
      }
      PrintWriter pw=new PrintWriter(new OutputStreamWriter(os));
      for (String s: fileNames)
        if (s!=null) {
          //log.log("FileListen.handOff(): Making handoff for "+s);
          pw.println(new File(s).getAbsolutePath());
        }
      pw.flush();
      os.flush();
      flocker.release();//This must happen before close or an error
      pw.close();
      pidFile.renameTo(homeWatch.nameFile(pidName));
    } catch (Exception e) {
      log.log(e, "Messed up writing pid file");
    }
    return false;
  }


  private class Listener implements Runnable {
    public void run(){
      WatchService service;
      try {
        service=makeWatcher(StandardWatchEventKinds.ENTRY_MODIFY,
                            StandardWatchEventKinds.ENTRY_CREATE);
        //This is ok because we delete the file after loading it, just
        //in case we get an event for it as well:
        for (String name: new File(homeWatch.dirName).list())
          loadFiles(name);
        while (true){
          WatchKey key=service.take();
          for (WatchEvent<?> we: key.pollEvents()){
            WatchEvent.Kind<?> kind=we.kind();
            String fileName=we.context().toString();
            //log.log("FileListen: WatchEvent "+we.count()+": "+fileName+" kind:"+kind);
            if (we.count()>1)
              continue;
            if (kind==StandardWatchEventKinds.ENTRY_CREATE ||
                kind==StandardWatchEventKinds.ENTRY_MODIFY)
              try {
                loadFiles(fileName);
              } catch (Exception e) {
                log.log(e, "FileListen failed on reading, waiting for another chance: "+fileName);
              }
            else
            if (kind==StandardWatchEventKinds.OVERFLOW)
              log.log("FileListen got OVERFLOWED...");
            else
              log.log("FileListen got weird event: "+kind);
          }
          if (!key.reset())
            log.log("FileListen couldn't reset key");
        }
      } catch (Exception e) {
        log.log(e, "FileListen: Errors listening to watch directory, probably time to exit");
      }
    }
  }



  private void loadFiles(String filename) throws Exception{
    try {
      if (filename.startsWith(DO_NOT_TOUCH))
        return;
      File pidFile=homeWatch.nameFile(filename);
      log.log("FileListen: Reading PID File..."+pidFile+" "+pidFile.exists());
      if (!pidFile.exists())
        return;
      RandomAccessFile fis=new RandomAccessFile(pidFile, "rw");
      FileChannel fc=fis.getChannel();
      FileLock lock=fc.lock();
      List<String> files=new LinkedList<String>();
      try {
        String s;
        while ((s=fis.readLine())!=null)
          if (!s.trim().equals("")){
            files.add(s);
            log.log("FileListen: Flagging file for open: "+s);
          }
      } finally {
        try {
          lock.release();
          fis.close();
          pidFile.delete();
        } catch (Exception e) {
          log.log(e, "COULDN'T UNLOCK/CLOSE/DELETE "+pidFile);
        }
      }
      if (files.size()>0) {
        log.log("FileListen: telling File Receiver to load from listener....");
        fileReceiver.set(files);
        log.log("FileListen: told File Receiver to load from listener.");
      }
    } catch (Exception e) {
      log.log("Failed on reading, waiting for another chance: "+filename+" Exception: "+e);
    }
  }

  private WatchService makeWatcher(WatchEvent.Kind<?>... kind) throws Exception {
    FileSystem fileSys=FileSystems.getDefault();
    WatchService watcher=fileSys.newWatchService();
    Path path=fileSys.getPath(homeWatch.dirName);
    path.register(watcher, kind);
    return watcher;
  }

  /** Mostly unnecessary but it hurts nothing */
  private void deleteOldPIDFiles() throws Exception {
    for (String name: new File(homeWatch.dirName).list()){
      File file=homeWatch.nameFile(name);
      log.log("Deleting old: "+file);
      file.delete();
    }
  }

}