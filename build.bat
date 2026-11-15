@echo off
set "JAVA_HOME=%USERPROFILE%\.android-dev\jdk17"
cd /d "%~dp0"
call gradlew.bat assembleDebug testDebugUnitTest %*
exit /b %ERRORLEVEL%
