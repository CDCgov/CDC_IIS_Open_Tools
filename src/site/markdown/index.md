# About This Project
The purpose of this project is to deliver tools that support production and consumption
of HL7 and flat file formats used to communicate COVID-19 Vaccination Data between IIS
and CDC to support tracking of vaccination progress for COVID-19.

Files included in this project are listed below.

NOTE: Many components are still to be done.  These are marked (TBD).
## CDC Specifications
The specifications from which this project was originally developed include the following,
included in this site:

* [CDC Specifications for the CDC COVID-19 Vaccine Reporting Specification v2 (Excel)](https://github.com/AudaciousInquiry/CDC_IIS_Open_Tools/blob/release_1.0.0beta/doc/CDC_COVID-19_Vaccination_Reporting_Specification_v2_CLEARED_20201029.xlsx)
* [CDC CVRS_What's New in Version 2 (Word)](https://github.com/AudaciousInquiry/CDC_IIS_Open_Tools/blob/release_1.0.0beta/doc/CDC_CVRS_What's_New_in_Version_2_CLEARED_20201028.docx)
* [CDC CVRS Instructions v2 (Word)](https://github.com/AudaciousInquiry/CDC_IIS_Open_Tools/blob/release_1.0.0beta/doc/CDC_CVRS_Instructions_v2_CLEARED_20201028.docx)

## Scripts
The [scripts](https://github.com/AudaciousInquiry/CDC_IIS_Open_Tools/tree/release_1.0.0beta/scripts) folder contains scripts that can be used to support file uploads.
These scripts rely on [CURL](https://curl.haxx.se/) (pronounced either as C-U-R-L or
"curl" as in the wave curls), a widely availablee command line tool for interacting
with WEB APIs.

## Mirth Channel (TBD)
There is a sample [Mirth channel](https://github.com/AudaciousInquiry/CDC_IIS_Open_Tools/tree/release_1.0.0beta/mirth) that includes a listener that you can use for local
testing, and a sender that will send files dropped into a folder to the specified destination.

## Java Code
Java code and documentation are included that support Validation of flat files and HL7 Version 2 Messages.

### Validator

The Validator is a command line tool and collection of Java classes that can be used
to support validation.

Validator is implemented in the `com.ainq.izgateway.extract.Validator` class and has
a command line interface described in the [Validator](Validator.html) description page.

### HL7 from/to Flat File Conversion
TBD -- See Converter source code.  Depends on [HAPI V2 Release 2.3](https://hapifhir.github.io/hapi-hl7v2/).

## SOAP Message Sender
Converts a batch file to a sequence of SOAP Messages and sends them to the destination
end point (TBD).

## Sample Messages and Flat Files
These are sample messages and flat files containing various forms of the CVRS Data for
testing.
[Testing Files](https://github.com/AudaciousInquiry/CDC_IIS_Open_Tools/tree/release_1.0.0beta/src/test/resources)

## Value Set Tables
Value Set tables used for content validation. These are simple space delimited files
containing the code in the first field, and additional helpful data in remaining fields.

  * [Value Set Files](https://github.com/AudaciousInquiry/CDC_IIS_Open_Tools/tree/release_1.0.0beta/src/main/resources)
  * [FIPS County Codes](https://github.com/AudaciousInquiry/CDC_IIS_Open_Tools/tree/release_1.0.0beta/src/main/resources/COUNTY.txt)
  * [Facility Type Codes (used during enrollment)](https://github.com/AudaciousInquiry/CDC_IIS_Open_Tools/tree/release_1.0.0beta/src/main/resources/DCHTYPE2.txt)
  * [CVX Codes](https://github.com/AudaciousInquiry/CDC_IIS_Open_Tools/tree/release_1.0.0beta/src/main/resources/CVX.txt)
  * [MVX Codes](https://github.com/AudaciousInquiry/CDC_IIS_Open_Tools/tree/release_1.0.0beta/src/main/resources/MVX.txt)
  * [NDC Codes](https://github.com/AudaciousInquiry/CDC_IIS_Open_Tools/tree/release_1.0.0beta/src/main/resources/NDC.txt)
  * [DOSE Codes (used by DCH for recording Dose Number)](https://github.com/AudaciousInquiry/CDC_IIS_Open_Tools/tree/release_1.0.0beta/src/main/resources/DOSE.txt)
  * [ETHNICITY Codes](https://github.com/AudaciousInquiry/CDC_IIS_Open_Tools/tree/release_1.0.0beta/src/main/resources/ETHNICITY.txt)
  * [RACE Codes](https://github.com/AudaciousInquiry/CDC_IIS_Open_Tools/tree/release_1.0.0beta/src/main/resources/RACE.txt)
  * [PROVIDER SUFFIX Codes](https://github.com/AudaciousInquiry/CDC_IIS_Open_Tools/tree/release_1.0.0beta/src/main/resources/PROVIDER_SUFFIX.txt)
  * [ROUTE Codes](https://github.com/AudaciousInquiry/CDC_IIS_Open_Tools/tree/release_1.0.0beta/src/main/resources/ROUTE.txt)
  * [SITE Codes](https://github.com/AudaciousInquiry/CDC_IIS_Open_Tools/tree/release_1.0.0beta/src/main/resources/SITE.txt)
  * [SEX Codes](https://github.com/AudaciousInquiry/CDC_IIS_Open_Tools/tree/release_1.0.0beta/src/main/resources/SEX.txt)

## HL7 Profiles for HL7 V2 Messages (TBD)
HL7 Profiles for V2 Messages containing CVRS Content

* Z22.CVRS - An enhanced Z22 containing additional fields to support communication of
CVRS data.
* Z50.CVRSExtract - A message containing only the data needed by DCH, and using the values
found in CVRS Release 2.
* Z22.Redacted - A Profile for redacted content based on Z22/Z22.CVRS ... additional requirements
for sending redacted data.
