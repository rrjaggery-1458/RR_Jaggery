package com.rrjaggery.finance.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rrjaggery.common.forms.dto.FormSubmissionRequest;
import com.rrjaggery.common.forms.service.FormRegistryService;
import com.rrjaggery.finance.dto.CreateInvestmentRequest;
import com.rrjaggery.finance.entity.Investment;
import com.rrjaggery.finance.repository.InvestmentRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(prefix = "app.forms", name = "enabled", havingValue = "true")
public class InvestmentService {
    private final InvestmentRepository investmentRepository;
    private final FormRegistryService formRegistryService;
    private final ObjectMapper objectMapper;

    public InvestmentService(
            InvestmentRepository investmentRepository,
            FormRegistryService formRegistryService,
            ObjectMapper objectMapper) {
        this.investmentRepository = investmentRepository;
        this.formRegistryService = formRegistryService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<Investment> getInvestments() {
        return investmentRepository.findAllByOrderByInvestmentDateDescCreatedAtDesc();
    }

    @Transactional
    public Investment createInvestment(CreateInvestmentRequest request) {
        formRegistryService.assertActiveOption("INVESTMENT", "investmentType", request.investmentType());
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Investment investment = new Investment();
        investment.setInvestorName(request.investorName().trim());
        investment.setInvestmentType(request.investmentType());
        investment.setAmount(request.amount());
        investment.setInvestmentDate(request.investmentDate());
        investment.setDetails(request.details() == null || request.details().isBlank() ? null : request.details().trim());
        investment.setCreatedBy(authentication.getName());
        investment = investmentRepository.saveAndFlush(investment);
        JsonNode investmentType = objectMapper.getNodeFactory().textNode(request.investmentType());
        formRegistryService.submit(new FormSubmissionRequest(
                "INVESTMENT", "INVESTMENT", investment.getId(), Map.of("investmentType", investmentType)));
        return investment;
    }

    @Transactional
    public Investment voidInvestment(java.util.UUID id, String reason) {
        Investment investment = investmentRepository.findById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Investment not found"));
        investment.setStatus("VOIDED");
        if (reason != null && !reason.isBlank()) {
            String updatedDetails = (investment.getDetails() == null ? "" : investment.getDetails() + " | ") + "VOIDED: " + reason.trim();
            investment.setDetails(updatedDetails.length() > 1000 ? updatedDetails.substring(0, 1000) : updatedDetails);
        }
        return investmentRepository.save(investment);
    }
}
