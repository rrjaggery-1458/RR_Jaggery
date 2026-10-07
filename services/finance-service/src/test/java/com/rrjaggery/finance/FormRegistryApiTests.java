package com.rrjaggery.finance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rrjaggery.finance.entity.Expense;
import com.rrjaggery.finance.repository.ExpenseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FormRegistryApiTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCanReadOperationalFormsButCannotManageDefinitions() throws Exception {
        mockMvc.perform(get("/api/v1/finance/forms/definitions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.formKey == 'EXPENSE')]").exists())
                .andExpect(jsonPath("$.data[?(@.formKey == 'INVESTMENT')]").doesNotExist());

        mockMvc.perform(post("/api/v1/finance/forms/definitions")
                        .contentType("application/json")
                        .content("""
                                {"formKey":"NOT_ALLOWED","displayName":"Not allowed"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                "/api/v1/finance/forms/fields/00000000-0000-0000-0000-000000000000")
                        .contentType("application/json")
                        .content("""
                                {"label":"Not allowed","fieldType":"TEXT","required":false,"displayOrder":0}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/finance/investments"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/finance/business-masters/expense-categories")
                        .param("activeOnly", "true"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/finance/business-masters/payment-methods")
                        .contentType("application/json")
                        .content("""
                                {"code":"TEST_METHOD","displayName":"Test Method"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void customerCannotAccessInternalFormDefinitions() throws Exception {
        mockMvc.perform(get("/api/v1/finance/forms/definitions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanConfigureVersionAndSubmitValidatedSupplementalData() throws Exception {
        mockMvc.perform(post("/api/v1/finance/forms/definitions")
                        .contentType("application/json")
                        .content("""
                                {"formKey":"CUSTOM_RUN","displayName":"Custom Run","description":"Test form"}
                                """))
                .andExpect(status().isCreated());

        MvcResult fieldResult = mockMvc.perform(post("/api/v1/finance/forms/definitions/CUSTOM_RUN/fields")
                        .contentType("application/json")
                        .content("""
                                {"fieldKey":"purpose","label":"Purpose","fieldType":"TEXT","required":true,"displayOrder":0}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode field = objectMapper.readTree(fieldResult.getResponse().getContentAsString()).path("data");
        String fieldId = field.path("id").asText();

        MvcResult secondFieldResult = mockMvc.perform(post("/api/v1/finance/forms/definitions/CUSTOM_RUN/fields")
                        .contentType("application/json")
                        .content("""
                                {"fieldKey":"context","label":"Context","fieldType":"TEXT","required":false,"displayOrder":1}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String secondFieldId = objectMapper.readTree(secondFieldResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        mockMvc.perform(patch("/api/v1/finance/forms/definitions/CUSTOM_RUN/fields/reorder")
                        .contentType("application/json")
                        .content("""
                                ["%s","%s"]
                                """.formatted(secondFieldId, fieldId)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/finance/forms/definitions/CUSTOM_RUN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fields[0].fieldKey").value("context"));

        mockMvc.perform(post("/api/v1/finance/forms/submissions")
                        .contentType("application/json")
                        .content("""
                                {"formKey":"CUSTOM_RUN","recordType":"CUSTOM_RUN","answers":{}}
                                """))
                .andExpect(status().isBadRequest());

        MvcResult submissionResult = mockMvc.perform(post("/api/v1/finance/forms/submissions")
                        .contentType("application/json")
                        .content("""
                                {"formKey":"CUSTOM_RUN","recordType":"CUSTOM_RUN","answers":{"purpose":"batch note"}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.formVersion").value(4))
                .andExpect(jsonPath("$.data.answers.purpose").value("batch note"))
                .andReturn();
        JsonNode submission = objectMapper.readTree(submissionResult.getResponse().getContentAsString()).path("data");
        String recordId = submission.path("recordId").asText();
        assertNotNull(recordId);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                "/api/v1/finance/forms/fields/" + fieldId)
                        .contentType("application/json")
                        .content("""
                                {"label":"Operational purpose","fieldType":"TEXT","required":true,"displayOrder":0}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/finance/forms/fields/" + fieldId + "/status")
                        .contentType("application/json")
                        .content("""
                                {"active":false}
                                """))
                .andExpect(status().isOk());

        MvcResult historyResult = mockMvc.perform(get("/api/v1/finance/forms/submissions")
                        .param("recordType", "CUSTOM_RUN")
                        .param("recordId", recordId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].revisionSnapshot.fields[1].label").value("Purpose"))
                .andExpect(jsonPath("$.data[0].revisionSnapshot.fields[1].active").value(true))
                .andReturn();
        JsonNode history = objectMapper.readTree(historyResult.getResponse().getContentAsString()).path("data").get(0);
        assertEquals(4, history.path("formVersion").asInt());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void selectValuesMustMatchAnActiveConfiguredOption() throws Exception {
        mockMvc.perform(post("/api/v1/finance/forms/definitions")
                        .contentType("application/json")
                        .content("""
                                {"formKey":"SELECT_RUN","displayName":"Select Run"}
                                """))
                .andExpect(status().isCreated());

        MvcResult fieldResult = mockMvc.perform(post("/api/v1/finance/forms/definitions/SELECT_RUN/fields")
                        .contentType("application/json")
                        .content("""
                                {"fieldKey":"choice","label":"Choice","fieldType":"SINGLE_SELECT","required":true,"displayOrder":0}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String fieldId = objectMapper.readTree(fieldResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        MvcResult optionResult = mockMvc.perform(post("/api/v1/finance/forms/fields/" + fieldId + "/options")
                        .contentType("application/json")
                        .content("""
                                {"value":"A","label":"Option A","displayOrder":0}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String optionId = objectMapper.readTree(optionResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                        "/api/v1/finance/forms/field-options/" + optionId)
                        .contentType("application/json")
                        .content("""
                                {"value":"A","label":"Revised option A","displayOrder":0}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/finance/forms/submissions")
                        .contentType("application/json")
                        .content("""
                                {"formKey":"SELECT_RUN","recordType":"SELECT_RUN","answers":{"choice":"NOT_CONFIGURED"}}
                                """))
                .andExpect(status().isBadRequest());

        MvcResult submissionResult = mockMvc.perform(post("/api/v1/finance/forms/submissions")
                        .contentType("application/json")
                        .content("""
                                {"formKey":"SELECT_RUN","recordType":"SELECT_RUN","answers":{"choice":"A"}}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String recordId = objectMapper.readTree(submissionResult.getResponse().getContentAsString())
                .path("data").path("recordId").asText();

        mockMvc.perform(patch("/api/v1/finance/forms/field-options/" + optionId + "/status")
                        .contentType("application/json")
                        .content("""
                                {"active":false}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/finance/forms/submissions")
                        .param("recordType", "SELECT_RUN")
                        .param("recordId", recordId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].revisionSnapshot.fields[0].options[0].label")
                        .value("Revised option A"))
                .andExpect(jsonPath("$.data[0].revisionSnapshot.fields[0].options[0].active").value(true));

        mockMvc.perform(post("/api/v1/finance/forms/submissions")
                        .contentType("application/json")
                        .content("""
                                {"formKey":"SELECT_RUN","recordType":"SELECT_RUN","answers":{"choice":"A"}}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void investmentUsesAdminConfiguredTypeAndIsSeparateFromOperatingExpenses() throws Exception {
        MvcResult definitionResult = mockMvc.perform(get("/api/v1/finance/forms/definitions/INVESTMENT"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode fields = objectMapper.readTree(definitionResult.getResponse().getContentAsString())
                .path("data").path("fields");
        String fieldId = fields.get(0).path("id").asText();

        mockMvc.perform(post("/api/v1/finance/forms/fields/" + fieldId + "/options")
                        .contentType("application/json")
                        .content("""
                                {"value":"OWNER_CAPITAL","label":"Owner capital","displayOrder":0}
                                """))
                .andExpect(status().isCreated());

        MvcResult investmentResult = mockMvc.perform(post("/api/v1/finance/investments")
                        .contentType("application/json")
                        .content("""
                                {"investorName":"Owner","investmentType":"OWNER_CAPITAL","amount":25000.00,"investmentDate":"2026-09-30"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.amount").value(25000.00))
                .andReturn();
        String investmentId = objectMapper.readTree(investmentResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        mockMvc.perform(get("/api/v1/finance/forms/submissions")
                        .param("recordType", "INVESTMENT")
                        .param("recordId", investmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].answers.investmentType").value("OWNER_CAPITAL"))
                .andExpect(jsonPath("$.data[0].revisionSnapshot.fields[0].options[0].label")
                        .value("Owner capital"));

        mockMvc.perform(get("/api/v1/finance/expenses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCanSubmitSupplementalAnswersOnlyForAnExistingExpense() throws Exception {
        Expense expense = new Expense();
        expense.setExpenseDate(LocalDate.of(2026, 9, 30));
        expense.setCategory("Test category");
        expense.setAmount(new BigDecimal("10.00"));
        expense.setPaymentMethod("CASH");
        expense.setCreatedBy("manager");
        expense = expenseRepository.saveAndFlush(expense);

        mockMvc.perform(post("/api/v1/finance/forms/submissions")
                        .contentType("application/json")
                        .content("""
                                {"formKey":"EXPENSE","recordType":"EXPENSE","recordId":"%s","answers":{}}
                                """.formatted(expense.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.formKey").value("EXPENSE"))
                .andExpect(jsonPath("$.data.recordId").value(expense.getId().toString()));

        mockMvc.perform(get("/api/v1/finance/forms/submissions")
                        .param("recordType", "EXPENSE")
                        .param("recordId", expense.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].formVersion").value(1));
    }
}
