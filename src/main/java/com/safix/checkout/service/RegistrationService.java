package com.safix.checkout.service;

import com.safix.checkout.model.Registration;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;

@Service
public class RegistrationService {
    
    @Autowired
    private EmailService emailService;
    
    private static final String UPLOAD_DIR = "uploads/screenshots/";
    private static final String EXCEL_FILE = "registrations.xlsx";
    
    public Registration saveRegistration(String name, String whatsapp, String email, MultipartFile screenshot) throws IOException {
        // Save screenshot
        String filename = System.currentTimeMillis() + "_" + screenshot.getOriginalFilename();
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        Files.copy(screenshot.getInputStream(), uploadPath.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
        
        // Create registration object
        Registration reg = new Registration();
        reg.setName(name);
        reg.setWhatsapp(whatsapp);
        reg.setEmail(email);
        reg.setPaymentScreenshot(filename);
        
        // Save to Excel (primary storage)
        exportToExcel(reg);
        
        // Send receipt email
        emailService.sendReceipt(reg);
        reg.setReceiptSent("YES");
        
        return reg;
    }
    
    private void exportToExcel(Registration reg) throws IOException {
        Workbook workbook;
        Sheet sheet;
        int rowNum;
        
        File file = new File(EXCEL_FILE);
        if (file.exists()) {
            FileInputStream fis = new FileInputStream(file);
            workbook = new XSSFWorkbook(fis);
            sheet = workbook.getSheetAt(0);
            rowNum = sheet.getLastRowNum() + 1;
            fis.close();
        } else {
            workbook = new XSSFWorkbook();
            sheet = workbook.createSheet("Registrations");
            
            // Create header row with styling
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            
            String[] headers = {"ID", "Name", "WhatsApp", "Email", "Screenshot", "Registered At", "Receipt Sent"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.autoSizeColumn(i);
            }
            rowNum = 1;
        }
        
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(reg.getId());
        row.createCell(1).setCellValue(reg.getName());
        row.createCell(2).setCellValue(reg.getWhatsapp());
        row.createCell(3).setCellValue(reg.getEmail());
        row.createCell(4).setCellValue(reg.getPaymentScreenshot());
        row.createCell(5).setCellValue(reg.getRegisteredAt().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));
        row.createCell(6).setCellValue(reg.getReceiptSent() != null ? reg.getReceiptSent() : "PENDING");
        
        FileOutputStream fos = new FileOutputStream(EXCEL_FILE);
        workbook.write(fos);
        workbook.close();
        fos.close();
    }
}
