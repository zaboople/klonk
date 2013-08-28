package org.tmotte.klonk.io;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.LinkedList;
import java.util.List;
import org.tmotte.klonk.KLog;

class Locker {
  private File lockFile;
  private FileLock lock;
  private FileOutputStream fos;
  private KLog log;
  
  public Locker(File lockFile, KLog log) {
    this.lockFile=lockFile;
    this.log=log;
  }
  
  public boolean lock() {
    try {
      fos=new FileOutputStream(lockFile);
      FileChannel fc=fos.getChannel();
      lock=fc.tryLock();
      if (lock==null){
        log.log("Locker: Failed, got a null value on lockFile: "+lockFile);
        return false;
      }
      fos.write(1);
      return true;
    } catch (Exception e) {
      log.log(e);
      return false;
    }
  }
  public void unlock() {
    try {
      lock.release();
      fos.close();
      lockFile.delete();
    } catch (Exception e) {
      log.log(e);
    }
  }
  
}