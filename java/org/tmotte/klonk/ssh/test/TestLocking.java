package org.tmotte.klonk.ssh.test;
import org.tmotte.klonk.ssh.MeatCounter;

public class TestLocking {
  public static void main(String[] args) throws Exception {
  
    //Creat the locker with whatever spin time. It's actually faster
    //to implement a 1-millisecond sleep than a 0-millisecond sleep, as the threads
    //tend to line up and behave instead of blitzkrieging:
    MeatCounter locker=new MeatCounter(1);
       
    //Use this to test integer overflow, which doesn't affect anything so I've disabled it:
    if (false)
      for (int i=0; i<Integer.MAX_VALUE-100; i++){
        locker.lock("ME2");
        locker.unlock();
      }
    
    //Lock everybody out until we're ready to go:
    locker.lock("ME");
    MyThing thing=new MyThing();
    for (int i=0; i<40; i++)
      new Thread(
        new Hitter("T"+i, thing, locker, 20)
      ).start();
      
    //All the threads are spinning & waiting, now cut them loose:
    locker.unlock();
  }
  static class MyThing {
    int done=0;
    public int inc() {
      return ++done;
    }
  }
  static class Hitter implements Runnable {
    final String name;
    final MyThing thing;
    final MeatCounter locker;
    final int max;
    public Hitter(String name, MyThing thing, MeatCounter locker, int max) {
      this.name=name;
      this.thing=thing;
      this.locker=locker;
      this.max=max;
    }
    public void run() {
      //To see what it looks like without locking, comment out
      //locker.lock & locker.unlock. There should be lots of overlapping
      //output.
      for (int i=1; i<=max; i++) {
        locker.lock(name);
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
          locker.unlock();
        }
      }
    }
  }
}