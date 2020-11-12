REM @ECHO off

if NOT "%SERVER%"=="" GOTO skip
REM Set to test endpoint if not set
set SERVER=https://covdch-dev.ohms.oracle.com
:skip

REM Create a request to generate a new token using CLIENTID and CLIENTSECRET
SET REQUEST={ "clientID": ^"%CLIENTID%^", "clientSecret": ^"%CLIENTSECRET%^", "scopes": ["UPLOAD" ] }

REM Parse the token from the response
for /F "usebackq delims=[]{},: tokens=2" %%f in (`echo %REQUEST% ^| curl --location --request POST %SERVER%/v0/token/gen --header "Content-Type: application/json" --header "Accept: application/json" -data @-`) do set TOKEN=%%~f

REM Remove REQUEST from the environment (it contains CLIENTSECRET)
SET REQUEST=

REM Send the requested file too the server
curl --location --request POST %SERVER%/v0/upload/cvrs/batch ^
	--header "Content-Type: text/plain" ^
	--header "Authorization: %TOKEN%" ^
	--header "Accept: application/json" ^
	--data-binary @%1

REM Remove TOKEN from the environment.
SET TOKEN=