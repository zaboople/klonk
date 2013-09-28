package org.tmotte.klonk.controller;
import org.tmotte.klonk.Editor;
import org.tmotte.klonk.config.msg.StatusUpdate;
import org.tmotte.klonk.config.msg.Editors;
import org.tmotte.klonk.config.KPersist;
import org.tmotte.klonk.windows.popup.Popups;
import java.util.LinkedList;
import org.tmotte.klonk.config.FontOptions;
import org.tmotte.klonk.config.LineDelimiterOptions;
import org.tmotte.klonk.config.TabAndIndentOptions;
import org.tmotte.klonk.windows.popup.LineDelimiterListener;

public class CtrlOptions {
  private Editors editors;
  private StatusUpdate statusBar;
  private Popups popups;
  private KPersist persist;
  private Favorites favorites;
  private TabAndIndentOptions taio;
  private FontOptions fontOptions;
  private LineDelimiterListener delimListener;
  
  public CtrlOptions(
      Editors editors, Popups popups, StatusUpdate statusBar, KPersist persist,
      Favorites favorites, LineDelimiterListener delimListener
    ) {
    this.editors=editors;
    this.statusBar=statusBar;
    this.popups=popups;
    this.persist=persist;
    this.favorites=favorites;
    this.taio=persist.getTabAndIndentOptions();
    this.fontOptions=persist.getFontAndColors();
    this.delimListener=delimListener;
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
    if (popups.showTabAndIndentOptions(taio)){
      editors.getFirst().setTabsOrSpaces(taio.indentionMode);
      for (Editor e: editors.forEach())
        e.setTabAndIndentOptions(taio);
      persist.setTabAndIndentOptions(taio);
      persist.save();
    }
  }
  
  public void doFontAndColors() {
    if (!popups.doFontAndColors(fontOptions))
      return;
    for (Editor e: editors.forEach())
      e.setFont(fontOptions);
    popups.setFontAndColors(fontOptions);
    persist.setFontAndColors(fontOptions);
    persist.save();
  }
  
  public void doFavorites() {
    if (!popups.showFavorites(favorites.getFiles(), favorites.getDirs())) 
      statusBar.showBad("Changes to favorite files/directories cancelled");
    else {
      favorites.set();
      statusBar.show("Changes to favorite files/directories saved");
    }
  }
 
  public void doLineDelimiters(){
    LineDelimiterOptions k=new LineDelimiterOptions();
    k.defaultOption=persist.getDefaultLineDelimiter();
    k.thisFile=editors.getFirst().getLineBreaker();
    popups.showLineDelimiters(k, delimListener);
  }

 
}