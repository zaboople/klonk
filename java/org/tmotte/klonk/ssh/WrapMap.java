package org.tmotte.klonk.ssh;
import java.util.HashMap;

/** 
 * This implements a basic Map cache where items are expired by age. When overcrowding occurs, 
 * the oldest go overboard first. This would use generic types for both key & value, but we have
 * an internal array and those don't allow generics, thanks java :/
 */
public class WrapMap<B> {
  private final int max;
  private final long ageLimit;
  
  private final HashMap<String,IndexVal> data;
  private final String[] names;
  private int limIndex=0;

  private final class IndexVal {
    B value;
    int index;
    long time;
  }

  public WrapMap(int max, long ageLimit) {
    this.max=max;
    this.ageLimit=ageLimit;
    data=new HashMap<>(this.max);
    names=new String[this.max];
  }
  
  
  public synchronized B get(String a) {    
    IndexVal vi=data.get(a);
    if (vi==null) 
      return null;
    else
    if (System.currentTimeMillis()-vi.time > ageLimit){
      data.remove(a);
      names[vi.index]=null;
      return null;
    }
    else
      return vi.value;
  }
  public synchronized void remove(String a) {    
    IndexVal vi=data.get(a);
    if (vi!=null) {
      data.remove(a);
      names[vi.index]=null;
    }
  }

  public synchronized void put(String name, B b) {
    String other=names[limIndex];
    if (other!=null) 
      data.remove(other);
  
    IndexVal vi=data.get(name);
    if (vi!=null)
      names[vi.index]=null;
    else {
      vi=new IndexVal();
      data.put(name, vi);
    }

    vi.value=b;
    vi.time=System.currentTimeMillis();
    vi.index=limIndex;
    
    names[limIndex]=name;
    limIndex++; if (limIndex==max) limIndex=0;
  }
  public synchronized int size(){
    return data.size();
  }

  public synchronized void reset() {
    for (int i=0; i<names.length; i++)
      names[i]=null;
    data.clear();
    limIndex=0;
  }
 
  ///////////////////////////////
  // TESTING:                  //
  // Kind of fun to watch too. //
  ///////////////////////////////
  
  public void debug(java.io.PrintStream ps, String formatStr, int perLine) {
    long time=System.currentTimeMillis();
    for (int i=0; i<names.length; i++) {
      String name=names[i];
      IndexVal vi=data.get(name);
      if (name!=null && vi==null) throw new RuntimeException("Where did "+name+" go?");
      ps.print(
        String.format(
          formatStr,
          name!=null ?name :"-",
          vi==null
            ?"-"
            :System.currentTimeMillis()-vi.time
        )
      );
      int count=i+1;
      if (count>=perLine && count%perLine==0)
        ps.println();
    }
    ps.println();
  }

}