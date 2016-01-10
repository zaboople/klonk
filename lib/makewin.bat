rem Note: javapackager comes with java 8 distributions only
rem This will advise you to install software from http://www.jrsoftware.org/, which
rem is kind of lame.

ant clean config.prod jar
javapackager -deploy -native exe -srcfiles dist/klonk.jar -outdir dist/ -outfile klonk -appclass org.tmotte.klonk.config.Klonk -title "Klonk" -name Klonk
