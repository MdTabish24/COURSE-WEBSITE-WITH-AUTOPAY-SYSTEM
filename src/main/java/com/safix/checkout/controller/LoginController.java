package com.safix.checkout.controller;

import com.safix.checkout.model.LoginRequest;
import com.safix.checkout.model.LoginResult;
import com.safix.checkout.service.GoogleSheetsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class LoginController {

    @Autowired
    private GoogleSheetsService googleSheetsService;

    @GetMapping("/login")
    public ModelAndView login(@RequestParam(required = false, defaultValue = "user") String role) {
        String normalized = normalizeRole(role);
        ModelAndView mav = new ModelAndView("login");
        mav.addObject("role", normalized);
        mav.addObject("roleLabel", roleLabel(normalized));
        mav.addObject("roleHint", roleHint(normalized));
        return mav;
    }

    @PostMapping("/login")
    public ModelAndView submitLogin(@RequestParam String role,
                                    @RequestParam String identifier,
                                    @RequestParam String password,
                                    HttpServletRequest request) {
        String normalized = normalizeRole(role);
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setRole(normalized);
        loginRequest.setIdentifier(identifier);
        loginRequest.setPassword(password);
        loginRequest.setIpAddress(request.getRemoteAddr());
        loginRequest.setUserAgent(request.getHeader("User-Agent"));
        loginRequest.setSource("website-login");

        LoginResult result = googleSheetsService.appendLogin(loginRequest);
        if (result.success()) {
            ModelAndView mav = new ModelAndView("login-success");
            mav.addObject("roleLabel", roleLabel(normalized));
            mav.addObject("identifier", identifier);
            return mav;
        }

        ModelAndView mav = new ModelAndView("login");
        mav.addObject("role", normalized);
        mav.addObject("roleLabel", roleLabel(normalized));
        mav.addObject("roleHint", roleHint(normalized));
        mav.addObject("error", result.message());
        return mav;
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return "user";
        }
        String normalized = role.trim().toLowerCase();
        if (normalized.equals("faculty") || normalized.equals("admin")) {
            return normalized;
        }
        return "user";
    }

    private String roleLabel(String role) {
        return switch (role) {
            case "faculty" -> "Faculty Login";
            case "admin" -> "Admin Login";
            default -> "User Login";
        };
    }

    private String roleHint(String role) {
        return switch (role) {
            case "faculty" -> "Access your faculty dashboard, class schedules, and student progress.";
            case "admin" -> "Manage students, courses, payments, and reports in one place.";
            default -> "Sign in to view your courses, assessments, and certificates.";
        };
    }
}
