# Mock

The Sender is a command line tool that runs a mock version of the Clearinghouse API for local testing.

## Usage
```
    $ java -jar clearinghouse-mock.jar -Dserver.port=port [-Dfeature=value]
```

-Dserver.port=_port_
: Specify the server port (default is 8080)

-D_feature_=_value_
This is a Spring Boot application using the Jetty Server.  Other features are available for configuration, see
[the Spring Boot documentation](https://docs.spring.io/spring-boot/docs/2.1.9.RELEASE/reference/html/howto-embedded-web-servers.html)
for details.

NOTE: **Do not send PHI to Mock**. Mock uses an insecure port (http:/localhost:8080) and temporary files
to validate the inputs sent to it, which could contain PHI. It attempts to delete these files afterwards
but may fail. It does not otherwise store any data.

## Known Issues

1. extract-validator contains both the Sender console application and a Mock server,
both of which rely on Spring Boot. As a result, running Sender will also launch a local instance of Jetty
listening on port 8080.  If Mock is already using port 8080, Sender will fail to load.
Until these applications are refactored into their own projects, this problem will prevent Sender and Mock
from running at the same time unless the port of Mock is changed from the default value.

See https://github.com/CDCGov/CDC_IIS_Open_Tools/issues/17