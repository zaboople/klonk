package org.tmotte.klonk.io;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import org.tmotte.klonk.config.KHome;
import org.tmotte.klonk.config.msg.Doer;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.klonk.config.msg.Getter;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

/**
 * Created because MacOS sucks and they don't have any real support for directory
 * watchers; they use polling and default to 10 seconds, and changing the 10 requires
 * doing "forbidden" APIs. Using a memory map works just as well and gets
 * pretty decent performance, even though it really is just polling, but I
 * control the poll interval.
 */
public class FileListenMemoryMap implements LockInterface {


  // DI STUFF: //

  private final File mapFile;
  private final KLog klog;

  // STATE: //

  /** The memory map can be this big */
  private final int mapCapacity=256 * 256;
  /** Our polling interval */
  private final int waitBetween=2000;
  /** Use this to check the map for changes on both read & write: */
  private byte[] checkBuffer=new byte[128];
  /** Used exclusively for reading all the requested files from other processes: */
  private byte[] readBuffer=new byte[mapCapacity];
  /** The basis of our memory map: */
  private FileChannel fileChannel;
  /** Locks the memory map file so we can read/write exclusively: */
  private Locker locker;

  /** We use this to declare ourselves the running application or
    hand off and bail. */
  private File seizeFile;


  public FileListenMemoryMap(KHome home, KLog klog) {
    try {
      this.mapFile=new File(home.dir, "sharedmap");
      this.seizeFile=FileListen.getSeizeFile(home);
      this.klog=klog;
      if (!mapFile.exists())
        mapFile.createNewFile();
      fileChannel=new RandomAccessFile(mapFile, "rw").getChannel();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public boolean lockOrSignal(String[] fileNames) {
    klog.log("FileListenMemoryMap.lockOrSignal()...");
    try {
      locker=new Locker(seizeFile, klog);
      if (locker.lock())
        return true;
      StringBuilder sb=new StringBuilder();
      for (String file : fileNames)
        if (file!=null) {
          sb.append(file);
          sb.append("\n");
        }
      write(sb.toString());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return false;
  }
  public void startListener(Setter<List<String>> fileReceiver) {
    startListener(waitBetween, fileReceiver);
  }
  public Doer getLockRemover(){
    return new Doer(){
      public @Override void doIt() {
        locker.unlock();
      }
    };
  }

  //////////////////////
  // PRIVATE METHODS: //
  //////////////////////

  private interface NeedsLock<T> {
    public T get() throws Exception;
  }

  private <T> T withLock(NeedsLock<T> toCall) throws Exception {
    FileLock lock=null;
    for (int i=0; lock==null && i<1000; i++) {
      try {
        lock=fileChannel.tryLock();
      } catch (java.nio.channels.OverlappingFileLockException e) {
        throw new RuntimeException(
          "This should only happen when multiple threads are getting locks", e
        );
      }
      if (lock==null)
        Thread.sleep(50);
    }
    if (lock==null)
      throw new RuntimeException("Could not get lock!");
    try {
      return toCall.get();
    } finally {
      lock.release();
    }
  }

  public void startListener(long sleepTime, final Setter<List<String>> fileReceiver) {
    MappedByteBuffer mbb;
    try {
      mbb=fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, mapCapacity);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    Thread thread=new Thread(){
      public void run() {
        while (true) {
          try {
            Thread.sleep(sleepTime);
          } catch (InterruptedException ie) {
            klog.log(ie);
          }
          try {
            List<String> files=read(mbb);
            if (files.size()>0)
              fileReceiver.set(files);
          } catch (Exception e) {
            klog.log(e);
          }
        }
      }
    };
    thread.setDaemon(true);
    thread.start();
  }

  private List<String> read(MappedByteBuffer mem) throws Exception {

    // Look for files being sent in:
    mem.position(0);
    mem.get(checkBuffer);
    boolean found=false;
    for (int i=0; !found && i<checkBuffer.length; i++)
      if (checkBuffer[i]!=0)
        found=true;
    if (!found)
      return java.util.Collections.emptyList();

    // Apparently we found something, so get it. Lock everybody else
    // out so we get clean data:
    resetBuffer(checkBuffer);
    return withLock(() -> {
      mem.position(0);
      mem.get(readBuffer);
      String totalString=new String(readBuffer).trim();
      mem.clear();
      if (totalString.length()>0) {
        resetBuffer(readBuffer);
        mem.put(readBuffer);
        String[] values=totalString.split("\n");
        List<String> result=new ArrayList<>(values.length);
        for (String s : values)
          result.add(s.trim());
        return result;
      }
      return java.util.Collections.emptyList();
    });
  }

  private void resetBuffer(byte[] buf) {
    for (int i=0; i<buf.length; i++)
      buf[i]=0;
  }


  public boolean write(String lines) throws Exception {
    return withLock(()->{
      MappedByteBuffer mem =fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, mapCapacity);

      // Try our darndest to load the data:
      mem.load();
      while (!mem.isLoaded())
        Thread.sleep(100);

      // Keep reading byte blocks until one is blank. We'll start
      // writing there.
      boolean ready=false;
      while (!ready) {
        mem.get(checkBuffer);
        boolean maybe=true;
        for (int i=0; maybe && i<checkBuffer.length; i++)
          if (checkBuffer[i]!=0)
            maybe=false;
        ready=maybe;
        if (!ready) System.out.println("."+lines);
      }

      // Now write all the lines:
      mem.position(mem.position()-checkBuffer.length);
      mem.put(lines.getBytes());
      mem.force();
      return true;
    });
  }

  //////////////
  //          //
  // TESTING: //
  //          //
  //////////////

  public static void main(String[] args) throws Exception {
    KHome khome=new KHome("./test/home");
    KLog klog=new KLog(khome, "*TEST*");
    final java.util.Random random=new java.util.Random(System.currentTimeMillis());

    for (String a: args)
      if (a.startsWith("-r"))
        startTestReader(khome, klog, random);
      else
      if (a.startsWith("-w"))
        startTestWriter(khome, klog, random);
  }
  private static void startTestReader(KHome khome, KLog klog, Random random) throws Exception {
    FileListenMemoryMap reader=new FileListenMemoryMap(khome, klog);
    reader.startListener(
      1500,
      new Setter<List<String>> () {
        public void set(List<String> files) {
          System.out.println();
          for (String f : files)
            System.out.println("RCV: "+f);
          System.out.println();
        }
      }
    );
    new Thread(){
      public void run() {
        while (true)
          try {Thread.sleep(100000);} catch (InterruptedException e) {}
      }
    }.start();
  }
  private static void startTestWriter(KHome khome, KLog klog, Random random) throws Exception {
    for (int i=1; i<2; i++){
      Thread ttt=
        new Thread() {
          public void run() {
            while (true)
              try {
                Thread.sleep(random.nextInt(500));
                int fileCount=random.nextInt(4)+1;
                String toPrint="";
                for (int fc=0; fc<fileCount; fc++){
                  int max=random.nextInt(75)+1;
                  for (int i=0; i<max; i++)
                    toPrint+=(char) (max+32);
                  toPrint+=".\n";
                }
                System.out.print("\nPRT:\n"+toPrint);
                final FileListenMemoryMap writer=new FileListenMemoryMap(khome, klog);
                writer.write(toPrint);
              } catch (Exception e) {
                e.printStackTrace();
                return;
              }
          }
        };
      ttt.setDaemon(false);
      ttt.start();
    }
  }
}