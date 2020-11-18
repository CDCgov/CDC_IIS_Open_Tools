@ECHO off

if NOT "%SERVER%"=="" GOTO skip
REM Set to test endpoint if not set
set SERVER=https://covdch-dev.ohms.oracle.com
:skip

REM Create a request to generate a new token using CLIENTID and CLIENTSECRET
SET REQUEST={ "clientID": ^"%CLIENTID%^"^^, "clientSecret": ^"%CLIENTSECRET%^"^^, "scopes": ["UPLOAD" ] }

REM Parse the token from the response
for /F "usebackq tokens=1,2 delims=[]{},: " %%f in (`echo %REQUEST% ^| curl --trace curl-trace.txt "%SERVER%/v0/token/gen" --header "Content-Type: application/json" --header "Accept: application/json" --data @-`) do set %%~f=%%~g

REM Remove REQUEST from the environment (it contains CLIENTSECRET)
SET REQUEST=

REM Send the requested file too the server
curl --location --request POST %SERVER%/v0/upload/cvrs/batch ^
	--header "Content-Type: text/plain" ^
	--header "Authorization: %TOKEN%" ^
	--header "Accept: application/json" ^
	--data-binary @%1

REM Remove TOKEN and EXPIRATION from the environment.
SET TOKEN=
SET EXPIRATION=
:end