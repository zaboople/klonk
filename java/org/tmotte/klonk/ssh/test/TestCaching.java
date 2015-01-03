package org.tmotte.klonk.ssh.test;
import org.tmotte.klonk.ssh.WrapMap;

public class TestCaching {  
  public static void main(String[] args) throws Exception {
    java.util.Random random=new java.util.Random(System.currentTimeMillis());
    
    int maxSize=100;
    int maxTime=1700;
    int maxSleep=100;
    int tries=525;
    int maxval=800;    
    String formatStr="%5s-%-6s ";
    int perLine=10;
    
    WrapMap<Integer> wc=new WrapMap<>(100, 2000);
    {
      for (int count=0; count<tries; count++){
        Thread.sleep(random.nextInt(maxSleep));
        int put=random.nextInt(maxval);
        int get=random.nextInt(maxval);
        wc.put(""+put, put);
        wc.get(""+get);
        System.out.println(count+" p"+put+" g"+get);
        wc.debug(System.out, formatStr, perLine);
      }
    }
    System.out.println("\nVALUES:");
    for (int v=0; v<maxval; v++){
      Integer x=wc.get(""+v);
      if (x!=null)
        System.out.print(x+" ");
    }    
    System.out.println("\nDEBUG LAST:");
    wc.debug(System.out, formatStr, perLine);
  }
}