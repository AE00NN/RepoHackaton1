package com.example.demo.security.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignInRequest {
    private String username;
    private String password;

    public SignInRequest() {
    }

    public SignInRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

}