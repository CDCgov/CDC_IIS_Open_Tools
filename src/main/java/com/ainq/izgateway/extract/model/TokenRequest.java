package com.ainq.izgateway.extract.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
public class TokenRequest {
    public String clientID;
    public String clientSecret;
    public String scopes[];
}