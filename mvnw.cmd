@ECHO OFF
SET BASEDIR=%~dp0
mvn -f "%BASEDIR%pom.xml" %*
