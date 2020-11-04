package com.ainq.izgateway.extract;

import com.ainq.izgateway.extract.annotations.FieldValidator;
import com.ainq.izgateway.extract.annotations.Requirement;
import com.ainq.izgateway.extract.annotations.RequirementType;
import com.ainq.izgateway.extract.annotations.V2Field;
import com.ainq.izgateway.extract.validation.BeanValidator;
import com.ainq.izgateway.extract.validation.DateValidator;
import com.ainq.izgateway.extract.validation.DateValidatorIfKnown;
import com.ainq.izgateway.extract.validation.DoNotPopulateValidator;
import com.ainq.izgateway.extract.validation.Matches;
import com.ainq.izgateway.extract.validation.RedactedValidator;
import com.ainq.izgateway.extract.validation.ValueSetValidator;
import com.ainq.izgateway.extract.validation.ValueSetValidatorIfKnown;

import static com.ainq.izgateway.extract.annotations.EventType.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.StringUtils;

import com.opencsv.bean.CsvBindByName;

/**
 * A POJO to record Extract Data.
 * This POJO is designed to be imported using OpenCSV and can be validated.
 * @author Keith W. Boone
 */
public class CVRSExtract implements CVRS {

    @FunctionalInterface
    protected static interface FieldProcesser {
        void accept(Field f) throws IllegalArgumentException, IllegalAccessException;
    };

    @FunctionalInterface
    protected static interface FieldLocator {
        boolean found(Field f) throws IllegalArgumentException, IllegalAccessException;
    };

    /** The vaccination event’s unique identifier within the system. */
    @FieldValidator(validator = Matches.class, paramString = "^\\S+$")
    @CsvBindByName
    @Requirement(value=RequirementType.REQUIRED, when = { VACCINATION, REFUSAL, MISSED_APPOINTMENT })
    @V2Field(value = "ORC-3-1")
    private String vax_event_id;

	/** Extract Type defines whether this file contains completely de-identified data, PPRL ID, or fully identifiable data. */
    @FieldValidator(validator = Matches.class, paramString = "^(D|P|I)$")
    @CsvBindByName(required = true)
    @V2Field(value = "MSH-8")
    private String ext_type;

	/** Privacy Preserving Record Linkage ID. */
    @FieldValidator(validator = DoNotPopulateValidator.class)
    @Requirement( value=RequirementType.DO_NOT_SEND, when = { VACCINATION, MISSED_APPOINTMENT, REFUSAL})
    @CsvBindByName
    @V2Field(value = "PID-3(1)-1")
    private String pprl_id;

	/** Unique ID for this recipient. This can be the ID used by your system to uniquely identify the recipient. Or, it can be a randomly assigned unique identifier. However, the Recipient ID must be the same from one report to the next for the same recipient to allow for linking doses to the same Recipient ID. */
    @FieldValidator(validator = Matches.class, paramString = "^\\S+$")
    @CsvBindByName
    @Requirement(value=RequirementType.REQUIRED, when = { VACCINATION, REFUSAL, MISSED_APPOINTMENT })
    @V2Field(value = "PID-3(0)-1")
    private String recip_id;

	/** Recipient's first name */
    @FieldValidator(validator = RedactedValidator.class)
    @CsvBindByName
    @V2Field(value = "PID-5-1")
    private String recip_first_name;

	/** Recipient's middle name */
    @FieldValidator(validator = RedactedValidator.class)
    @CsvBindByName
    @V2Field(value = "PID-5-3")
    private String recip_middle_name;

	/** Recipient's last name */
    @FieldValidator(validator = RedactedValidator.class)
    @CsvBindByName
    @V2Field(value = "PID-5-2")
    private String recip_last_name;

	/** Recipient's date of birth  */
    @FieldValidator(validator = DateValidator.class, paramString = "yyyy-MM-dd|yyyy-MM|yyyy")
    @CsvBindByName
    @Requirement(value=RequirementType.REQUIRED, when = { VACCINATION, REFUSAL, MISSED_APPOINTMENT })
    @V2Field(value = "PID-7-1")
    private String recip_dob;

	/** Sex of recipient */
    @FieldValidator(validator = ValueSetValidator.class, paramString = "SEX")
    @CsvBindByName
    @Requirement(value=RequirementType.REQUIRED, when = { VACCINATION, REFUSAL, MISSED_APPOINTMENT })
    @V2Field(value = "PID-8")
    private String recip_sex;

	/** The street component of the recipient's address */
    @FieldValidator(validator = RedactedValidator.class)
    @CsvBindByName
    @V2Field(value = "PID-11-1")
    private String recip_address_street;

	/** The steet 2 component of the recipient's address */
    @FieldValidator(validator = RedactedValidator.class)
    @CsvBindByName
    @V2Field(value = "PID-11-2")
    private String recip_address_street_2;

	/** The city component of the recipient's address */
    @FieldValidator(validator = RedactedValidator.class)
    @CsvBindByName
    @V2Field(value = "PID-11-3")
    private String recip_address_city;

	/** The county component of the recipient's address */
    @FieldValidator(validator = ValueSetValidator.class, paramString = "COUNTY")
    @CsvBindByName
    @V2Field(value = "PID-11-9")
    private String recip_address_county;

	/** The state component of the recipient's address */
    @FieldValidator(validator = ValueSetValidator.class, paramString = "STATE")
    @CsvBindByName
    @V2Field(value = "PID-11-4")
    private String recip_address_state;

	/** The zip code of the recipient's address. 5 digit or 10 digits (with hyphen) are acceptable */
    @FieldValidator(validator = Matches.class, paramString = "\\d{5}(-\\d{4})?")
    @CsvBindByName
    @V2Field(value = "PID-11-5")
    private String recip_address_zip;

	/** Patient's self-reported race */
    @FieldValidator(validator = ValueSetValidator.class, paramString = "RACEWITHUNK")
    @CsvBindByName
    @Requirement(value=RequirementType.REQUIRED, when = { VACCINATION, REFUSAL, MISSED_APPOINTMENT })
    @V2Field(value = "PID-10(0)-1", system="CDCREC")
    private String recip_race_1;

	/** Patient's self-reported race. Fields 2 through 6 support patients with more than 1 reported race. If only one race is reported, fields 2 through 6 are not necessary. */
    @FieldValidator(validator = ValueSetValidatorIfKnown.class, paramString = "RACE")
    @CsvBindByName
    @V2Field(value = "PID-10(1)-1", system="CDCREC")
    private String recip_race_2;

	/** Patient's self-reported race. Fields 2 through 6 support patients with more than 1 reported race. If only one race is reported, fields 2 through 6 are not necessary. */
    @FieldValidator(validator = ValueSetValidatorIfKnown.class, paramString = "RACE")
    @CsvBindByName
    @V2Field(value = "PID-10(2)-1", system="CDCREC")
    private String recip_race_3;

	/** Patient's self-reported race. Fields 2 through 6 support patients with more than 1 reported race. If only one race is reported, fields 2 through 6 are not necessary. */
    @FieldValidator(validator = ValueSetValidatorIfKnown.class, paramString = "RACE")
    @CsvBindByName
    @V2Field(value = "PID-10(3)-1", system="CDCREC")
    private String recip_race_4;

	/** Patient's self-reported race. Fields 2 through 6 support patients with more than 1 reported race. If only one race is reported, fields 2 through 6 are not necessary. */
    @FieldValidator(validator = ValueSetValidatorIfKnown.class, paramString = "RACE")
    @CsvBindByName
    @V2Field(value = "PID-10(4)-1", system="CDCREC")
    private String recip_race_5;

	/** Patient's self-reported race. Fields 2 through 6 support patients with more than 1 reported race. If only one race is reported, fields 2 through 6 are not necessary. */
    @FieldValidator(validator = ValueSetValidatorIfKnown.class, paramString = "RACE")
    @CsvBindByName
    @V2Field(value = "PID-10(5)-1", system="CDCREC")
    private String recip_race_6;

	/** The ancestry of the patient */
    @FieldValidator(validator = ValueSetValidator.class, paramString = "ETHNICITY")
    @CsvBindByName
    @Requirement(value=RequirementType.REQUIRED, when = { VACCINATION, REFUSAL, MISSED_APPOINTMENT })
    @V2Field(value = "PID-22-1", system="CDCREC")
    private String recip_ethnicity;

	/** The date the vaccination event occurred (or was intended to occur) */
    @FieldValidator(validator = DateValidator.class)
    @CsvBindByName
    @Requirement(value=RequirementType.REQUIRED, when = { VACCINATION, REFUSAL, MISSED_APPOINTMENT })
    @V2Field(value = "RXA-3-1")
    private String admin_date;

	/** The vaccine type that was administered. */
    @FieldValidator(validator = ValueSetValidatorIfKnown.class, paramString = "CVX")
    @Requirement(value=RequirementType.DO_NOT_SEND, when = { MISSED_APPOINTMENT })
    @Requirement(value=RequirementType.REQUIRED, when = { VACCINATION, REFUSAL })
    @CsvBindByName
    @V2Field(value = "RXA-5-1", system="CVX")
    private String cvx;

	/** The vaccine product that was administered. Unit of Use (UoU) is preferred if both UoU and Unit of Sale (UoS) are available. */
    @FieldValidator(validator = ValueSetValidatorIfKnown.class, paramString = "NDC")
    @Requirement(value=RequirementType.DO_NOT_SEND, when = { MISSED_APPOINTMENT, REFUSAL })
    @CsvBindByName
    @V2Field(value = "RXA-5-4", system="NDC")
    private String ndc;

	/** The manufacturer of the vaccine administered */
    @FieldValidator(validator = ValueSetValidatorIfKnown.class, paramString = "MVX")
    @Requirement(value=RequirementType.DO_NOT_SEND, when = { MISSED_APPOINTMENT, REFUSAL })
    @CsvBindByName
    @V2Field(value = "RXA-17-1", system="MVX")
    private String mvx;

	/** The lot number of the vaccine administered: Unit of Use (UoU) is preferred if both UoU and Unit of Sale (UoS) are available. */
    @Requirement(value=RequirementType.DO_NOT_SEND, when = { MISSED_APPOINTMENT, REFUSAL })
    @CsvBindByName
    @V2Field(value = "RXA-15")
    private String lot_number;

	/** The expiration date of the vaccine administered. This can either be YYYY-MM-DD or YYYY-MM */
    @FieldValidator(validator = DateValidatorIfKnown.class, paramString = "yyyy-MM-dd|yyyy-MM")
    @Requirement(value=RequirementType.DO_NOT_SEND, when = { MISSED_APPOINTMENT, REFUSAL })
    @CsvBindByName
    @V2Field(value = "RXA-16")
    private String vax_expiration;

	/** The body site of vaccine administration. */
    @FieldValidator(validator = ValueSetValidatorIfKnown.class, paramString = "SITE")
    @Requirement(value=RequirementType.DO_NOT_SEND, when = { MISSED_APPOINTMENT, REFUSAL })
    @CsvBindByName
    @V2Field(value = "RXR-1-1", system="HL70163")
    private String vax_admin_site;

	/** The route of vaccine administration (e.g., oral, subcutaneous) */
    @FieldValidator(validator = ValueSetValidatorIfKnown.class, paramString = "ROUTE")
    @Requirement(value=RequirementType.DO_NOT_SEND, when = { MISSED_APPOINTMENT, REFUSAL })
    @CsvBindByName
    @V2Field(value = "RXR-2-1", system="NCIT")
    private String vax_route;

	/** Dose # in vaccination series provided dose is considered valid (e.g., counts towards immunity). */
    @FieldValidator(validator = ValueSetValidatorIfKnown.class, paramString = "DOSE")
    @Requirement(value=RequirementType.DO_NOT_SEND, when = { MISSED_APPOINTMENT, REFUSAL })
    @Requirement(value=RequirementType.REQUIRED, when = { VACCINATION })
    @CsvBindByName
    @V2Field(value = "OBX-5-1", system="DCHDOSE", obx3="30973-2^Dose number in series^LN")
    private String dose_num;

	/** Report if the vaccination series is complete. Select 'YES' when last valid dose is administered and patient has satisfied the requirements of COVID vaccination. If more doses are recommended select 'NO'. If it is not known (or cannot be calculated), select 'UNK' */
    @FieldValidator(validator = ValueSetValidatorIfKnown.class, paramString = "YES_NO_UNK")
    @Requirement(value=RequirementType.DO_NOT_SEND, when = { MISSED_APPOINTMENT, REFUSAL })
    @Requirement(value=RequirementType.REQUIRED, when = { VACCINATION })
    @CsvBindByName
    @V2Field(value = "OBX-5-1", obx3="59783-1^Status in immunization series^LN")
    private String vax_series_complete;

	/** The name of the the organization that originated and is accountable for the content of the record. This can be thought of as the "parent organization", "Health System", etc. It is related to the Administered at Location field. If an organization has several clinics or facilities, this would be the organization that represents all of those clinics/facilities. */
    @FieldValidator(validator = Matches.class, paramString = "^\\S+.*$")
    @CsvBindByName
    @Requirement(value=RequirementType.REQUIRED, when = { VACCINATION, REFUSAL, MISSED_APPOINTMENT })
    @V2Field(value = "MSH-22-1")
    private String responsible_org;

	/** The name of the facility that reported the vaccination, refusal, or missed appointment. This is the physical clinic or facility owned by the Responsible Organization. It is possible in a small practice setting that Responsible Organization and Administered at Location are one in the same. */
    @FieldValidator(validator = Matches.class, paramString = "^\\S+.*$")
    @CsvBindByName
    @Requirement(value=RequirementType.REQUIRED, when = { VACCINATION, REFUSAL, MISSED_APPOINTMENT })
    @V2Field(value = "RXA-11-4-1")
    private String admin_name;

	/** This is the 6-digit Provider PIN in VTrckS. For VFC Providers, this is the VFC PIN. This ID is being used for linking across data sources, so population is critical. */
    @FieldValidator(validator = Matches.class, paramString = "^\\d{6}?")
    @CsvBindByName
    @Requirement(value=RequirementType.REQUIRED, when = { VACCINATION, REFUSAL, MISSED_APPOINTMENT }, versions= { "1" } )
    @Requirement(value=RequirementType.REQUIRED_IF_KNOWN, when = { VACCINATION, REFUSAL, MISSED_APPOINTMENT }, versions= { "2" } )
    @V2Field(value = "RXA-11-4-2")
    private String vtrcks_prov_pin;

	/** The characteristic of the provider site that reported the vaccination, refusal, or missed appointment */
    @FieldValidator(validator = ValueSetValidatorIfKnown.class, paramString = "1=DCHTYPE1|2=DCHTYPE2")
    @CsvBindByName
    @Requirement(value=RequirementType.REQUIRED, when = { VACCINATION, REFUSAL})
    @V2Field(value = "RXA-11-6-1")
    private String admin_type;

	/** The street component of where the vaccine is being administered (or was intended to be administered) (i.e. the administered at location). */
    @CsvBindByName
    @V2Field(value = "RXA-11-9")
    private String admin_address_street;

	/** The street 2 component of where the vaccine is being administered (or was intended to be administered) (i.e. the administered at location). */
    @CsvBindByName
    @V2Field(value = "RXA-11-10")
    private String admin_address_street_2;

	/** The city component of where the vaccine is being administered (or was intended to be administered) (i.e. the administered at location). */
    @CsvBindByName
    @V2Field(value = "RXA-11-11")
    private String admin_address_city;

	/** The county component of where the vaccine is being administered (or was intended to be administered) (i.e. the administered at location). */
    @FieldValidator(validator = ValueSetValidatorIfKnown.class, paramString = "COUNTY")
    @CsvBindByName
    @V2Field(value = "RXA-11-16")
    private String admin_address_county;

	/** The state component of where the vaccine is being administered (or was intended to be administered) (i.e. the administered at location). */
    @FieldValidator(validator = ValueSetValidatorIfKnown.class, paramString = "STATE")
    @CsvBindByName
    @V2Field(value = "RXA-11-12")
    private String admin_address_state;

	/** The zip code component of where the vaccine is being administered (or was intended to be administered) (i.e. the administered at location). */
    @FieldValidator(validator = Matches.class, paramString = "^(\\d{5}(-\\d{4})?)?$")
    @CsvBindByName
    @V2Field(value = "RXA-11-13")
    private String admin_address_zip;

	/** The professional designation of the person administering the vaccination. (e.g., MD, LPN, RN). May also be referenced as vaccination administering provider type. */
    @FieldValidator(validator = ValueSetValidatorIfKnown.class, paramString = "PROVIDER_SUFFIX")
    @Requirement(value=RequirementType.DO_NOT_SEND, when = { MISSED_APPOINTMENT, REFUSAL }, versions = "1")
    @Requirement(value=RequirementType.DO_NOT_SEND, when = { REFUSAL }, versions = "2")
    @CsvBindByName
    @V2Field(value = "RXA-10-21")
    private String vax_prov_suffix;

	/** Vaccination was refused, select 'Yes'. If the vaccine was an administered, select 'No' */
    @FieldValidator(validator = ValueSetValidator.class, paramString = "YES_NO")
    @CsvBindByName
    @Requirement(value=RequirementType.REQUIRED, when = { VACCINATION, REFUSAL, MISSED_APPOINTMENT })
    @V2Field(value = "RXA-20", map = { "YES", "RE", "NO", "" })
    private String vax_refusal;

    /**
     * Report if the recipient has a comorbidity. Comorbid conditions are coexisting or co-occurring conditions and
     * sometimes also "multimorbidity" or "multiple chronic conditions".
     * If the recipient has at least one of the below options, select Yes.
     *     -Asthma
     *     -Serious Heart Condition
     *     -Liver Disease
     *     -Chronic Lung Disease
     *     -Chronic Kidney Disease
     *     -Diabetes
     *     -Severe Obesity
     *     -Immunocompromised
     *
     *     If the patient is known to not have any Existing Conditions then select No.
     *
     *     If you do not collect (or are unable to calculate) comorbidity status, please populate with UNK"
     */
    @FieldValidator(validator = ValueSetValidator.class, paramString = "YES_NO_UNK")
    @CsvBindByName
    @Requirement(value=RequirementType.REQUIRED, when = { VACCINATION, REFUSAL, MISSED_APPOINTMENT })
    @V2Field(value = "OBX-5-1", obx3="VXC8^Member of Special Risk Group^PHIN VS")
    private String cmorbid_status;

    /** Report if the patient missed their vaccination appointment  */
    @FieldValidator(validator = ValueSetValidator.class, paramString = "YES_NO")
    @CsvBindByName
    @Requirement(value=RequirementType.REQUIRED, when = { VACCINATION, REFUSAL, MISSED_APPOINTMENT }, versions = { "1" })
    @Requirement(value=RequirementType.IGNORE, when = { VACCINATION, REFUSAL, MISSED_APPOINTMENT }, versions = { "2" })
    @V2Field(value = "RXA-20", map = { "YES", "MA", "NO", "" })
    private String recip_missed_appt;

    /** Report if there was a positive Serology (Antibody test) result
     * If you do not collect, please populate with UNK
     */
    @FieldValidator(validator = ValueSetValidator.class, paramString = "YES_NO_UNK")
    @CsvBindByName
    @Requirement(value=RequirementType.REQUIRED, when = { VACCINATION, REFUSAL, MISSED_APPOINTMENT })
    @V2Field(value = "OBX-5", obx3="75505-8^Serological Evidence of Immunity^LN",
        map = { "YES", "YES", "NO", "NO", "UNK", "UNK^Unknown^NullFlavor" } )
    private String serology;

    private static String[] HEADERS = null;

    public CVRSExtract() {

    }

    public CVRSExtract(CVRS extract) {
        copy(extract);
    }

    protected void copy(CVRS extract) {
        // For each field in a CVRSExtract
        Class<? extends CVRS> c = extract.getClass();
        for (String header: getHeaders(null)) {
            try {
                Method m = c.getMethod("get" + StringUtils.capitalize(header));
                String value = (String) m.invoke(extract);
                this.setField(header, value);
            } catch (Exception e) {
                throw new RuntimeException("Unexpected exception during copy");
            }
        }
    }

    public static String[] getHeaders(String version) {
        if (HEADERS == null) {
            List<String> headers = new ArrayList<>();
            for (Field f: CVRSExtract.class.getDeclaredFields()) {
                if ((f.getModifiers() & (Modifier.TRANSIENT|Modifier.STATIC)) == 0) {
                    // If version is unknown, or the field is not known to be ignored in the specified
                    // version, return it as a header.
                    if (version == null ||
                        BeanValidator.getRequirement(f, RequirementType.IGNORE, version) == null) {
                        headers.add(f.getName());
                    }
                }
            }
            HEADERS = headers.toArray(new String[headers.size()]);
        }
        return HEADERS;
    }

    public String getField(String name) {
        Field f;
        try {
            f = getClass().getDeclaredField(name);
            return (String) f.get(this);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    public void setField(String name, String value) {
        Field f;
        try {
            f = getClass().getDeclaredField(name);
            f.set(this, value);
            return;
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    void forEachField(boolean inHL7Order, FieldProcesser action) {
        Field fields[] = getClass().getDeclaredFields();
        if (inHL7Order) {
            Arrays.sort(fields, Converter::compareFields);
        }
        for (Field f: getClass().getDeclaredFields()) {
            if ((f.getModifiers() & (Modifier.TRANSIENT|Modifier.STATIC)) == 0) {
                try {
                    action.accept(f);
                } catch (IllegalArgumentException | IllegalAccessException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        }
    }

    protected Field locateField(FieldLocator locator) {
        for (Field f: getClass().getDeclaredFields()) {
            if ((f.getModifiers() & (Modifier.TRANSIENT|Modifier.STATIC)) == 0) {
                try {
                    if (locator.found(f)) {
                        return f;
                    }
                } catch (IllegalArgumentException | IllegalAccessException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
        }
        return null;
    }

    public String[] getValues() {
        List<String> values = new ArrayList<>();
        forEachField(false, f -> {
            f.setAccessible(true);
            values.add((String) f.get(this));
        });
        return values.toArray(new String[values.size()]);
    }

    public String[] getValues(String names[]) {
        List<String> values = new ArrayList<>();
        for (String name: names) {
            values.add(this.getField(name));
        }
        return values.toArray(new String[values.size()]);
    }

    public int hashCode() {
        int hashCode = 0;
        // I'm lazy. This also means we don't have to rewrite this code every
        // time a new field is added.

        for (String v: getValues()) {
            hashCode += v == null ? 0 : v.hashCode();
            hashCode *= 31;
        }

        if (hashCode == 0) {
            // Force to non-zero after compute
            hashCode = 31;
        }
        return hashCode;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        if (!(o instanceof CVRSExtract)) {
            return false;
        }
        CVRSExtract that = (CVRSExtract) o;
        if (this.hashCode() != that.hashCode()) {
            return false;
        }
        return notEqualsAt(that) == null;
    }

    public Field notEqualsAt(CVRSExtract that) {
        // So lazy.
        return locateField (field -> {
            String o1 = (String) field.get(this);
            String o2 = (String) field.get(that);
            if (o1 != o2) {
                // Treat null == empty string for this comparison
                if (StringUtils.isEmpty(o1) && StringUtils.isEmpty(o2)) {
                    return false;
                }
                if (o1 == null || o2 == null || !o1.equalsIgnoreCase(o2)) {
                    return true;
                }
            }
            return false;
        });
    }

    public CVRSExtract clone() {
        try {
            return (CVRSExtract) super.clone();
        } catch (CloneNotSupportedException e) {
            // This will not happen.
            return null;
        }
    }
	/**
     * @return the ext_type
     */
    public String getExt_type() {
        return ext_type;
    }

	/**
     * @param ext_type the ext_type to set
     */
    public void setExt_type(String ext_type) {
        this.ext_type = ext_type;
    }

	/**
     * @return the pprl_id
     */
    public String getPprl_id() {
        return pprl_id;
    }

	/**
     * @param pprl_id the pprl_id to set
     */
    public void setPprl_id(String pprl_id) {
        this.pprl_id = pprl_id;
    }

	/**
     * @return the recip_id
     */
    public String getRecip_id() {
        return recip_id;
    }

	/**
     * @param recip_id the recip_id to set
     */
    public void setRecip_id(String recip_id) {
        this.recip_id = recip_id;
    }

	/**
     * @return the recip_first_name
     */
    public String getRecip_first_name() {
        return recip_first_name;
    }

	/**
     * @param recip_first_name the recip_first_name to set
     */
    public void setRecip_first_name(String recip_first_name) {
        this.recip_first_name = recip_first_name;
    }

	/**
     * @return the recip_middle_name
     */
    public String getRecip_middle_name() {
        return recip_middle_name;
    }

	/**
     * @param recip_middle_name the recip_middle_name to set
     */
    public void setRecip_middle_name(String recip_middle_name) {
        this.recip_middle_name = recip_middle_name;
    }

	/**
     * @return the recip_last_name
     */
    public String getRecip_last_name() {
        return recip_last_name;
    }

	/**
     * @param recip_last_name the recip_last_name to set
     */
    public void setRecip_last_name(String recip_last_name) {
        this.recip_last_name = recip_last_name;
    }

	/**
     * @return the recip_dob
     */
    public String getRecip_dob() {
        return recip_dob;
    }

	/**
     * @param recip_dob the recip_dob to set
     */
    public void setRecip_dob(String recip_dob) {
        this.recip_dob = recip_dob;
    }

	/**
     * @return the recip_sex
     */
    public String getRecip_sex() {
        return recip_sex;
    }

	/**
     * @param recip_sex the recip_sex to set
     */
    public void setRecip_sex(String recip_sex) {
        this.recip_sex = recip_sex;
    }

	/**
     * @return the recip_address_street
     */
    public String getRecip_address_street() {
        return recip_address_street;
    }

	/**
     * @param recip_address_street the recip_address_street to set
     */
    public void setRecip_address_street(String recip_address_street) {
        this.recip_address_street = recip_address_street;
    }

	/**
     * @return the recip_address_street_2
     */
    public String getRecip_address_street_2() {
        return recip_address_street_2;
    }

	/**
     * @param recip_address_street_2 the recip_address_street_2 to set
     */
    public void setRecip_address_street_2(String recip_address_street_2) {
        this.recip_address_street_2 = recip_address_street_2;
    }

	/**
     * @return the recip_address_city
     */
    public String getRecip_address_city() {
        return recip_address_city;
    }

	/**
     * @param recip_address_city the recip_address_city to set
     */
    public void setRecip_address_city(String recip_address_city) {
        this.recip_address_city = recip_address_city;
    }

	/**
     * @return the recip_address_county
     */
    public String getRecip_address_county() {
        return recip_address_county;
    }

	/**
     * @param recip_address_county the recip_address_county to set
     */
    public void setRecip_address_county(String recip_address_county) {
        this.recip_address_county = recip_address_county;
    }

	/**
     * @return the recip_address_state
     */
    public String getRecip_address_state() {
        return recip_address_state;
    }

	/**
     * @param recip_address_state the recip_address_state to set
     */
    public void setRecip_address_state(String recip_address_state) {
        this.recip_address_state = recip_address_state;
    }

	/**
     * @return the recip_address_zip
     */
    public String getRecip_address_zip() {
        return recip_address_zip;
    }

	/**
     * @param recip_address_zip the recip_address_zip to set
     */
    public void setRecip_address_zip(String recip_address_zip) {
        this.recip_address_zip = recip_address_zip;
    }

	/**
     * @return the recip_race_1
     */
    public String getRecip_race_1() {
        return recip_race_1;
    }

	/**
     * @param recip_race_1 the recip_race_1 to set
     */
    public void setRecip_race_1(String recip_race_1) {
        this.recip_race_1 = recip_race_1;
    }

	/**
     * @return the recip_race_2
     */
    public String getRecip_race_2() {
        return recip_race_2;
    }

	/**
     * @param recip_race_2 the recip_race_2 to set
     */
    public void setRecip_race_2(String recip_race_2) {
        this.recip_race_2 = recip_race_2;
    }

	/**
     * @return the recip_race_3
     */
    public String getRecip_race_3() {
        return recip_race_3;
    }

	/**
     * @param recip_race_3 the recip_race_3 to set
     */
    public void setRecip_race_3(String recip_race_3) {
        this.recip_race_3 = recip_race_3;
    }

	/**
     * @return the recip_race_4
     */
    public String getRecip_race_4() {
        return recip_race_4;
    }

	/**
     * @param recip_race_4 the recip_race_4 to set
     */
    public void setRecip_race_4(String recip_race_4) {
        this.recip_race_4 = recip_race_4;
    }

	/**
     * @return the recip_race_5
     */
    public String getRecip_race_5() {
        return recip_race_5;
    }

	/**
     * @param recip_race_5 the recip_race_5 to set
     */
    public void setRecip_race_5(String recip_race_5) {
        this.recip_race_5 = recip_race_5;
    }

	/**
     * @return the recip_race_6
     */
    public String getRecip_race_6() {
        return recip_race_6;
    }

	/**
     * @param recip_race_6 the recip_race_6 to set
     */
    public void setRecip_race_6(String recip_race_6) {
        this.recip_race_6 = recip_race_6;
    }

	/**
     * @return the recip_ethnicity
     */
    public String getRecip_ethnicity() {
        return recip_ethnicity;
    }

	/**
     * @param recip_ethnicity the recip_ethnicity to set
     */
    public void setRecip_ethnicity(String recip_ethnicity) {
        this.recip_ethnicity = recip_ethnicity;
    }

	/**
     * @return the vax_event_id
     */
    public String getVax_event_id() {
        return vax_event_id;
    }

	/**
     * @param vax_event_id the vax_event_id to set
     */
    public void setVax_event_id(String vax_event_id) {
        this.vax_event_id = vax_event_id;
    }

	/**
     * @return the admin_date
     */
    public String getAdmin_date() {
        return admin_date;
    }

	/**
     * @param admin_date the admin_date to set
     */
    public void setAdmin_date(String admin_date) {
        this.admin_date = admin_date;
    }

	/**
     * @return the cvx
     */
    public String getCvx() {
        return cvx;
    }

	/**
     * @param cvx the cvx to set
     */
    public void setCvx(String cvx) {
        this.cvx = cvx;
    }

	/**
     * @return the ndc
     */
    public String getNdc() {
        return ndc;
    }

	/**
     * @param ndc the ndc to set
     */
    public void setNdc(String ndc) {
        this.ndc = ndc;
    }

	/**
     * @return the mvx
     */
    public String getMvx() {
        return mvx;
    }

	/**
     * @param mvx the mvx to set
     */
    public void setMvx(String mvx) {
        this.mvx = mvx;
    }

	/**
     * @return the lot_number
     */
    public String getLot_number() {
        return lot_number;
    }

	/**
     * @param lot_number the lot_number to set
     */
    public void setLot_number(String lot_number) {
        this.lot_number = lot_number;
    }

	/**
     * @return the vax_expiration
     */
    public String getVax_expiration() {
        return vax_expiration;
    }

	/**
     * @param vax_expiration the vax_expiration to set
     */
    public void setVax_expiration(String vax_expiration) {
        this.vax_expiration = vax_expiration;
    }

	/**
     * @return the vax_admin_site
     */
    public String getVax_admin_site() {
        return vax_admin_site;
    }

	/**
     * @param vax_admin_site the vax_admin_site to set
     */
    public void setVax_admin_site(String vax_admin_site) {
        this.vax_admin_site = vax_admin_site;
    }

	/**
     * @return the vax_route
     */
    public String getVax_route() {
        return vax_route;
    }

	/**
     * @param vax_route the vax_route to set
     */
    public void setVax_route(String vax_route) {
        this.vax_route = vax_route;
    }

	/**
     * @return the dose_num
     */
    public String getDose_num() {
        return dose_num;
    }

	/**
     * @param dose_num the dose_num to set
     */
    public void setDose_num(String dose_num) {
        this.dose_num = dose_num;
    }

	/**
     * @return the vax_series_complete
     */
    public String getVax_series_complete() {
        return vax_series_complete;
    }

	/**
     * @param vax_series_complete the vax_series_complete to set
     */
    public void setVax_series_complete(String vax_series_complete) {
        this.vax_series_complete = vax_series_complete;
    }

	/**
     * @return the responsible_org
     */
    public String getResponsible_org() {
        return responsible_org;
    }

	/**
     * @param responsible_org the responsible_org to set
     */
    public void setResponsible_org(String responsible_org) {
        this.responsible_org = responsible_org;
    }

	/**
     * @return the admin_name
     */
    public String getAdmin_name() {
        return admin_name;
    }

	/**
     * @param admin_name the admin_name to set
     */
    public void setAdmin_name(String admin_name) {
        this.admin_name = admin_name;
    }

	/**
     * @return the vtrcks_prov_pin
     */
    public String getVtrcks_prov_pin() {
        return vtrcks_prov_pin;
    }

	/**
     * @param vtrcks_prov_pin the vtrcks_prov_pin to set
     */
    public void setVtrcks_prov_pin(String vtrcks_prov_pin) {
        this.vtrcks_prov_pin = vtrcks_prov_pin;
    }

	/**
     * @return the admin_type
     */
    public String getAdmin_type() {
        return admin_type;
    }

	/**
     * @param admin_type the admin_type to set
     */
    public void setAdmin_type(String admin_type) {
        this.admin_type = admin_type;
    }

	/**
     * @return the admin_address_street
     */
    public String getAdmin_address_street() {
        return admin_address_street;
    }

	/**
     * @param admin_address_street the admin_address_street to set
     */
    public void setAdmin_address_street(String admin_address_street) {
        this.admin_address_street = admin_address_street;
    }

	/**
     * @return the admin_address_street_2
     */
    public String getAdmin_address_street_2() {
        return admin_address_street_2;
    }

	/**
     * @param admin_address_street_2 the admin_address_street_2 to set
     */
    public void setAdmin_address_street_2(String admin_address_street_2) {
        this.admin_address_street_2 = admin_address_street_2;
    }

	/**
     * @return the admin_address_city
     */
    public String getAdmin_address_city() {
        return admin_address_city;
    }

	/**
     * @param admin_address_city the admin_address_city to set
     */
    public void setAdmin_address_city(String admin_address_city) {
        this.admin_address_city = admin_address_city;
    }

	/**
     * @return the admin_address_county
     */
    public String getAdmin_address_county() {
        return admin_address_county;
    }

	/**
     * @param admin_address_county the admin_address_county to set
     */
    public void setAdmin_address_county(String admin_address_county) {
        this.admin_address_county = admin_address_county;
    }

	/**
     * @return the admin_address_state
     */
    public String getAdmin_address_state() {
        return admin_address_state;
    }

	/**
     * @param admin_address_state the admin_address_state to set
     */
    public void setAdmin_address_state(String admin_address_state) {
        this.admin_address_state = admin_address_state;
    }

	/**
     * @return the admin_address_zip
     */
    public String getAdmin_address_zip() {
        return admin_address_zip;
    }

	/**
     * @param admin_address_zip the admin_address_zip to set
     */
    public void setAdmin_address_zip(String admin_address_zip) {
        this.admin_address_zip = admin_address_zip;
    }

	/**
     * @return the vax_prov_suffix
     */
    public String getVax_prov_suffix() {
        return vax_prov_suffix;
    }

	/**
     * @param vax_prov_suffix the vax_prov_suffix to set
     */
    public void setVax_prov_suffix(String vax_prov_suffix) {
        this.vax_prov_suffix = vax_prov_suffix;
    }

	/**
     * @return the vax_refusal
     */
    public String getVax_refusal() {
        return vax_refusal;
    }

	/**
     * @param vax_refusal the vax_refusal to set
     */
    public void setVax_refusal(String vax_refusal) {
        this.vax_refusal = vax_refusal;
    }

	/**
     * @return the cmorbid_status
     */
    public String getCmorbid_status() {
        return cmorbid_status;
    }

	/**
     * @param cmorbid_status the cmorbid_status to set
     */
    public void setCmorbid_status(String cmorbid_status) {
        this.cmorbid_status = cmorbid_status;
    }

	/**
     * @return the recip_missed_appt
     */
    public String getRecip_missed_appt() {
        return recip_missed_appt;
    }

	/**
     * @param recip_missed_appt the recip_missed_appt to set
     */
    public void setRecip_missed_appt(String recip_missed_appt) {
        this.recip_missed_appt = recip_missed_appt;
    }

	/**
     * @return the serology
     */
    public String getSerology() {
        return serology;
    }

	/**
     * @param serology the serology to set
     */
    public void setSerology(String serology) {
        this.serology = serology;
    }

    /**
     * Redact this record.  Sets all fields that have been marked as being redacted
     * to the value "Redacted".
     *
     * TODO: Change the way we mark redacted fields using annotations.
     */
    public void redact() {
        // For each field
        forEachField(false, f -> {
            FieldValidator val = f.getAnnotation(FieldValidator.class);
            // If it is validated using the RedactedValidator
            if (val != null && val.validator().isAssignableFrom(RedactedValidator.class)) {
                // Set the value to Redacted
                f.set(this, "Redacted");
            }
        });
    }

    /**
     * Determine whether this record meets the redaction requirements.
     * @return true if the record has been redacted, false otherwise
     */
    public boolean isRedacted() {
        // See if we can locate a field that hasn't been redacted that should be
        return locateField(f -> {
            FieldValidator val = f.getAnnotation(FieldValidator.class);
            // If it is validated using the RedactedValidator
            if (val != null && val.validator().isAssignableFrom(RedactedValidator.class)) {
                // Set the value to Redacted
                if (!"Redacted".equalsIgnoreCase((String) f.get(this))) {
                    // If this field is not redacted, return true
                    return true;
                }
            }
            // Otherwise return falls
            return false;
        }) == null; // If we found no unredacted fields, this extract is redacted.
    }
}
