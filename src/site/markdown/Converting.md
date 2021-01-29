# File Conversions
The Validator tool can also be used to convert between file formats, with or without validating content.
This capability can be used to:

1. Convert HL7 formatted files to Tab Delimited CVRS format to enable import into Database management tools.
2. Convert Tab Delimited CVRS format files into HL7 Messages for subsequent communication to systems supporting HL7 Messaging formats.

## Converting HL7 Formatted Files

```
java -jar extract-validator-${project-version}/extract-validator-${project-version}.jar -d -c -i file.hl7 ...
```

Each input file must contain one or more HL7 VXU Messages. Each new message is expected to begin with an MSH
segment, and each segment must end with a carriage return or newline character.  File and Batch Header (FHS and BHS)
and Trailer (FTS and BTS) segments in the input file are ignored.

Output files will be created using the same names as the input files with a .txt extension.  The example above would
create file.txt.

## Converting Tab Delimited CVRS Formatted Files
```
java -jar extract-validator-${project-version}/extract-validator-${project-version}.jar -7 -i file.txt
```
Each input file must contain one or more tab delimited records. Each new message is expected to begin on a new line, with
the first line containing the field names.

Output files will be created using the same names as the input files with a .hl7 extension.  The example above would
create file.hl7.

