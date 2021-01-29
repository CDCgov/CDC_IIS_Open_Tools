package com.ainq.izgateway.extract;
/*
 * Copyright 2020 Audiacious Inquiry, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
public interface CVRS extends Cloneable {

    public CVRS clone();

    public String getExt_type();
    /**
     * @param ext_type the ext_type to set
     */
    public void setExt_type(String ext_type);
    /**
     * @return the pprl_id
     */
    public String getPprl_id();
    /**
     * @param pprl_id the pprl_id to set
     */
    public void setPprl_id(String pprl_id);
    /**
     * @return the recip_id
     */
    public String getRecip_id();
    /**
     * @param recip_id the recip_id to set
     */
    public void setRecip_id(String recip_id);
    /**
     * @return the recip_first_name
     */
    public String getRecip_first_name();
    /**
     * @param recip_first_name the recip_first_name to set
     */
    public void setRecip_first_name(String recip_first_name);
    /**
     * @return the recip_middle_name
     */
    public String getRecip_middle_name();
    /**
     * @param recip_middle_name the recip_middle_name to set
     */
    public void setRecip_middle_name(String recip_middle_name);
    /**
     * @return the recip_last_name
     */
    public String getRecip_last_name();
    /**
     * @param recip_last_name the recip_last_name to set
     */
    public void setRecip_last_name(String recip_last_name);
    /**
     * @return the recip_dob
     */
    public String getRecip_dob();
    /**
     * @param recip_dob the recip_dob to set
     */
    public void setRecip_dob(String recip_dob);
    /**
     * @return the recip_sex
     */
    public String getRecip_sex();
    /**
     * @param recip_sex the recip_sex to set
     */
    public void setRecip_sex(String recip_sex);
    /**
     * @return the recip_address_street
     */
    public String getRecip_address_street();
    /**
     * @param recip_address_street the recip_address_street to set
     */
    public void setRecip_address_street(String recip_address_street);
    /**
     * @return the recip_address_street_2
     */
    public String getRecip_address_street_2();
    /**
     * @param recip_address_street_2 the recip_address_street_2 to set
     */
    public void setRecip_address_street_2(String recip_address_street_2);
    /**
     * @return the recip_address_city
     */
    public String getRecip_address_city();
    /**
     * @param recip_address_city the recip_address_city to set
     */
    public void setRecip_address_city(String recip_address_city);
    /**
     * @return the recip_address_county
     */
    public String getRecip_address_county();
    /**
     * @param recip_address_county the recip_address_county to set
     */
    public void setRecip_address_county(String recip_address_county);
    /**
     * @return the recip_address_state
     */
    public String getRecip_address_state();
    /**
     * @param recip_address_state the recip_address_state to set
     */
    public void setRecip_address_state(String recip_address_state);
    /**
     * @return the recip_address_zip
     */
    public String getRecip_address_zip();
    /**
     * @param recip_address_zip the recip_address_zip to set
     */
    public void setRecip_address_zip(String recip_address_zip);
    /**
     * @return the recip_race_1
     */
    public String getRecip_race_1();
    /**
     * @param recip_race_1 the recip_race_1 to set
     */
    public void setRecip_race_1(String recip_race_1);
    /**
     * @return the recip_race_2
     */
    public String getRecip_race_2();
    /**
     * @param recip_race_2 the recip_race_2 to set
     */
    public void setRecip_race_2(String recip_race_2);
    /**
     * @return the recip_race_3
     */
    public String getRecip_race_3();
    /**
     * @param recip_race_3 the recip_race_3 to set
     */
    public void setRecip_race_3(String recip_race_3);
    /**
     * @return the recip_race_4
     */
    public String getRecip_race_4();
    /**
     * @param recip_race_4 the recip_race_4 to set
     */
    public void setRecip_race_4(String recip_race_4);
    /**
     * @return the recip_race_5
     */
    public String getRecip_race_5();
    /**
     * @param recip_race_5 the recip_race_5 to set
     */
    public void setRecip_race_5(String recip_race_5);
    /**
     * @return the recip_race_6
     */
    public String getRecip_race_6();
    /**
     * @param recip_race_6 the recip_race_6 to set
     */
    public void setRecip_race_6(String recip_race_6);
    /**
     * @return the recip_ethnicity
     */
    public String getRecip_ethnicity();
    /**
     * @param recip_ethnicity the recip_ethnicity to set
     */
    public void setRecip_ethnicity(String recip_ethnicity);
    /**
     * @return the vax_event_id
     */
    public String getVax_event_id();
    /**
     * @param vax_event_id the vax_event_id to set
     */
    public void setVax_event_id(String vax_event_id);
    /**
     * @return the admin_date
     */
    public String getAdmin_date();
    /**
     * @param admin_date the admin_date to set
     */
    public void setAdmin_date(String admin_date);
    /**
     * @return the cvx
     */
    public String getCvx();
    /**
     * @param cvx the cvx to set
     */
    public void setCvx(String cvx);
    /**
     * @return the ndc
     */
    public String getNdc();
    /**
     * @param ndc the ndc to set
     */
    public void setNdc(String ndc);
    /**
     * @return the mvx
     */
    public String getMvx();
    /**
     * @param mvx the mvx to set
     */
    public void setMvx(String mvx);
    /**
     * @return the lot_number
     */
    public String getLot_number();
    /**
     * @param lot_number the lot_number to set
     */
    public void setLot_number(String lot_number);
    /**
     * @return the vax_expiration
     */
    public String getVax_expiration();
    /**
     * @param vax_expiration the vax_expiration to set
     */
    public void setVax_expiration(String vax_expiration);
    /**
     * @return the vax_admin_site
     */
    public String getVax_admin_site();
    /**
     * @param vax_admin_site the vax_admin_site to set
     */
    public void setVax_admin_site(String vax_admin_site);
    /**
     * @return the vax_route
     */
    public String getVax_route();
    /**
     * @param vax_route the vax_route to set
     */
    public void setVax_route(String vax_route);
    /**
     * @return the dose_num
     */
    public String getDose_num();
    /**
     * @param dose_num the dose_num to set
     */
    public void setDose_num(String dose_num);
    /**
     * @return the vax_series_complete
     */
    public String getVax_series_complete();
    /**
     * @param vax_series_complete the vax_series_complete to set
     */
    public void setVax_series_complete(String vax_series_complete);
    /**
     * @return the responsible_org
     */
    public String getResponsible_org();
    /**
     * @param responsible_org the responsible_org to set
     */
    public void setResponsible_org(String responsible_org);
    /**
     * @return the admin_name
     */
    public String getAdmin_name();
    /**
     * @param admin_name the admin_name to set
     */
    public void setAdmin_name(String admin_name);
    /**
     * @return the vtrcks_prov_pin
     */
    public String getVtrcks_prov_pin();
    /**
     * @param vtrcks_prov_pin the vtrcks_prov_pin to set
     */
    public void setVtrcks_prov_pin(String vtrcks_prov_pin);
    /**
     * @return the admin_type
     */
    public String getAdmin_type();
    /**
     * @param admin_type the admin_type to set
     */
    public void setAdmin_type(String admin_type);
    /**
     * @return the admin_address_street
     */
    public String getAdmin_address_street();
    /**
     * @param admin_address_street the admin_address_street to set
     */
    public void setAdmin_address_street(String admin_address_street);
    /**
     * @return the admin_address_street_2
     */
    public String getAdmin_address_street_2();
    /**
     * @param admin_address_street_2 the admin_address_street_2 to set
     */
    public void setAdmin_address_street_2(String admin_address_street_2);
    /**
     * @return the admin_address_city
     */
    public String getAdmin_address_city();
    /**
     * @param admin_address_city the admin_address_city to set
     */
    public void setAdmin_address_city(String admin_address_city);
    /**
     * @return the admin_address_county
     */
    public String getAdmin_address_county();
    /**
     * @param admin_address_county the admin_address_county to set
     */
    public void setAdmin_address_county(String admin_address_county);
    /**
     * @return the admin_address_state
     */
    public String getAdmin_address_state();
    /**
     * @param admin_address_state the admin_address_state to set
     */
    public void setAdmin_address_state(String admin_address_state);
    /**
     * @return the admin_address_zip
     */
    public String getAdmin_address_zip();
    /**
     * @param admin_address_zip the admin_address_zip to set
     */
    public void setAdmin_address_zip(String admin_address_zip);
    /**
     * @return the vax_prov_suffix
     */
    public String getVax_prov_suffix();
    /**
     * @param vax_prov_suffix the vax_prov_suffix to set
     */
    public void setVax_prov_suffix(String vax_prov_suffix);
    /**
     * @return the vax_refusal
     */
    public String getVax_refusal();
    /**
     * @param vax_refusal the vax_refusal to set
     */
    public void setVax_refusal(String vax_refusal);
    /**
     * @return the cmorbid_status
     */
    public String getCmorbid_status();
    /**
     * @param cmorbid_status the cmorbid_status to set
     */
    public void setCmorbid_status(String cmorbid_status);
    /**
     * @return the recip_missed_appt
     */
    public String getRecip_missed_appt();
    /**
     * @param recip_missed_appt the recip_missed_appt to set
     */
    public void setRecip_missed_appt(String recip_missed_appt);
    /**
     * @return the serology
     */
    public String getSerology();
    /**
     * @param serology the serology to set
     */
    public void setSerology(String serology);

    /**
     * Redact this record.  Sets all fields that have been marked as being redacted
     * to the value "Redacted".
     */
    public void redact();

    /**
     * Determine whether this record meets the redaction requirements.
     * @return true if the record has been redacted, false otherwise
     */
    public boolean isRedacted();
}
