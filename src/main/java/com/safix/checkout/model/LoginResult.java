package com.safix.checkout.model;

public record LoginResult(boolean success, String message) {
    public static LoginResult ok(String message) {
        return new LoginResult(true, message);
    }

    public static LoginResult fail(String message) {
        return new LoginResult(false, message);
    }
}
