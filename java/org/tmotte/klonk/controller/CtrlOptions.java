package org.tmotte.klonk.controller;
import java.util.LinkedList;
import java.util.List;
import org.tmotte.klonk.Editor;
import org.tmotte.klonk.ssh.SSHConnections;
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
import org.tmotte.klonk.windows.popup.LineDelimiters;
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
  private LineDelimiters lineDelimiters;
  private SSHConnections sshConns;

  private Favorites favorites;
  private SSHOptionPicker sshOptionPicker;
  private TabsAndIndents tabsAndIndents;
  private FontPicker fontPicker;

  public CtrlOptions(
      Editors editors, StatusUpdate statusBar, KPersist persist, CtrlFavorites ctrlFavorites,
      LineDelimiterListener delimListener, List<Setter<FontOptions>> fontListeners, SSHConnections sshConns,
      SSHOptionPicker sshOptionPicker, TabsAndIndents tabsAndIndents,
      Favorites favorites, FontPicker fontPicker, LineDelimiters lineDelimiters
    ) {
    this.editors=editors;
    this.statusBar=statusBar;
    this.persist=persist;
    this.ctrlFavorites=ctrlFavorites;
    this.delimListener=delimListener;
    this.fontListeners=fontListeners;
    this.sshConns=sshConns;

    this.sshOptionPicker=sshOptionPicker;
    this.tabsAndIndents=tabsAndIndents;
    this.favorites=favorites;
    this.fontPicker=fontPicker;
    this.lineDelimiters=lineDelimiters;

    this.taio=persist.getTabAndIndentOptions();
    this.fontOptions=persist.getFontAndColors();
    this.sshOptions=persist.getSSHOptions();
  }

  public void doWordWrap() {
    boolean b=persist.getWordWrap();
    persist.setWordWrap(!b);
    persist.save();
    for (Editor e: editors.forEach())
      e.setWordWrap(!b);
  }

  public void doAutoTrim() {
    boolean b=persist.getAutoTrim();
    persist.setAutoTrim(!b);
    persist.save();
    for (Editor e: editors.forEach())
      e.setAutoTrim(!b);
  }


  public void doTabsAndIndents(){
    taio.indentionMode=editors.getFirst().getTabsOrSpaces();
    if (tabsAndIndents.show(taio)){
      editors.getFirst().setTabsOrSpaces(taio.indentionMode);
      for (Editor e: editors.forEach())
        e.setTabAndIndentOptions(taio);
      persist.setTabAndIndentOptions(taio);
      persist.save();
      statusBar.show("Changes to tabs & indents saved");
    }
    else
      statusBar.showBad("Changes to tabs & indents cancelled");
  }

  public void doFontBigger() {
    fontOptions.setFontSize(fontOptions.getFontSize()+1);
    pushFont();
  }
  public void doFontSmaller() {
    int i=fontOptions.getFontSize()-1;
    if (i>0) {
      fontOptions.setFontSize(i);
      pushFont();
    }
    else
      statusBar.showBad("Cannot make font any smaller");
  }
  public void doFontAndColors() {
    if (!fontPicker.show(fontOptions)){
      statusBar.showBad("Changes to font & colors cancelled");
      return;
    }
    statusBar.show("Changes to font & colors saved");
    pushFont();
  }
  private void pushFont() {
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
    lineDelimiters.show(k, delimListener);
  }

  public void doSSH(){
    List<String> hosts=sshConns.getConnectedHosts();
    if (sshOptionPicker.show(sshOptions, hosts)){
      persist.writeSSHOptions();
      persist.save();
      for (String dis: hosts)
        sshConns.close(dis);
      sshConns.withOptions(sshOptions);
      statusBar.show("SSH changes saved");
    }
    else
      statusBar.showBad("Changes to SSH cancelled");

  }
}
