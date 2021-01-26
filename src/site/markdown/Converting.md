# File Conversions
The Validator tool can also be used to convert between file formats, with or without validating content.
This capability can be used to:

1. Convert HL7 formatted files to Tab Delimited CVRS format to enable import into Database management tools.
2. Convert Tab Delimited CVRS format files into HL7 Messages for subsequent communication to systems supporting HL7 Messaging formats.

## Converting HL7 Formatted Files

## Converting Tab Delimited CVRS Formatted Files
The input file must contain one or more HL7 VXU Messages. Each new message is expected to beging with an MSH
segment, and each segment must end with a carriage return or newline character.  File Heea  
