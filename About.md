# About This Project
The purpose of this project is to deliver tools that support production and consumption
of HL7 and flat file formats used to communicate COVID-19 Vaccination Data between IIS
and CDC to support tracking of vaccination progress for COVID-19.

Files included in this project are listed below.

NOTE: Many components are still to be done.  These are marked (TBD).
## CDC Specifications
The specifications from which this project was originally developed include the following,
included in this site:

* [CDC Specifications for the CDC COVID-19 Vaccine Reporting Specification (Release 2)](#todo)
* [What's New Guide](#todo)
* [Data Revision Notes](#todo)

## CURL Scripts
A CURL Script (will be) included to support file uploads.

## Mirth Channel
There is a sample Mirth channel that includes a listener that you can use for local
testing, and a sender that will send files dropped into a folder to the specified destination.

## Java Code
Java code and documentation are included that support Validation of flat files and HL7 Version 2 Messages.

### Validation
```
    $ java -classpath extract-validator.jar com.ainq.izgateway.extract.Validator [options]? [files] ...
```

#### Options
-e  Exit (ignoring further command line arguments)<br/>
-7  Display HL7 Message Format from files<br/>
-c  Display Tab Delimited Format from files<br/>
-b  Display Both HL7 and Tab-Delimited from files<br/>
-n  Display Neither HL7 or Tab-Delimited file content<br/>
-v1 Use Version 1 CVRS Validation Tables<br/>
-v1 Use Version 2 CVRS Validation Tables<br/>
-k  Comment <br/>
-K  End of Comment<br/>

file    An HL7 V2 VXU Message or Tab Delimited File

The type of file will automatically detected from its content.

Tab Delimited Files must conform to the CDC CVRS Tab Delimited Format

HL7 VXU Message Files can contain a single HL7 V2 Message. Each HL7 V2 Segment must
be terminated by a newline or carriage-return or carriage-return/newline pair of
characters.

NOTE: Future support is anticipated for HL7 Batch messages that use the FHS/FFS and
BHS/BFS segments.

### HL7 from/to Flat File Conversion
TBD -- See Converter source code.  Depends on [HAPI V2 Release 2.3](https://hapifhir.github.io/hapi-hl7v2/).

### SOAP Message Sender
Converts a batch file to a sequence of SOAP Messages and sends them to the destination
end point (TBD).

## Sample Messages and Flat Files
These are sample messages and flat files containing various forms of the CVRS Data for
testing.
[Testing Files](src\test\resources)

## Value Set Tables
Value Set tables used for content validation. These are simple space delimited files
containing the code in the first field, and additional helpful data in remaining fields.

* [Value Set Files](src\main\resources)
  * [FIPS County Codes](src\main\resources\COUNTY.txt)
  * [Facility Type Codes (used during enrollment)](src\main\resources\DCHTYPE2.txt)
  * [CVX Codes](src\main\resources\CVX.txt)
  * [MVX Codes](src\main\resources\MVX.txt)
  * [NDC Codes](src\main\resources\NDC.txt)
  * [DOSE Codes (used by DCH for recording Dose Number)](src\main\resources\DOSE.txt)
  * [ETHNICITY Codes](src\main\resources\ETHNICITY.txt)
  * [RACE Codes](src\main\resources\RACE.txt)
  * [PROVIDER SUFFIX Codes](src\main\resources\PROVIDER_SUFFIX.txt)
  * [ROUTE Codes](src\main\resources\ROUTE.txt)
  * [SITE Codes](src\main\resources\SITE.txt)
  * [SEX Codes](src\main\resources\SEX.txt)

## HL7 Profiles for HL7 V2 Messages
HL7 Profiles for V2 Messages containing CVRS Content (TBD)
* Z22.CVRS - An enhanced Z22 containing additional fields to support communication of
CVRS data.
* Z50.CVRSExtract - A message containing only the data needed by DCH, and using the values
found in CVRS Release 2.
* Z22.Identified - A Profile for identified content based on Z22.CVRS ... additional requirements
for sending identified data.
* Z22.Redacted - A Profile for redacted content based on Z22.CVRS ... additional requirements
for sending identified data.
