package org.tmotte.klonk.ssh;
import java.util.HashMap;
import java.util.ArrayList;

class WrapMap<B> {
  private final int max;
  private final long ageLimit;
  private final HashMap<String,B> data;

  private final String[] names;
  private final long[] ages;  
  private int zeroStop=0;//point of last zero
  private int limIndex=0;

  public WrapMap(int max, long ageLimit) {
    this.max=max;
    this.ageLimit=ageLimit;
    names=new String[this.max];
    ages=new long[this.max];
    data=new HashMap<>(this.max);

    limIndex=0;
    zeroStop=max-1;
  }
  
  
  public synchronized B get(String a) {    
    clean();
    return data.get(a);
  }

  public synchronized void put(String name, B b) {
    clean();
    if (data.containsKey(name))
      killKey(name);
    if (names[limIndex]!=null) 
      data.remove(names[limIndex]);
    names[limIndex]=name;
    ages[limIndex]=System.currentTimeMillis();
    limIndex++;
    if (limIndex==max) limIndex=0;
    data.put(name, b);
  }
  public synchronized int size(){
    return data.size();
  }

  public synchronized void reset() {
    for (int i=0; i<names.length; i++){
      names[i]=null;
      ages[i]=0;
    }
    data.clear();
    limIndex=0;
    zeroStop=max-1;
  }
  
  private void killKey(String name) {
    int i=zeroStop;
    for (int count=0; count<max; count++){
      i++; if (i==max) i=0;
      if (name.equals(names[i])){
        names[i]=null;
        ages[i]=1;
        return;
      }
    }  
    throw new RuntimeException("Could not kill duplicate key "+name);
  }
  private int distanceToZero(int i) {
    int distance=zeroStop-i;
    if (distance<0) distance=max+distance;
    return distance;
  }
  private void clean() {
    int i=limIndex;
    long currTime=System.currentTimeMillis();
    for (int count=0; count<max; count++) {
      i++; if (i==max) i=0;
      long date=ages[i];
      if (date==0) {
        count+=distanceToZero(i);
        i=zeroStop;
      }
      else 
      if (date==1) {
        //Reclaimed slot from duplicate key
      }
      else
      if (currTime-date>ageLimit){
        System.out.print("Yanking "+ages[i]);
        ages[i]=0;
        zeroStop=i;
        data.remove(names[i]);
        names[i]=null;
      }
    }
  }
  
  //QUICK TEST:
  
  public static void main(String[] args) throws Exception {
    WrapMap<Integer> wc=new WrapMap<>(100, 200);
    for (int i=0; i<105; i++){
      Thread.sleep(i);
      wc.put(""+i, i);
      System.out.print(" p"+wc.size()+" ");
      System.out.flush();
    }
    System.out.println();
    for (int i=0; i<105; i++){
      Integer x=wc.get(""+i);
      if (x!=null)
        System.out.print(x+" ");
    }
  }
}