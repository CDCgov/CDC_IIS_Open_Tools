# Release Notes for 1.0.0
This is the first release of the validation tools.  It includes the following files:

* validator-extract-1.0.0.jar Command Line tool for validation and File Conversion
* validator-extract-lib-1.0.0.zip Library for use of software with other applications.
* extract-validator-javadoc-1.0.0.jar Java Doc for tools
* extract-validator-sources-1.0.0.jar Source Code for tools
* extract-validator-site-1.0.0.zip Documentation Files

### Changes since last release

#### [Validator](Validator.html)
1. CVX, MVX and NDC Value Sets have been updated to include the [provisional codes](https://www.cdc.gov/vaccines/programs/iis/code-sets.html) for Moderna and Pfizer vaccines.
2. Added default capability for HL7 Message Conversion and Validation.  Missing HL7 segments will result in use of
appropriate UNK values in the resulting tab delimited formats. This is enabled using the -d option on the Validator.
3. Updated Summary report to make it more useful for validation. The new report summarized by error code and field name.
4. Added checks for correct header names and tab delimited format.
5. Added PW and FM State codes for Palau and Federated States of Micronesia
6. Added a capability to produce a report in JSON format

#### [Sender](Sender.html)
This is a new tool that will send files to the COVID-19 test endpoint (or other designated endpoint).

#### [Mock](Mock.html)
This is a new tool that provides runs local mock server imitating the Clearinghouse API for testing
automation locally.

#### [Batch Upload Script](Scripts.html)
A [CURL](https://curl.haxx.se/) Script for uploading via the command line is provided in the `scripts` folder of the Source Code
archive.

# Release Notes for 1.0.0beta
The 1.0.0 Beta release is an early release of the tools to support Validation and Conversion of Tab Delimited and HL7 V2
CVRS Extract files.

It includes the following:

* validator-extract-1.0.0beta.jar Command Line tool for validation and File Conversion
* extract-validator-javadoc-1.0.0beta.jar Java Doc for tools
* extract-validator-sources-1.0.0beta.jar Source Code for tools
* extract-validator-site-1.0.0beta.zip Documentation Files
* Value Set files are included in Source Code
