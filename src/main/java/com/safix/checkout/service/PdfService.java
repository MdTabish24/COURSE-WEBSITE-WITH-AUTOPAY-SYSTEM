package com.safix.checkout.service;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.safix.checkout.model.Registration;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Service
public class PdfService {
    
    public byte[] generateReceipt(Registration reg) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);
            
            // Header
            Paragraph header = new Paragraph("UNIVERSAL SKILL DEVELOPMENT CENTRE")
                .setFontSize(24)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
            document.add(header);
            
            document.add(new Paragraph("PAYMENT RECEIPT").setFontSize(16).setTextAlignment(TextAlignment.CENTER));
            document.add(new Paragraph("\n"));
            
            // Details
            document.add(new Paragraph("Receipt ID: " + reg.getId()));
            document.add(new Paragraph("Name: " + reg.getName()));
            document.add(new Paragraph("Email: " + reg.getEmail()));
            document.add(new Paragraph("WhatsApp: " + reg.getWhatsapp()));
            document.add(new Paragraph("Date: " + reg.getRegisteredAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))));
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Amount Paid: ₹4999").setBold());
            document.add(new Paragraph("\n"));
            document.add(new Paragraph("Thank you for your purchase!").setTextAlignment(TextAlignment.CENTER));
            
            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
}
