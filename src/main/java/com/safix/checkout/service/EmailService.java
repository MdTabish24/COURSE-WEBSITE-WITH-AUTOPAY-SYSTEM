package com.safix.checkout.service;

import com.safix.checkout.model.Registration;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    
    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private PdfService pdfService;
    
    public void sendReceipt(Registration reg) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setTo(reg.getEmail());
            helper.setSubject("Universal Skill Development Centre - Payment Receipt");
            helper.setText(buildEmailBody(reg), true);
            
            // Attach PDF receipt
            byte[] pdfBytes = pdfService.generateReceipt(reg);
            helper.addAttachment("USDC_Receipt_" + reg.getId() + ".pdf", new ByteArrayResource(pdfBytes));
            
            mailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private String buildEmailBody(Registration reg) {
        return "<html><body style='font-family: Inter, sans-serif; background: #0D0D0D; color: #F5F5F7; padding: 20px;'>" +
               "<div style='max-width: 600px; margin: auto; background: #1A1A1A; padding: 30px; border: 2px solid #C5A059;'>" +
               "<h1 style='color: #C5A059; text-align: center;'>UNIVERSAL SKILL DEVELOPMENT CENTRE</h1>" +
               "<h2 style='color: #F5F5F7;'>Welcome, " + reg.getName() + "!</h2>" +
               "<p>Your registration is confirmed. Find your receipt attached.</p>" +
               "<p style='color: #C5A059;'>Access details will be sent to your WhatsApp within 24 hours.</p>" +
               "<hr style='border-color: #C5A059;'/>" +
               "<p style='font-size: 12px; color: #888;'>Registration ID: " + reg.getId() + "</p>" +
               "</div></body></html>";
    }
}
