package com.ainq.izgateway.extract.annotations;

public enum ExtractType {
    REDACTED("D"), PPRL("P"), IDENTIFIED("I");
    private String code;
    private ExtractType(String code) {
        this.code = code;
    }
    public String getCode() {
        return code;
    }
}
