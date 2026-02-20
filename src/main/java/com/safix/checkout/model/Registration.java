package com.safix.checkout.model;

import java.time.LocalDateTime;

public class Registration {
    
    private Long id;
    private String name;
    private String whatsapp;
    private String email;
    private String paymentScreenshot;
    private String selectedCourse;
    private LocalDateTime registeredAt;
    private String receiptSent;
    
    public Registration() {
        this.registeredAt = LocalDateTime.now();
        this.id = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String whatsapp) { this.whatsapp = whatsapp; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPaymentScreenshot() { return paymentScreenshot; }
    public void setPaymentScreenshot(String screenshot) { this.paymentScreenshot = screenshot; }
    
    public String getSelectedCourse() { return selectedCourse; }
    public void setSelectedCourse(String course) { this.selectedCourse = course; }
    
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime time) { this.registeredAt = time; }
    
    public String getReceiptSent() { return receiptSent; }
    public void setReceiptSent(String status) { this.receiptSent = status; }
}
