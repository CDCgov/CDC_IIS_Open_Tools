## Data Clearinghouse - User Documentation
The Data Clearinghouse allows you to submit CVRS and HL7 records for processing by the
CDC through a number of APIs.  You can also export files that have been uploaded on your
behalf from clinic partners with proper credentials. You can also use the APIs to download
data in CVRS or HL7 format that has been received from

## Prerequisites
Before beginning you must obtain the following from CDC:

- client_id and client_secret
- certificate in PFX format
- certificate password

These can be obtained from CDC by ...

> **NOTE:** IIS will need instructions on how this is going to happen.  Do they need
> to make a request?  If so how?  Is this going to be sent to them automatically?
> If so, to whom? Will they need to log in somewhere to download this information?

Software that must be installed is listed below.  You can download a compiled binary version for your operating
system at the links provided below:

- [openssl](https://wiki.openssl.org/index.php/Binaries) - OpenSSL provides a command line tool to convert certificates between
  different formats.  It is used to convert certificates provided CDC to different formats used by IT systems.

- [curl](https://curl.se/download.html) - Curl is a tool that can interact with web-based APIs to
  send and receive information.

## Convert PFX Certificate to PEM Certificate
Certificates provided by CDC will be in PFX format, but will be needed in PEM format
to be used with some versions of CURL. You can use OpenSSL to convert these certificates to PEM format.

- Convert the Certificate to PEM format
```
    openssl pkcs12 -in /path/to/cert.pfx -nokeys -out cert.pem
```

- Convert the Key to PEM format.
```
    openssl pkcs12 -in ~/Downloads/cert_name.pfx -nocerts -out key.pem -nodes
```

> **NOTE:** Are /path/to/cert.pfx and ~/Downloads/cert_name.pfx the same thing?  If so, these should
> have the same text.

> **NOTE:** Certificate keys are secure artifacts used to verify a system's identity,
> and should be protected with a password. This does not seem to be addressed in the commands
> above.

## Generate a Token
Every API call must be submitted with an authorized token.

### <a name="upload-token">Generating a Token for Upload</a>
To obtain a token for __upload__
you can run the following from the command line:

```
curl --cert cert.pem --key key.pem -X POST "https://covdch.cdc.gov/v0/token/gen" -H "accept: application/json" -H "Content-Type: application/json" -d "{\"clientID\":\"YOUR_CLIENT_ID_GOES_HERE\",\"clientSecret\":\"YOUR_CLIENT_SECRET_GOES_HERE\",\"scopes\":[\"UPLOAD\"]}"
```

### <a name="download-token">Generating a Token for Download</a>
To obtain a token for __download__ you can call the following:

```
curl --cert cert.pem --key key.pem -X POST "https://covdch.cdc.gov/v0/token/gen" -H "accept: application/json" -H "Content-Type: application/json" -d "{\"clientID\":\"YOUR_CLIENT_ID_GOES_HERE\",\"clientSecret\":\"YOUR_CLIENT_SECRET_GOES_HERE\",\"scopes\":[\"EXPORT\"]}"
```

### Token Generation Response
The response for either request shown above will look something like the following:
```
{
  "token": "YOUR_TOKEN_HERE"
  "expiration": 1605231395
}
```

This token will last for 15m but can be refreshed for up to 24 hours by calling:

```
curl -X POST "https://covdch.cdc.gov/v0/token/refresh" -H  "accept: application/json" -H  "Authorization: YOUR_TOKEN_HERE" -d ""
```

## Upload a CVRS File
The token must be a token generated with the UPLOAD scope. See the instructions above
for [Generating a Token for Upload](#upload-token).
Files can then be uploaded by running the following command:

```
curl -X POST "https://covdch.cdc.gov/v0/upload/cvrs/batch" -H  "accept: application/json" -H  "Authorization: YOUR_TOKEN_HERE" -H "Content-Type: text/plain" -d @myfile.cvrs
```

> **NOTE:** We have a script that does both token generation and upload that simply
> takes a file name and client-id and client-secret from the environment today in GitHub. This
> can be updated to include the certificate.

## Export a CVRS File
The token must be a token generated with the EXPORT scope. See the instructions above
for [Generating a Token for Download](#download-token).
Files can be downloaded by running the following command:

```
curl -X POST "https://covdch.cdc.gov/v0/upload/export" -H  "accept: application/json" -H  "Authorization: YOUR_TOKEN_HERE" -H "Content-Type: application/json" -d "{\"startDate\":\"2020-11-16\",\"endDate\":\"2020-11-16\",\"format\":\"CVRS\"}"
```

## Export an HL7 File
The token must be a token generated with the EXPORT scope. See the instructions above
for [Generating a Token for Download](#download-token).
Files can be downloaded by running the following command:

```
curl -X POST "https://covdch.cdc.gov/v0/upload/export" -H  "accept: application/json" -H  "Authorization: YOUR_TOKEN_HERE" -H "Content-Type: application/json" -d "{\"startDate\":\"2020-11-16\",\"endDate\":\"2020-11-16\",\"format\":\"HL7\"}"
```
