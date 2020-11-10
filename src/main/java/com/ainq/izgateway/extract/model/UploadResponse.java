package com.ainq.izgateway.extract.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
public class UploadResponse {
    public String id;
    public String[] validationErrors;
    public String[] processingErrors;
    public boolean truncated;
}