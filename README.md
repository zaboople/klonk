# Klonk
Klonk is a simple but robust text editor that aims for fluid ease-of-use and convenience over exotic features. I wrote it for myself and use it as my everyday editor.

Capabilities worth mentioning:

* 100% Java as well as convenient OSX/MacOS & Windows executables
* Keyboard-friendly but equally menu-friendly
* Simple undo/redo system that allows recovery of all edit states from beginning to end
* Find & replace with multi-line & regex
* Rapid file switching, opening, reopening, with "favorite" directories & files support
* Open files over SSH
* Execute shell scripts without switching applications
* The usual things: Trailing-whitespace-trim, auto-indent with tabs or spaces, line wrap control, sorting, marking, alignment tricks and so forth

# Building it
Klonk requires Java 8 to run. To build, use a Java 8 JDK and a reasonably recent version of [Apache Ant](http://ant.apache.org/).

To build a Windows Klonk.exe you will need [JSmooth](http://jsmooth.sourceforge.net/). Type `ant help` in the git checkout directory for detailed instructions.

OSX/MacOS native executables are supported via Java's built-in `javapackager` utility. Refer to the script [lib/makedmg](lib/makedmg). Note that this script includes a `-Bruntime=` flag that tells javapackager not to put a java JRE into the install - this only works correctly when building for java 1.8.0_92 and above. You need to remove it for earlier versions of Java 8 (or just upgrade to the latest).
