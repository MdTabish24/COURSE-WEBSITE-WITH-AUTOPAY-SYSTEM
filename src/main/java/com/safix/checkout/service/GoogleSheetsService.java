package com.safix.checkout.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponseException;
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class GoogleSheetsService {
    private static final JacksonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final long MIN_INTERVAL_MS = 1100L;
    private static final int MAX_RETRIES = 4;
    private static final long BASE_BACKOFF_MS = 250L;
    private static final long MAX_BACKOFF_MS = 5000L;
    private static final Object RATE_LOCK = new Object();
    private static long nextAllowedTimeMs = 0L;
    private final Sheets sheets;
    private final String spreadsheetId;
    private final String enquirySheetName;
    private final String databaseSheetName;
    private final String loginSheetName;

    public GoogleSheetsService(@Value("${google.sheets.spreadsheet-id:}") String spreadsheetId,
                               @Value("${google.sheets.credentials-path:}") String credentialsPath,
                               @Value("${google.sheets.credentials-json:}") String credentialsJson,
                               @Value("${google.sheets.credentials-base64:}") String credentialsBase64,
                               @Value("${google.sheets.enquiry-sheet:Sheet1}") String enquirySheetName,
                               @Value("${google.sheets.database-sheet:Sheet2}") String databaseSheetName,
                               @Value("${google.sheets.login-sheet:Login}") String loginSheetName) {
        this.spreadsheetId = spreadsheetId;
        this.enquirySheetName = enquirySheetName;
        this.databaseSheetName = databaseSheetName;
        this.loginSheetName = loginSheetName;
        this.sheets = buildSheetsClient(credentialsPath, credentialsJson, credentialsBase64);
    }

    public LoginResult appendLogin(LoginRequest request) {
        if (!isConfigured()) {
            return LoginResult.fail("Google Sheets not configured. Add google.sheets.spreadsheet-id and credentials (path/json/base64) in application.properties.");
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
            return EnquiryResult.fail("Google Sheets not configured. Add google.sheets.spreadsheet-id and credentials (path/json/base64) in application.properties.");
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

    private Sheets buildSheetsClient(String credentialsPath, String credentialsJson, String credentialsBase64) {
        boolean hasPath = credentialsPath != null && !credentialsPath.isBlank();
        boolean hasJson = credentialsJson != null && !credentialsJson.isBlank();
        boolean hasBase64 = credentialsBase64 != null && !credentialsBase64.isBlank();
        if (!hasPath && !hasJson && !hasBase64) {
            return null;
        }
        try (InputStream in = openCredentialsStream(credentialsPath, credentialsJson, credentialsBase64)) {
            if (in == null) {
                return null;
            }
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

    private InputStream openCredentialsStream(String credentialsPath, String credentialsJson, String credentialsBase64) {
        try {
            if (credentialsPath != null && !credentialsPath.isBlank()) {
                return Files.newInputStream(Path.of(credentialsPath));
            }
            if (credentialsJson != null && !credentialsJson.isBlank()) {
                String normalized = credentialsJson.replace("\\n", "\n");
                return new ByteArrayInputStream(normalized.getBytes(StandardCharsets.UTF_8));
            }
            if (credentialsBase64 != null && !credentialsBase64.isBlank()) {
                byte[] decoded = Base64.getDecoder().decode(credentialsBase64);
                return new ByteArrayInputStream(decoded);
            }
        } catch (Exception ex) {
            return null;
        }
        return null;
    }

    private boolean isConfigured() {
        return sheets != null && spreadsheetId != null && !spreadsheetId.isBlank();
    }

    private String appendRow(String sheetName, List<Object> row, String label) {
        if (sheetName == null || sheetName.isBlank()) {
            return label + " sheet name is not configured.";
        }
        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                throttleRequests();
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
                if (!isRetryable(ex) || attempt == MAX_RETRIES) {
                    return "Google Sheets error: " + ex.getMessage();
                }
                try {
                    backoffDelay(attempt);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    return "Google Sheets error: interrupted while retrying.";
                }
            }
        }
        return label + " could not be stored.";
    }

    private void throttleRequests() throws InterruptedException {
        synchronized (RATE_LOCK) {
            long now = System.currentTimeMillis();
            long waitMs = nextAllowedTimeMs - now;
            if (waitMs > 0) {
                Thread.sleep(waitMs);
            }
            nextAllowedTimeMs = System.currentTimeMillis() + MIN_INTERVAL_MS;
        }
    }

    private void backoffDelay(int attempt) throws InterruptedException {
        long exp = 1L << attempt;
        long delay = Math.min(MAX_BACKOFF_MS, BASE_BACKOFF_MS * exp);
        long jitter = ThreadLocalRandom.current().nextLong(60L, 180L);
        Thread.sleep(delay + jitter);
    }

    private boolean isRetryable(Exception ex) {
        if (ex instanceof GoogleJsonResponseException jsonEx) {
            int code = jsonEx.getStatusCode();
            return code == 429 || code == 500 || code == 502 || code == 503 || code == 504;
        }
        if (ex instanceof HttpResponseException httpEx) {
            int code = httpEx.getStatusCode();
            return code == 429 || code == 500 || code == 502 || code == 503 || code == 504;
        }
        String message = ex.getMessage();
        return message != null && message.contains("429");
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
