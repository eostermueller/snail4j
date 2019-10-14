LANDING=/Users/erikostermueller/Documents/src/jssource/havoc2/installer/installFiles


#These are part of snail4j distribution
PM_HOME=/Users/erikostermueller/Documents/src/jssource/havoc2/havoc2/processManager
JM_HOME=/Users/erikostermueller/Documents/src/jssource/havoc2/havoc2/jmeterFiles
WM_HOME=/Users/erikostermueller/Documents/src/jssource/havoc2/havoc2/wiremock
MVN_BIN=http://www-us.apache.org/dist/maven/maven-3/3.6.2/binaries/apache-maven-3.6.2-bin.zip
MVN_REPO=/Users/erikostermueller/.m2

# local location of this repo: https://github.com/eostermueller/tjp2
TJP_HOME=/Users/erikostermueller/Documents/src/jsource/tjp2

#Snail4j starup looks for carefully named zip files in this folder, then unzips them.
TARGET=/Users/erikostermueller/Documents/src/jssource/havoc2/havoc2/backend/src/main/resources

echo Creating processManager.zip
jar cvfM $TARGET/processManager.zip -C $PM_HOME .
echo processManager.zip is created

echo Creating jmeterFiles.zip
jar cvfM $TARGET/jmeterFiles.zip -C $JM_HOME .
echo jmeterFiles.zip is created

echo Creating wiremockFiles.zip
jar cvfM $TARGET/wiremockFiles.zip -C $WM_HOME .
echo wiremockFiles.zip is created

echo Creating sutApp.zip
jar cvfM $TARGET/sutApp.zip -C $TJP_HOME .
echo sutApp.zip created.

#
#  Geez this takes so long, so keep it commented out until absolutely necessary
#
echo Creating repository.zip
jar cvfM $TARGET/repository.zip -C $MVN_REPO repository
echo repository.zip is created

cp $LANDING/data.zip $TARGET/


wget --output-document=$TARGET/apache-maven-3.6.2-bin.zip $MVN_BIN

