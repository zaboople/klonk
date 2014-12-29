package org.tmotte.klonk.ssh.test;
import org.tmotte.klonk.ssh.MeatCounter;

public class TestLocking {
  public static void main(String[] args) throws Exception {
    testMeatCounter();
    //testFinally();
  }
  
  /////////////
  // TEST 1: //
  /////////////
  
  
  /**
   * Verifies that our take-a-number locking system works.
   */
  static void testMeatCounter() {
    //It's actually faster to use a 1-millisecond sleep than a 0-millisecond sleep. The threads
    //tend to line up and behave instead of blitzkrieging:
    MeatCounter locker=new MeatCounter(1);
       
    //Tests integer overflow, which doesn't affect anything so I've disabled it:
    if (false)
      for (int i=0; i<Integer.MAX_VALUE-100; i++){
        locker.lock("ME2");
        locker.unlock();
      }
    
    //Lock everybody out until we're ready to go:
    locker.lock("ME");
    
    //Start everything and let them spin:
    MyThing thing=new MyThing();
    for (int i=0; i<40; i++)
      new Thread(
        new Hitter("T"+i, thing, locker, true, 20)
      ).start();
      
    //Unleash the horde:
    locker.unlock();
  }
  
  static class MyThing {
    int done=0;
    public int inc() {return ++done;}
  }
  static class Hitter implements Runnable {
    final String name;
    final MyThing thing;
    final MeatCounter locker;
    final int max;
    final boolean doLocking;
    public Hitter(String name, MyThing thing, MeatCounter locker, boolean doLocking, int max) {
      this.name=name;
      this.thing=thing;
      this.locker=locker;
      this.doLocking=doLocking;
      this.max=max;
    }
    public void run() {
      for (int i=1; i<=max; i++) {
        if (doLocking) locker.lock(name);
        try {
          int next=thing.inc();
          for (int k=0; k<next; k++)
            if (k % 10 == 0)
              System.out.print(".");
          System.out.print(next);
          System.out.print("-");
          System.out.print(name);
          if (i==max)
            System.out.print("-DONE");
          System.out.println();
          System.out.flush();
        } finally {
          if (doLocking) locker.unlock();
        }
      }
    }
  }
  
  //////////////
  // TEST 2:  //
  //////////////


  /** 
   * This was to give myself confidence that finally always executes, even when
   * catch() rethrows an Exception. People say things, you never know...
   */
  private static void testFinally() throws Exception {
    try {
      if (true)
        throw new Exception("BANG");
    } catch (Exception e) {
      throw new RuntimeException("RETHROW FROM CATCH", e);
    } finally {
      System.out.println("NO MATTER WHAT");
    }
  }  
 
f