package com.ainq.izgateway.extract.validation;

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
