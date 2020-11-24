ECHO off

SETLOCAL ENABLEEXTENSIONS

if NOT "%SERVER%"=="" GOTO skip
REM Set to test endpoint if not set
set SERVER=https://covdch-dev.ohms.oracle.com

:skip

IF /I "%1" EQU "-clientId" ( set CLIENTID=%2 & shift /1 & shift /1 & goto skip)
IF /I "%1" EQU "-clientSecret" ( set CLIENTSECRET=%2 & shift /1 & shift /1 & goto skip)
IF /I "%1" EQU "-server" (set SERVER=%2 & shift /1 & shift /1 & goto skip)

if "%CLIENTID%"=="" (echo CLIENTID is not set & goto usage)
if "%CLIENTSECRET%"=="" (echo CLIENTSECRET is not set & goto usage)
if "%SERVER%"=="" (echo SERVER is not set & goto usage)

REM Create a request to generate a new token using CLIENTID and CLIENTSECRET
SET REQUEST={ "clientID": ^"%CLIENTID%^"^^, "clientSecret": ^"%CLIENTSECRET%^"^^, "scopes": ["UPLOAD" ] }

REM To enable tracing, chance the following to SET TRACE=--trace curl-trace.txt
SET TRACE=

:tryAgain
if "%1"=="" (echo No Files to send & goto usage)
IF NOT EXIST %1 (echo "%1" does not exist && shift /1 && goto tryAgain)


REM Parse the token from the response
for /F "usebackq tokens=1,2 delims=[]{},: " %%f in (`echo %REQUEST% ^| curl %TRACE% "%SERVER%/v0/token/gen" --header "Content-Type: application/json" --header "Accept: application/json" --data @-`) do set %%~f=%%~g

if "%TOKEN%"=="" (echo CLIENTID or CLIENTSECRET is not valid & goto end)

:nextFile
IF NOT EXIST %1 (echo "%1" does not exist && shift /1 && goto nextFile)

REM Send the requested file too the server
curl --location --request POST %SERVER%/v0/upload/cvrs/batch ^
	--header "Content-Type: text/plain" ^
	--header "Authorization: %TOKEN%" ^
	--header "Accept: application/json" ^
	--data-binary @%1

IF NOT "%2"=="" shift /1 & goto nextFile

:end
ENDLOCAL
Exit /B

:usage
ECHO Usage: %0 [-clientId CLIENTID] [-clientSecret CLIENTSECRET] [-server SERVER] file ...
ECHO OFF
ECHO -clientId CLIENTID
ECHO    Set the clientId for the connection. If not set, uses CLIENTID in the environment.
ECHO OFF
ECHO -clientSecret CLIENTSECRET
ECHO 	Set the clientSecret for the connection. If not set, uses CLIENTSECRET in the environment.
ECHO OFF
ECHO -server SERVER
ECHO 	Set the server for the connection. If not set, uses SERVER in the environment, and if that is not set, uses the test endpoint
ECHO OFF
ECHO file ...
ECHO 	Specify the files to send
