package org.tmotte.klonk.controller;
import java.util.ArrayList;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.LinkedList;
import org.tmotte.klonk.Editor;
import org.tmotte.klonk.config.KPersist;
import org.tmotte.klonk.config.msg.Setter;

/** This is a secondary controller; it is invoked by other controllers. */
public class CtrlFavorites {

  //DI resources:
  private KPersist persist;
  private Setter<List<String>> favoriteFileListener, favoriteDirListener;

  //Private data:
  private ArrayList<String> favoriteDirs, favoriteFiles;

  public CtrlFavorites(
      KPersist persist,
      Setter<List<String>> favoriteFileListener,
      Setter<List<String>> favoriteDirListener
    ) {
    this.persist=persist;
    this.favoriteFileListener=favoriteFileListener;
    this.favoriteDirListener=favoriteDirListener;
    favoriteDirs    =new ArrayList<>(persist.maxFavorite);
    favoriteFiles   =new ArrayList<>(persist.maxFavorite);
    persist.getFavorites(favoriteFiles, favoriteDirs);
    favoriteFileListener.set(favoriteFiles);
    favoriteDirListener.set(favoriteDirs);
  }
  public Collection<String> getFiles() {
    return favoriteFiles;
  }
  public Collection<String> getDirs() {
    return favoriteDirs;
  }
  public void set() {
    favoriteFileListener.set(favoriteFiles);
    favoriteDirListener.set(favoriteDirs);
    persist.setFavoriteFiles(favoriteFiles);
    persist.setFavoriteDirs(favoriteDirs);
    persist.save();
  }

}