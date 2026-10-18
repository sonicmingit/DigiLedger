package com.digiledger.backend.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public class LoginProperties {
    private String password = "";
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
