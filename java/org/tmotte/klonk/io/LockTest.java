package org.tmotte.klonk.io;
import java.util.Random;
import java.security.SecureRandom;

public class LockTest {
  public static void main(String[] args) throws Exception {
    Random random=new SecureRandom();
    String fileName=args[0];
    String index=args[1];
    Locker locker=new Locker(new java.io.File(fileName), new KLog(System.out));
    int sleep1=random.nextInt(2000), sleep2=random.nextInt(2000);
    Thread.sleep(sleep1);
    if (locker.lock()) {
      System.out.println("\n"+index+": Got lock after "+sleep1);
      Thread.sleep(sleep2);
      locker.unlock();
      System.out.println(index+": Released lock after "+sleep2);
    }
    else
      System.out.println("\n"+index+": Did not get lock after "+sleep1);
    System.out.flush();
  }
}