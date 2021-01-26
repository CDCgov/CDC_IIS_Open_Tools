# Release Notes for 2.1.0
Prepared content for publication to CDC Open Source Site.
Fixed formatting for nine digit zip-codes to match CVRS Specification when missing hypen (-) character in input HL7 content.

# Release Notes for 2.0.5
Fixed logic issue for recip_middle_name and recip_street_address_2 not being defaulted correctly for non-redacted inputs

# Release Notes for 2.0.4
Updated the converter to take responsible_org from MSH-4-1 if MSH-22-1 is empty.

# Release Notes for 2.0.3
Reverting change to default admin_name and responsible_org value appropriately based on content.

# Release Notes for 2.0.2
Fixed an issue to default admin_name and responsible_org value appropriately based on content,
and to replace an empty recip_middle_name or recip_street_address_2 with a single space for identified
records converted from HL7 V2.

# Release Notes for 2.0.1
Fixed an issue to default ext_type value appropriately based on content.

# Release Notes for 2.0.0
Refactored the release into a multi-module project to better support web-based use of validation
tools.
Corrected issues for default handling for refusals when converting HL7 messages.

# Release Notes for 1.1.0
In this release, the deliverables have been repackaged to support use of the
validation/conversion tools separately from the web services client and mock
server. Tools now target Java 1.8 (a.k.a. Java 8) to enable them to be used
with older virtual machines (this does not prevent them from being used in
a Java 9 or later edition).

This release will include the following files:

* extract-validator-1.1.0.jar Command Line tool for validation and File Conversion
* extract-validator-lib-1.1.0.zip Library for use of software with other applications.
* extract-validator-javadoc-1.1.0.jar Java Doc for tools
* extract-validator-sources-1.1.0.jar Source Code for tools
* extract-validator-site-1.1.0.zip Documentation Files
* clearinghouse-sender-1.1.0.jar Command Line tool to send data to the clearinghouse.
* clearinghouse-mock-1.1.0.jar Command Line tool to mock the clearinghouse APIs for upload.

# Release Notes for 1.0.1
This is a patch to fix a bug found after release which threw exceptions
on incorrectly formatted dates.

# Release Notes for 1.0.0
This is the first release of the validation tools.  It includes the following files:

* extract-validator-1.0.0.jar Command Line tool for validation and File Conversion
* extract-validator-lib-1.0.0.zip Library for use of software with other applications.
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
