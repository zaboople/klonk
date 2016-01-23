package org.tmotte.klonk.io;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.klonk.config.msg.Doer;
import java.util.List;

public interface LockInterface {
  public boolean lockOrSignal(String[] fileNames);
  public boolean startListener(Setter<List<String>> fileReceiver);
  public Doer getLockRemover();
}
