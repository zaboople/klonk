package org.tmotte.klonk.controller;
import java.util.LinkedList;
import java.util.List;
import org.tmotte.klonk.Editor;
import org.tmotte.klonk.config.KPersist;
import org.tmotte.klonk.config.msg.Editors;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.klonk.config.msg.StatusUpdate;
import org.tmotte.klonk.config.option.FontOptions;
import org.tmotte.klonk.config.option.LineDelimiterOptions;
import org.tmotte.klonk.config.option.SSHOptions;
import org.tmotte.klonk.config.option.TabAndIndentOptions;
import org.tmotte.klonk.windows.popup.Favorites;
import org.tmotte.klonk.windows.popup.FontPicker;
import org.tmotte.klonk.windows.popup.LineDelimiterListener;
import org.tmotte.klonk.windows.popup.Popups;
import org.tmotte.klonk.windows.popup.TabsAndIndents;
import org.tmotte.klonk.windows.popup.ssh.SSHOptionPicker;

public class CtrlOptions {

  private Editors editors;
  private StatusUpdate statusBar;
  private KPersist persist;
  private CtrlFavorites ctrlFavorites;
  private TabAndIndentOptions taio;
  private FontOptions fontOptions;
  private SSHOptions sshOptions;
  private LineDelimiterListener delimListener;
  private List<Setter<FontOptions>> fontListeners;
  private Popups popups;

  private Favorites favorites;
  private SSHOptionPicker sshOptionPicker;
  private TabsAndIndents tabsAndIndents;
  private FontPicker fontPicker;
  
  public CtrlOptions(
      Editors editors, StatusUpdate statusBar, KPersist persist, CtrlFavorites ctrlFavorites, 
      LineDelimiterListener delimListener, List<Setter<FontOptions>> fontListeners,
      SSHOptionPicker sshOptionPicker, TabsAndIndents tabsAndIndents, 
      Favorites favorites, FontPicker fontPicker,
      Popups popups
    ) {
    this.editors=editors;
    this.statusBar=statusBar;
    this.persist=persist;
    this.favorites=favorites;
    this.ctrlFavorites=ctrlFavorites;
    this.delimListener=delimListener;
    this.fontListeners=fontListeners;
    this.sshOptionPicker=sshOptionPicker;
    this.tabsAndIndents=tabsAndIndents;
    this.fontPicker=fontPicker;
    this.popups=popups;

    this.taio=persist.getTabAndIndentOptions();
    this.fontOptions=persist.getFontAndColors();
    this.sshOptions=persist.getSSHOptions();
  } 
  
  public void doWordWrap() {
    boolean b=persist.getWordWrap();
    persist.setWordWrap(!b);
    persist.save();
    for (Editor e: editors.forEach())
      e.setWordWrap(!b);;
  }

  public void doTabsAndIndents(){
    taio.indentionMode=editors.getFirst().getTabsOrSpaces();
    if (tabsAndIndents.show(taio)){
      editors.getFirst().setTabsOrSpaces(taio.indentionMode);
      for (Editor e: editors.forEach())
        e.setTabAndIndentOptions(taio);
      persist.setTabAndIndentOptions(taio);
      persist.save();
    }
  }
  
  public void doFontAndColors() {
    if (!fontPicker.show(fontOptions))
      return;
    for (Editor e: editors.forEach())
      e.setFont(fontOptions);
    for (Setter<FontOptions> setter: fontListeners)
      setter.set(fontOptions);
    persist.setFontAndColors(fontOptions);
    persist.save();
  }
  
  public void doFavorites() {
    if (!favorites.show(ctrlFavorites.getFiles(), ctrlFavorites.getDirs())) 
      statusBar.showBad("Changes to favorite files/directories cancelled");
    else {
      ctrlFavorites.set();
      statusBar.show("Changes to favorite files/directories saved");
    }
  }
 
  public void doLineDelimiters(){
    LineDelimiterOptions k=new LineDelimiterOptions();
    k.defaultOption=persist.getDefaultLineDelimiter();
    k.thisFile=editors.getFirst().getLineBreaker();
    popups.showLineDelimiters(k, delimListener);
  }

  public void doSSH(){ 
    if (sshOptionPicker.show(sshOptions)){
      persist.setSSHOptions(sshOptions);
      persist.save();
    }
    else
      statusBar.show("Cancelled.");
  }
}