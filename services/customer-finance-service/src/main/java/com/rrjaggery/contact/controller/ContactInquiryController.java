package com.rrjaggery.contact.controller;

import com.rrjaggery.common.dto.ApiResponse;
import com.rrjaggery.contact.dto.ContactInquiryRequest;
import com.rrjaggery.contact.service.ContactInquiryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/contact")
public class ContactInquiryController {

    private final ContactInquiryService contactInquiryService;

    public ContactInquiryController(ContactInquiryService contactInquiryService) {
        this.contactInquiryService = contactInquiryService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> submitInquiry(@Valid @RequestBody ContactInquiryRequest inquiry) {
        contactInquiryService.sendInquiry(inquiry);
        return ResponseEntity.ok(ApiResponse.ok(null, "Contact inquiry sent successfully."));
    }
}
