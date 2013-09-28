package org.tmotte.klonk.io;

public class LockTest {
  public static void main(String[] args) throws Exception {
    String fileName=args[0];
    Locker locker=new Locker(new java.io.File(fileName), new KLog(System.out));
    if (locker.lock()) {
      System.out.println("YES");
      Thread.sleep(10000);
      locker.unlock();
      System.out.println("ha ha ha ok now");
    }
    else
      System.out.println("no");    
    System.in.read();
  }
}