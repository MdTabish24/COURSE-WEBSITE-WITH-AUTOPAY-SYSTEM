package com.safix.checkout.model;

public record EnquiryResult(boolean success, String message) {
    public static EnquiryResult ok(String message) {
        return new EnquiryResult(true, message);
    }

    public static EnquiryResult fail(String message) {
        return new EnquiryResult(false, message);
    }
}
