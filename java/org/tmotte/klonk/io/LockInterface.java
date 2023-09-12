package org.tmotte.klonk.io;
import org.tmotte.klonk.config.msg.Setter;
import java.util.List;

public interface LockInterface {
  public boolean lockOrSignal(String[] fileNames);
  public void startListener(Setter<List<String>> fileReceiver);
  public Runnable getLockRemover();
}
