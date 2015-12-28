package org.tmotte.klonk.config;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.swing.KeyStroke;
import org.tmotte.common.swang.KeyParser;

public class KeyConfig {
  private static String pfx="shortcut.";
  public static String
    accelSwitchMenu=pfx+"accel.switch.menu";

  private Map<String, KeyStroke> keyMap=new HashMap<>();
  public KeyConfig(Properties props) throws Exception {
    for (Enumeration names=props.propertyNames(); names.hasMoreElements();){
      String name=names.nextElement().toString();
      if (name.startsWith("shortcut."))
        keyMap.put(name, KeyParser.parse(props.getProperty(name)));
    }
  }
}