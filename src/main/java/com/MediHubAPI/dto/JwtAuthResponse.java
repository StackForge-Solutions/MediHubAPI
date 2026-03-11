package com.MediHubAPI.dto;

public class JwtAuthResponse {
    private static final String DEFAULT_TOKEN_TYPE = "Bearer";

    private String accessToken;
    private String tokenType;

    public JwtAuthResponse() {
        this.tokenType = DEFAULT_TOKEN_TYPE;
    }

    public JwtAuthResponse(String accessToken) {
        this(accessToken, DEFAULT_TOKEN_TYPE);
    }

    public JwtAuthResponse(String accessToken, String tokenType) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }
}
