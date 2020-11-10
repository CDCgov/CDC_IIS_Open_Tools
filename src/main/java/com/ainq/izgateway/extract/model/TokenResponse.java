package com.ainq.izgateway.extract.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize
public class TokenResponse {
    public String token;
    public long expiration;
}