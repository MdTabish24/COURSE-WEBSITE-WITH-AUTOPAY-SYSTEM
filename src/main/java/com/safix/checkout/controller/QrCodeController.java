package com.safix.checkout.controller;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;

@RestController
public class QrCodeController {
    
    @Value("${upi.merchant.id}")
    private String upiId;
    
    @Value("${upi.merchant.name}")
    private String merchantName;
    
    @Value("${upi.course.amount}")
    private String amount;
    
    @GetMapping(value = "/api/qr-code", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] generateQrCode() throws Exception {
        String upiUrl = String.format("upi://pay?pa=%s&pn=%s&am=%s&cu=INR&tn=Elite%%20Course%%20Payment",
                upiId, merchantName.replace(" ", "%20"), amount);
        
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(upiUrl, BarcodeFormat.QR_CODE, 300, 300);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", baos);
        return baos.toByteArray();
    }
}
