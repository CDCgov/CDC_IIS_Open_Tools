package com.ainq.izgateway.extract.validation;
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
import java.util.Set;
import com.ainq.izgateway.extract.CVRSExtract;
import com.opencsv.bean.BeanVerifier;

public class NullValidator extends BeanValidator implements BeanVerifier<CVRSExtract> {

    public NullValidator(Set<String> suppressed, String version) {
        super(suppressed, version);
    }

    public boolean verifyBean(CVRSExtract bean) throws CVRSValidationException {
        return true;
    }
}
