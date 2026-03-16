package com.safix.checkout.model;

import java.time.LocalDateTime;

public class EnquiryRequest {
    private Long id;
    private String firstName;
    private String lastName;
    private String city;
    private String phone;
    private String email;
    private String topic;
    private String message;
    private boolean whatsappConsent;
    private String ipAddress;
    private String userAgent;
    private String source;
    private LocalDateTime submittedAt;

    public EnquiryRequest() {
        this.submittedAt = LocalDateTime.now();
        this.id = System.currentTimeMillis();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isWhatsappConsent() { return whatsappConsent; }
    public void setWhatsappConsent(boolean whatsappConsent) { this.whatsappConsent = whatsappConsent; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}
