package org.tmotte.klonk.ssh;

/**
 * This implements the take-a-number system at the grocery store. It uses the volatile keyword 
 * and Thread.sleep() to sneak around any need for synchronization. <em>However:</em>
 * <br>
 * Everyone must play nice and answer when their number is called. 
 * If a thread fails to call unlock(), the whole system seizes up thereafter, so lock()
 * and unlock() should always be used with try-finally. When sleeping between lock
 * attempts, if the thread is sent an interrupt() we ignore that and keep going.
 */
public class MeatCounter {

  //Yes, these ints will eventually wrap around to Integer.MIN_VALUE, which is ok:
  private volatile int next=1;
  private volatile int waiting=0;
  private final long spintime;

  /** 
   * @param spintime This is how long we tell a Thread to sleep when it's 
   *   waiting for a lock. If the value is <= 0, we don't sleep at all.
   */
  public MeatCounter(long spintime){
    this.spintime=spintime;
  }
  
  public void unlock() {
    next++;
    //System.out.println("UNLOCK "+next+" "+Thread.currentThread().hashCode());
  }
  
  /** 
   * @return true if we received an Exception when sleeping between
   *  lock attempts, most likely an InterruptedException. We will still
   *  keep sleeping until we get the lock, however.
   */
  public boolean lock(String name) {
    final int myTurn=++waiting; 
    //System.out.println("LOCKING "+myTurn+" "+Thread.currentThread().hashCode()+" "+name);
    boolean interrupted=false;
    while (next!=myTurn) {
      //System.out.println("WAIT "+myTurn+" "+Thread.currentThread().hashCode()+" "+name);
      if (spintime>0)
        try {Thread.sleep(spintime);} 
        catch (Exception e) {
          //We have no choice but to keep trying, since otherwise
          //we end up with a lock that will never be unlocked.
          interrupted=true;
        }
    }
    //System.out.println("GOT "+myTurn+" "+Thread.currentThread().hashCode()+" "+name);
    return interrupted;
  }
  
}