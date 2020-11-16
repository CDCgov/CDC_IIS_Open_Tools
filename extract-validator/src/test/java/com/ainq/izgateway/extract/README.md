# TEST Plan for Validator and Converter

1. All detectable errors reportable by the Validator are verified to be generated for a test case
2. Valid input variations (e.g., Vaccination, Rejection) are tested and do not generate a validation error.
3. Variations of #2 with each possible missing field are tested and do not generate a validation error.
4. HL7 content created from valid messages is comparable to the original input from a tab delimited file
   e.g., For: Tab Delimited -> CVRS Extract Object 1 -> HL7 Message -> CVRS Extract Object 2
   CVRS Extract Object 1 == CVRS Extract Object 2
5. HL7 forms of valid CVRS are also reported as valid
6. HL7 forms of invalid CVRS are also rejected
7. Code Coverage is completed for 90% of runtime code expected to be used in a production environment
   * Command Line Interfaces are not tested with automation.
   * Handling for "unexpected" Exceptions need not have Coverage
