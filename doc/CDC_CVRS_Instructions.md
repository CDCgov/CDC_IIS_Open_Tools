# CDC COVID-19 Vaccination Reporting Specification (CVRS)

# Introduction

This specification defines the COVID-19 vaccination reporting
requirements to the Centers for Disease Control and Prevention’s (CDC)
Immunization Data Clearinghouse (DCH). Whenever possible, existing and
new connections between provider organizations and immunization
information systems (IISs) should be leveraged to report vaccinations
directly to IISs. This specification addresses how IISs will report
these data to CDC's DCH, as well as how provider organizations that are
unable to report to IISs can still report to CDC's DCH to ensure a
comprehensive accounting of administered doses of COVID-19 vaccine.

Currently, this specification supports the submission of deidentified
case-level data. It includes reference to identified data elements, but
these elements may be populated with “Redacted” or an appropriate coded
value.

In the future, this specification will be expanded to include two
additional models for reporting. All models of reporting will use the
same file format but will vary in what identifying information is
provided.

The specification allows for submission of three different types of
events: (1) vaccination events and (2) vaccine refusals. Each has unique
requirements and is defined in the “Deidentified Extract Format.” Only
vaccination events are required to be reported at this time.

CDC understands that not all IISs collect data about comorbidities or
serology. When these data are not collected by the IIS, the values for
these variables must be reported as “unknown.” It is also understood
that an IIS may not have information about missed appointments or
refusals. These data are only required to be reported if and when
available.

# Data Conventions

1)  > All data shall be output to the extract file as UTF-8 encoded text
    > strings without change.

2)  > Fields in the extract file shall be separated by a tab character.

> **NOTE**: It is possible for some extracted data elements to contain
> one or more-tab (ASCII 09) characters. In such instances, any tab
> characters appearing in data elements shall be mapped to space (ASCII
> 32) characters in the extract file.

3)  > Files can be new-line terminated or carriage-return new-line
    > terminated.

4)  > The first line must be a header row containing the variable names.

5)  > Three data types are used in the extract specification:

<!-- end list -->

  - **String:** Free text fields

  - **Date:** Formatted YYYY-MM-DD or YYYY-MM

  - **Coded Value:** A selection of predefined values. **NOTE**: Many
    coded values are from HL7, with some extensions to support unknown
    and local law/policy restrictions.

<!-- end list -->

6)  > Field-level data population requirements are defined in the
    > extract specification spreadsheet using the following language:

<!-- end list -->

  - **Required:** These fields must have a value. Without a value, these
    records will not be accepted by the DCH. For deidentified reporting,
    some fields default to “Redacted.” In coded values, codes have been
    provided to accommodate local law/policy restrictions or unknown
    values.

  - **Required if known:** These fields may not have a value 100% of the
    time, but if the field value is known, it should be populated.

  - **Do not populate:** There are conditional situations where it does
    not make sense to populate a field (e.g., a lot number for a vaccine
    refusal). In these cases, the expectation is that the field is left
    empty, but still represented in the extract (i.e., do not skip the
    field in the extract).

<!-- end list -->

7)  Data are not case-sensitive.

Extract File Conventions

1)  > Each day a single file will be extracted and sent to CDC’s DCH.

2)  > The extract file shall be named using the format
    > yyyymmdd\_NN\_AAA.txt :

<!-- end list -->

  - yyyy = 4-digit year

  - mm = 2-digit month

  - dd = 2-digit day of month

  - \_NN = sequential count of file for given day (e.g., 01, 02). This
    will likely always be 01.

  - \_AAA = awardee or submitting entity 3-character code. These codes
    are listed on the "Submitting Entity Codes" tab.

The date portion of the file name should represent the date being
reported, not necessarily the date the extract was run.

3\) Individual extract files must not exceed 200,000 records. Data
submissions over 200,000 records must be broken into subsequent files.

Daily Extract Criteria for COVID-19 Vaccines

## Inclusion Criteria

1)  Each extract shall include one specific day of COVID-19 vaccine
    data.

2)  Each extract shall include all newly created vaccination events or
    refusals for the day.

> **NOTE:** At this time updated and/or deleted records cannot be
> processed by the DCH; therefore, they should not be sent.

3)  IISs shall include records even if the patient address is outside of
    their jurisdiction.

4)  Patients who have more than one event in a single day (e.g., a
    refusal and a vaccination event) will have two records (lines) in
    the extract file.

5)  Include both administered and historical vaccines unless the
    vaccination event is a confirmed duplicate.

Exclusion Criteria

1)  Previously submitted vaccination events or refusals shall not be
    submitted a second, third, etc. time even if they are updated or
    deleted.

> **NOTE:** At this time updated and/or deleted records cannot be
> processed by the DCH; therefore, they should not be sent.

2)  Do NOT include newly created COVID-19 vaccination events that have
    been provided to the IIS from another originating jurisdiction (IIS)
    (e.g., through the IZ Gateway or other sharing arrangements).

Extract Timing

1)  Reporting time period: 12:00 am to 11:59 pm.

2)  Deadline for submitting: 12:00 pm local time the following day.

> **NOTE:** Batch processing may be submitted the morning of the
> following day (e.g., prior to 12:00 pm local time the following day)
> to allow for nightly calculations or data quality checks to be
> completed prior to extract and submission to CDC’s DCH.
