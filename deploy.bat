@echo off
call ant wasapi
if NOT [%ERRORLEVEL%] == [0] goto QUIT
echo built native libs
 
cp lib\native\windows\jnwasapi.dll ..\AccessionDesktop\jitsi\lib\native\windows
if NOT [%ERRORLEVEL%] == [0] goto QUIT
echo Copied native libs

call ant make
if NOT [%ERRORLEVEL%] == [0] goto QUIT
echo Built libjitsi 

cp libjitsi.jar ..\AccessionDesktop\jitsi\lib\installer-exclude
if NOT [%ERRORLEVEL%] == [0] goto QUIT
cp libjitsi.jar ..\AccessionDesktop\jitsi\sc-bundles
if NOT [%ERRORLEVEL%] == [0] goto QUIT
echo Copied libjitsi

pushd ..\AccessionDesktop
if NOT [%1] == [] call ..\AccessionDesktop\run.bat %1
if [%1] == [] call ..\AccessionDesktop\run.bat rebuild
if NOT [%ERRORLEVEL%] == [0] goto QUIT
popd
goto :EOF

:QUIT
echo Build failed. Exiting
popd
