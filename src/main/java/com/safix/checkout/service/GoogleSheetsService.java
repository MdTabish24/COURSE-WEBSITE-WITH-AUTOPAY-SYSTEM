package com.safix.checkout.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AppendValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.safix.checkout.model.EnquiryRequest;
import com.safix.checkout.model.EnquiryResult;
import com.safix.checkout.model.LoginRequest;
import com.safix.checkout.model.LoginResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;

@Service
public class GoogleSheetsService {
    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private final Sheets sheets;
    private final String spreadsheetId;
    private final String enquirySheetName;
    private final String databaseSheetName;
    private final String loginSheetName;

    public GoogleSheetsService(@Value("${google.sheets.spreadsheet-id:}") String spreadsheetId,
                               @Value("${google.sheets.credentials-path:}") String credentialsPath,
                               @Value("${google.sheets.enquiry-sheet:Sheet1}") String enquirySheetName,
                               @Value("${google.sheets.database-sheet:Sheet2}") String databaseSheetName,
                               @Value("${google.sheets.login-sheet:Login}") String loginSheetName) {
        this.spreadsheetId = spreadsheetId;
        this.enquirySheetName = enquirySheetName;
        this.databaseSheetName = databaseSheetName;
        this.loginSheetName = loginSheetName;
        this.sheets = buildSheetsClient(credentialsPath);
    }

    public LoginResult appendLogin(LoginRequest request) {
        if (!isConfigured()) {
            return LoginResult.fail("Google Sheets not configured. Add google.sheets.spreadsheet-id and google.sheets.credentials-path in application.properties.");
        }

        List<Object> row = List.of(
                safe(request.getSubmittedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)),
                safe(request.getId()),
                safe(request.getRole()),
                safe(request.getIdentifier()),
                safe(hashPassword(request.getPassword())),
                safe(request.getIpAddress()),
                safe(request.getUserAgent()),
                safe(request.getSource())
        );

        String loginError = appendRow(loginSheetName, row, "Login");
        if (loginError == null) {
            return LoginResult.ok("Login stored.");
        }
        return LoginResult.fail(loginError);
    }

    public EnquiryResult appendEnquiry(EnquiryRequest request) {
        if (!isConfigured()) {
            return EnquiryResult.fail("Google Sheets not configured. Add google.sheets.spreadsheet-id and google.sheets.credentials-path in application.properties.");
        }

        List<Object> row = List.of(
                safe(request.getSubmittedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)),
                safe(request.getId()),
                safe(request.getFirstName()),
                safe(request.getLastName()),
                safe(request.getCity()),
                safe(request.getPhone()),
                safe(request.getEmail()),
                safe(request.getTopic()),
                safe(request.getMessage()),
                safe(request.isWhatsappConsent()),
                safe(request.getIpAddress()),
                safe(request.getUserAgent()),
                safe(request.getSource())
        );

        String primaryError = appendRow(enquirySheetName, row, "Enquiry");
        if (primaryError != null) {
            return EnquiryResult.fail(primaryError);
        }
        if (databaseSheetName != null && !databaseSheetName.isBlank()) {
            String databaseError = appendRow(databaseSheetName, row, "Database");
            if (databaseError != null) {
                return EnquiryResult.fail(databaseError);
            }
        }
        return EnquiryResult.ok("Enquiry stored.");
    }

    private Sheets buildSheetsClient(String credentialsPath) {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            return null;
        }
        try (InputStream in = Files.newInputStream(Path.of(credentialsPath))) {
            GoogleCredentials credentials = GoogleCredentials.fromStream(in)
                    .createScoped(List.of(SheetsScopes.SPREADSHEETS));
            HttpRequestInitializer initializer = new HttpCredentialsAdapter(credentials);
            return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, initializer)
                    .setApplicationName("Universal Skills Website")
                    .build();
        } catch (Exception ex) {
            return null;
        }
    }

    private boolean isConfigured() {
        return sheets != null && spreadsheetId != null && !spreadsheetId.isBlank();
    }

    private String appendRow(String sheetName, List<Object> row, String label) {
        if (sheetName == null || sheetName.isBlank()) {
            return label + " sheet name is not configured.";
        }
        try {
            ValueRange body = new ValueRange().setValues(List.of(row));
            AppendValuesResponse response = sheets.spreadsheets().values()
                    .append(spreadsheetId, sheetName, body)
                    .setValueInputOption("RAW")
                    .setInsertDataOption("INSERT_ROWS")
                    .execute();
            if (response.getUpdates() != null) {
                return null;
            }
            return label + " could not be stored.";
        } catch (Exception ex) {
            return "Google Sheets error: " + ex.getMessage();
        }
    }

    private Object safe(Object value) {
        return value == null ? "" : value;
    }

    private String hashPassword(String password) {
        if (password == null || password.isBlank()) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encoded);
        } catch (Exception ex) {
            return "";
        }
    }
}
