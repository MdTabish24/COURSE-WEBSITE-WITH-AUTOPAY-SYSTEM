package com.safix.checkout.controller;

import com.safix.checkout.model.Registration;
import com.safix.checkout.service.RegistrationService;
import com.safix.checkout.service.SyllabusService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class CheckoutController {
    
    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private SyllabusService syllabusService;
    
    @GetMapping("/")
    public String index() {
        return "index";
    }
    
    @GetMapping("/checkout")
    public ModelAndView checkout(
            @RequestParam(required = false, defaultValue = "Elite Course") String course,
            @RequestParam(required = false, defaultValue = "5000") String price) {
        
        ModelAndView mav = new ModelAndView("checkout-form");
        mav.addObject("selectedCourse", course);
        mav.addObject("price", price);
        return mav;
    }

    @GetMapping("/syllabus")
    public ModelAndView syllabus(@RequestParam String course) {
        ModelAndView mav = new ModelAndView("syllabus");
        mav.addObject("course", course);
        mav.addObject("sector", syllabusService.getSectorByCourse(course));
        mav.addObject("syllabusText", syllabusService.getSyllabusByCourse(course));
        return mav;
    }
    
    @PostMapping("/register")
    public ModelAndView register(
            @RequestParam String name,
            @RequestParam String whatsapp,
            @RequestParam String email,
            @RequestParam(required = false, defaultValue = "Elite Course") String course,
            @RequestParam("screenshot") MultipartFile screenshot) {
        
        try {
            Registration reg = registrationService.saveRegistration(name, whatsapp, email, screenshot);
            reg.setSelectedCourse(course);
            ModelAndView mav = new ModelAndView("success");
            mav.addObject("registration", reg);
            return mav;
        } catch (Exception e) {
            ModelAndView mav = new ModelAndView("error");
            mav.addObject("message", e.getMessage());
            return mav;
        }
    }
}
