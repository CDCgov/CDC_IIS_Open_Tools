# Upload.cmd
`Upload.cmd` is a batch file that relies on [CURL](https://curl.haxx.se/) to send data files to the Clearinghouse
test endpoint.

To use it, your `clientId` and `clientSecret` values normally should be stored in environment variables
named `CLIENTID` and `CLIENTSECRET` respectively, but can also be set using command line arguments.

It will normally send data to the Clearinghouse test endpoint.

To change this behavior set the value of SERVER in your environment to the base URL for the server to send
data to as shown in the example below, or change the value using the -server argument from the command line.
The base URL is the part of the URL before /v0

```
   SET SERVER=http://localhost:8080
```

This can be used to change the endpoint to a production endpoint, or to the endpoint
using the [Mock](Mock.html) application (normally http://localhost:8080)

Usage:

$ upload [-clientId _clientId_] [-clientSecret _clientSecret_] [-server _serverBaseURL_] file ...

Where:

_clientId_
: Is the client identifier issued to you from the COVID-19 Data Clearinghouse for API Access. Setting from the
command line overrides the value of CLIENTID in the environment.

_clientSecret_
: Is the client secret issued to you from the COVID-19 Data Clearinghouse for API Access.  Setting from the
command line overrides the value of CLIENTSECRET in the environment.

_serverBaseURL_
: Is the base URL for the data clearinghouse API endpoint.  Defaults to https://covdch-dev.ohms.oracle.com if not set
from command line or environment.

file
: One or more files to upload