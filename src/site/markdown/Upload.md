# Upload.cmd
`Upload.cmd` is a batch file that relies on [CURL](https://curl.haxx.se/) to send data files to the Clearinghouse
test endpoint.

To use it, your `clientId` and `clientSecret` values must be stored in environment variables
named `CLIENTID` and `CLIENTSECRET` respectively.

It will normally send data to the Clearinghouse test endpoint.
To change this behavior set the value of SERVER to the base URL for the server to send
data to as shown in the example below. The base URL is the part of the URL before /v0

```
   SET SERVER=http://localhost:8080
```

This can be used to change the endpoint to a production endpoint, or to the endpoint
using the [Mock](Mock.html) application (normally http://localhost:8080)