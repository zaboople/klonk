cd /c/troy/dev/textedit
ant config.test compile && java -Xshare:off -Xms6m -classpath build "$@"
