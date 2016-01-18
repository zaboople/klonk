package org.tmotte.klonk.io;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import org.tmotte.klonk.config.KHome;
import org.tmotte.klonk.config.msg.Setter;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class FileListenMemoryMap {

  interface NeedsLock<T> {
    public T get() throws Exception;
  }

  final File mapFile;
  final int mapCapacity=256 * 256;
  private int currPos;
  private byte[] blankBuffer=new byte[256];
  private FileChannel fileChannel;

  public FileListenMemoryMap(File homeDir) throws Exception {
    mapFile=new File(homeDir, "sharedmap");
    if (!mapFile.exists())
      mapFile.createNewFile();
    fileChannel=new RandomAccessFile(mapFile, "rw").getChannel();
  }
  private <T> T withLock(NeedsLock<T> toCall) throws Exception {
    FileLock lock=null;
    for (int i=0; lock==null && i<1000; i++) {
      try {
        lock=fileChannel.tryLock();
      } catch (java.nio.channels.OverlappingFileLockException e) {
        System.err.println("FIXME overlapping");
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

  public void startListener(long sleepTime, final Setter<List<String>> fileReceiver) throws Exception {
    MappedByteBuffer mbb=fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, mapCapacity);
    Thread thread=new Thread(){
      public void run() {
        while (true) {
          try {
            Thread.sleep(sleepTime);
          } catch (InterruptedException ie) {
            System.err.println("FIXME caught1 "+ie);
          }
          try {
            List<String> files=read(mbb);
            if (files.size()>0)
              fileReceiver.set(files);
          } catch (Exception e) {
            System.err.println("FIXME caught2 "+e);
          }
        }
      }
    };
    thread.setDaemon(true);
    thread.start();
  }

  private List<String> read(MappedByteBuffer mem) throws Exception {
    return withLock(() -> {
      //Note that the FileChannel.size() does not necessarily match the capacity
      //we assign to the buffer.
      mem.position(0);

      byte[] buf=new byte[mapCapacity];
      mem.get(buf);
      String totalString=new String(buf).trim();
      mem.clear();
      if (totalString.length()>0) {
        for (int i=0; i<buf.length; i++)
          buf[i]=0;
        mem.put(buf);
        return java.util.Arrays.asList(totalString.split("\n"));
      }
      else
        return java.util.Collections.emptyList();
    });
  }


  public boolean write(String lines) throws Exception {
    return withLock(()->{
      MappedByteBuffer mem =fileChannel.map(FileChannel.MapMode.READ_WRITE, 0, mapCapacity);

      // Try our darndest to load the data:
      mem.load();
      while (!mem.isLoaded())
        Thread.sleep(100);

      // Keep reading 256 byte blocks until one is blank. We'll start
      // writing there.
      byte[] checkBuffer=new byte[256];
      boolean ready=false;
      while (!ready) {
        mem.get(checkBuffer);
        boolean maybe=true;
        for (int i=0; maybe && i<checkBuffer.length; i++)
          if (checkBuffer[i]!=0)
            maybe=false;
        ready=maybe;
      }

      mem.position(mem.position()-bufSize);
      mem.put(lines.getBytes());
      mem.force();
      return true;
    });
  }

  //////////////
  // TESTING: //
  //////////////K0fnL44c,mlkJ6d

  public static void main(String[] args) throws Exception {
    KHome khome=new KHome("./test/home");
    KLog klog=new KLog(khome, "*TEST*");
    final java.util.Random random=new java.util.Random(System.currentTimeMillis());

    for (String a: args)
      if (a.startsWith("-r"))
        startTestReader(khome, random);
      else
      if (a.startsWith("-w"))
        startTestWriter(khome, random);
  }
  private static void startTestReader(KHome khome, Random random) throws Exception {
    FileListenMemoryMap reader=new FileListenMemoryMap(khome.dir);
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
  private static void startTestWriter(KHome khome, Random random) throws Exception {
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
                final FileListenMemoryMap writer=new FileListenMemoryMap(khome.dir);
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