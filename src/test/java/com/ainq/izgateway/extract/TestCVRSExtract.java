package com.ainq.izgateway.extract;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.ainq.izgateway.extract.CVRSExtract.FieldLocator;
class TestCVRSExtract {
    private static CVRSExtract init(CVRSExtract e) {
        e.setVax_event_id("good4_sample4");
        e.setExt_type("D");
        e.setPprl_id("");
        e.setRecip_first_name("Redactable First Name");
        e.setRecip_middle_name("Redactable Middle Name");
        e.setRecip_last_name("Redactable Last Name");
        e.setRecip_dob("2015-08-26");
        e.setRecip_sex("F");
        e.setRecip_address_street("Redactable Address Street");
        e.setRecip_address_street_2("Redactable Address Street 2");
        e.setRecip_address_city("Redactable City");
        e.setRecip_address_county("26001");
        e.setRecip_address_state("MI");
        e.setRecip_address_zip("48787");
        e.setRecip_race_1("UNK");
        e.setRecip_race_2("");
        e.setRecip_race_3("");
        e.setRecip_race_4("");
        e.setRecip_race_5("");
        e.setRecip_race_6("");
        e.setRecip_ethnicity("UNK");
        e.setAdmin_date("2020-10-04");
        e.setCvx("900");
        e.setNdc("80777-9999-00");
        e.setMvx("AAA");
        e.setLot_number("M9999-05");
        e.setVax_expiration("2020-11-01");
        e.setVax_admin_site("RVL");
        e.setVax_route("C38276");
        e.setDose_num("1");
        e.setVax_series_complete("No");
        e.setResponsible_org("COVID-D");
        e.setAdmin_name("Library");
        e.setVtrcks_prov_pin("577689");
        e.setAdmin_type("1");
        e.setAdmin_address_street("admin Street");
        e.setAdmin_address_street_2("admin Street 2");
        e.setAdmin_address_city("admin City");
        e.setAdmin_address_county("");
        e.setAdmin_address_state("DC");
        e.setAdmin_address_zip("26011");
        e.setVax_prov_suffix("OTH");
        e.setVax_refusal("No");
        e.setCmorbid_status("UNK");
        e.setSerology("UNK");
        return e;
    }

    @Test public void testHashCode() {
        CVRSExtract ex = new CVRSExtract();
        int hashCode = ex.hashCode();
        init(ex);
        assertNotEquals(hashCode, ex.hashCode());
    }

    @Test public void testEquals() {
        CVRSExtract blank = new CVRSExtract();
        CVRSExtract ex = init(new CVRSExtract());
        assertEquals(blank, blank);
        assertEquals(ex, ex);
        assertNotEquals(ex, blank);
    }

    @Test public void testRedact() {
        CVRSExtract ex = init(new CVRSExtract());
        assertFalse(ex.isRedacted());
        FieldLocator locator = f -> {
            f.setAccessible(true);
            String value = (String) f.get(ex);
            return value != null && value.contains("Redactable");
        };
        assertNotNull(ex.locateField(locator));
        ex.redact();
        assertTrue(ex.isRedacted());
        assertNull(ex.locateField(locator));
        locator = f -> {
            f.setAccessible(true);
            String value = (String) f.get(ex);
            return value != null && value.contains("Redacted");
        };
        assertNotNull(ex.locateField(locator));
    }

    @Test public void testSetGet() {
        CVRSExtract e = init(new CVRSExtract());
        assertTrue(e.getVax_event_id().equals("good4_sample4"));
        assertTrue(e.getExt_type().equals("D"));
        assertTrue(e.getPprl_id().equals(""));
        assertTrue(e.getRecip_first_name().equals("Redactable First Name"));
        assertTrue(e.getRecip_middle_name().equals("Redactable Middle Name"));
        assertTrue(e.getRecip_last_name().equals("Redactable Last Name"));
        assertTrue(e.getRecip_dob().equals("2015-08-26"));
        assertTrue(e.getRecip_sex().equals("F"));
        assertTrue(e.getRecip_address_street().equals("Redactable Address Street"));
        assertTrue(e.getRecip_address_street_2().equals("Redactable Address Street 2"));
        assertTrue(e.getRecip_address_city().equals("Redactable City"));
        assertTrue(e.getRecip_address_county().equals("26001"));
        assertTrue(e.getRecip_address_state().equals("MI"));
        assertTrue(e.getRecip_address_zip().equals("48787"));
        assertTrue(e.getRecip_race_1().equals("UNK"));
        assertTrue(e.getRecip_race_2().equals(""));
        assertTrue(e.getRecip_race_3().equals(""));
        assertTrue(e.getRecip_race_4().equals(""));
        assertTrue(e.getRecip_race_5().equals(""));
        assertTrue(e.getRecip_race_6().equals(""));
        assertTrue(e.getRecip_ethnicity().equals("UNK"));
        assertTrue(e.getAdmin_date().equals("2020-10-04"));
        assertTrue(e.getCvx().equals("900"));
        assertTrue(e.getNdc().equals("80777-9999-00"));
        assertTrue(e.getMvx().equals("AAA"));
        assertTrue(e.getLot_number().equals("M9999-05"));
        assertTrue(e.getVax_expiration().equals("2020-11-01"));
        assertTrue(e.getVax_admin_site().equals("RVL"));
        assertTrue(e.getVax_route().equals("C38276"));
        assertTrue(e.getDose_num().equals("1"));
        assertTrue(e.getVax_series_complete().equals("No"));
        assertTrue(e.getResponsible_org().equals("COVID-D"));
        assertTrue(e.getAdmin_name().equals("Library"));
        assertTrue(e.getVtrcks_prov_pin().equals("577689"));
        assertTrue(e.getAdmin_type().equals("1"));
        assertTrue(e.getAdmin_address_street().equals("admin Street"));
        assertTrue(e.getAdmin_address_street_2().equals("admin Street 2"));
        assertTrue(e.getAdmin_address_city().equals("admin City"));
        assertTrue(e.getAdmin_address_county().equals(""));
        assertTrue(e.getAdmin_address_state().equals("DC"));
        assertTrue(e.getAdmin_address_zip().equals("26011"));
        assertTrue(e.getVax_prov_suffix().equals("OTH"));
        assertTrue(e.getVax_refusal().equals("No"));
        assertTrue(e.getCmorbid_status().equals("UNK"));
        assertTrue(e.getSerology().equals("UNK"));
    }
}
