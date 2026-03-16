package com.safix.checkout.controller;

import com.safix.checkout.model.EnquiryRequest;
import com.safix.checkout.model.EnquiryResult;
import com.safix.checkout.service.GoogleSheetsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EnquiryController {

    @Autowired
    private GoogleSheetsService googleSheetsService;

    @PostMapping("/api/enquiry")
    public EnquiryResult submitEnquiry(@RequestBody EnquiryRequest request,
                                       HttpServletRequest httpRequest) {
        if (request == null) {
            return EnquiryResult.fail("Invalid enquiry payload.");
        }
        request.setIpAddress(httpRequest.getRemoteAddr());
        request.setUserAgent(httpRequest.getHeader("User-Agent"));
        request.setSource("website-enquiry");
        return googleSheetsService.appendEnquiry(request);
    }
}
