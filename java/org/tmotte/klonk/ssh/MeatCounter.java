package org.tmotte.klonk.ssh;

/**
 * This implements the take-a-number system at the grocery store, thus preventing 
 * "thread starvation". It's very clever, <em>but:</em>
 * <br>
 * Everyone must play nice and answer when their number is called. 
 * If a thread fails to call unlock(), the whole system seizes up thereafter, so lock()
 * and unlock() should always be used with try-finally. When sleeping between lock
 * attempts, if the thread is sent an interrupt() we ignore that and keep going.
 */
public class MeatCounter {

  //Yes, these ints will eventually wrap around to Integer.MIN_VALUE, which is ok:
  private volatile int nextUp=1;
  private int lastTicket=0;
  private final long spintime;

  /** 
   * @param spintime This is how long we tell a Thread to sleep when it's 
   *   waiting for a lock. If the value is <= 0, we don't sleep at all.
   */
  public MeatCounter(long spintime){
    this.spintime=spintime;
  }
  
  /** Unsynchronized because it should only be called when you hold the lock. */
  public void unlock() {
    nextUp++;
    //mylog(nextUp, "UNLOCK ");
  }
  
  /** 
   * @return true if we received an Exception when sleeping between
   *  lock attempts, most likely an InterruptedException. We will still
   *  keep sleeping until we get the lock, however.
   */
  public boolean lock(String name) {
    final int myTurn=takeANumber();
    //mylog(myTurn, "PICKED: "+name);
    boolean interrupted=false;
    while (nextUp!=myTurn) {
      //mylog(myTurn, "WAIT:   "+name);
      if (spintime>0)
        try {Thread.sleep(spintime);} 
        catch (Exception e) {
          //We have no choice but to keep trying, since otherwise
          //we end up with a lock that will never be unlocked.
          interrupted=true;
        }
    }
    //mylog(myTurn, "LOCKED: "+name);
    return interrupted;
  }
  
  /** 
   * This must be synchronized because the ++ operator is not atomic,
   * nor is it "more atomic" just because you toss in a volatile keyword.
   */
  private synchronized int takeANumber() {
    return ++lastTicket;
  }
  private static void mylog(int number, String msg) {
    System.out.println("MeatCounter: "+number+" "+msg);
  }
  
}
