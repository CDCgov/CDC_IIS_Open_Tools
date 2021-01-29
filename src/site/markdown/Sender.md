# Sender

The Sender is a command line tool to send files to the COVID-19 Clearinghouse Test Endpoint
and report the status of its content.

## Usage
```
    $ java -cp clearinghouse-sender.jar -DclientId=clientIDValue -DclientSecret=clientSecretValue file ...
```

-DclientId=_clientIDValue_
: Specify the clientId value to use to obtain a token.

-DclientSecret=_clientSecretValue_
: Specify the clientSecret value to use to obtain a token.

-Durl=_endpointUrl_
: Specify the endpointUrl where the data will be sent. This defaults to
the clearinghouse test endpoint.  This endpoint can be change to enable
sending to a production or mock instance of the API for other testing
scenarios.

file ...
: One or more files to send.

## Known Issues
1. Sender does not yet have a capability to configure the X.509 certificate or
the trust store used for sending, so this utility cannot yet be used to send to a
production environment. See https://github.com/CDCGov/CDC_IIS_Open_Tools/issues/16

2. extract-validator contains both the Sender console application and a Mock server (not yet complete),
both of which rely on Spring Boot.  As a result, running Sender will also launch a local instance of Jetty
listening on port 8080.  If other services are already using port 8080, Sender will fail to load.
Until these applications are refactored into their own projects, this problem will prevent two instances
of Sender (or one instance when another application is listening on port 8080) from running at the same time.
See https://github.com/CDCGov/CDC_IIS_Open_Tools/issues/17