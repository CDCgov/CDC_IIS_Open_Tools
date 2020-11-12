REM @ECHO off 

if NOT "%SERVER%"=="" GOTO skip
REM Set to test endpoint if not set
set SERVER=https://covdch-dev.ohms.oracle.com
:skip

SET REQUEST={ "clientID": ^"%CLIENTID%^", "clientSecret": ^"%CLIENTSECRET%^", "scopes": ["UPLOAD" ] }

for /F "usebackq delims=[]{},: tokens=2" %%f in (`echo %REQUEST% ^| curl --location --request POST %SERVER%/v0/token/gen --header "Content-Type: application/json" --header "Accept: application/json" -data @-`) do set TOKEN=%%~f

curl --location --request POST %SERVER%/v0/upload/cvrs/batch ^
	--header "Content-Type: text/plain" ^
	--header "Authorization: %TOKEN%" ^
	--header "Accept: application/json" ^
	--data-binary @%1