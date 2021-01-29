# CDC COVID-19 Vaccination Reporting Specification (CVRS) – What’s New in Version 2

## Introduction

This document catalogs the changes from version 1 to version 2 of the
CDC COVID-19 Vaccination Reporting Specification (CVRS).

## Instruction Changes

  - Removed all references to missed appointments

  - Added language about reporting requirements in “Introduction”

  - Added line termination guidance in “Data Conventions”

  - Added character case guidance in “Data Conventions”

  - Added submitting entity code to the file name in “Extract File
    Conventions”

  - Added file record limit to “Extract File Conventions”

  - Added “for COVID-19 vaccines” to “Daily Extract Criteria”

  - Added guidance for historical vaccines to “Daily Extract Criteria”

  - Added guidance on shared events to “Exclusion Criteria”

  - Added guidance for timing of submissions to “Extract Timing”

  - Added version number to bottom of instructions

## Deidentified Extract Fields Changes

  - Shifted field numbers to allow for “Vaccination event ID” to be the
    first field.

  - Shifted field numbers for the removal of “Vaccine administering
    provider suffix” and “Recipient missed vaccination appointment”

  - Removed column “Missed Appointment Data Population Requirements”

| Field                                                     | Change for Version 2                                                                                                                                                                            |
| --------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Field 1: Vaccination event ID                             | “Field Number” was changed to 1. “This should be a unique identifier for each vaccination event” was added to the Data Element Description and Extract Guidance.                                |
| Field 4: Recipient ID                                     | Updated guidance for unique ID to the Data Element Description and Extract Guidance.                                                                                                            |
| Field 36: Administer at location: type                    | Value set was changed to match the “CDC Covid-19 Vaccination Program Provider Agreement” form. “Sample Responses” was changed to 17.                                                            |
| Field 35: VTrckS provider PIN                             | Updated guidance for historical events to the Data Element Description and Extract Guidance. Changed to “Required if known” in the Data Population Requirements.                                |
| Fields 37–42: Administration address                      | “Exception for mobile clinics” was removed and other guidance was added to the Data Element Description and Extract Guidance.                                                                   |
| Field 44: Comorbidity status                              | Updated information in the Data Element Description and Extract Guidance.                                                                                                                       |
| Field 45: Serology results                                | “If the provider knows of any positive serology results, they should report them regardless of whether they conducted the test” was added to the Data Element Description and Extract Guidance. |
| Former Field 43: Vaccine administering provider suffix    | This field has been removed and is not longer requested.                                                                                                                                        |
| Former Field 46: Recipient missed vaccination appointment | This field has been removed and is not longer requested.                                                                                                                                        |
