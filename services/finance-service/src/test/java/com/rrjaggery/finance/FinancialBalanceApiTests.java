package com.rrjaggery.finance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rrjaggery.finance.dto.CreateExpenseRequest;
import com.rrjaggery.finance.dto.CreateInvestmentRequest;
import com.rrjaggery.finance.entity.Expense;
import com.rrjaggery.finance.entity.Investment;
import com.rrjaggery.finance.repository.ExpenseRepository;
import com.rrjaggery.finance.repository.InvestmentRepository;
import com.rrjaggery.finance.service.FinanceOrderServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FinancialBalanceApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InvestmentRepository investmentRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @MockBean
    private FinanceOrderServiceClient orderServiceClient;

    @BeforeEach
    void setup() {
        expenseRepository.deleteAll();
        investmentRepository.deleteAll();
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    @DisplayName("Section Y Test Scenario: Full running balance calculation across initial investment, expense, wholesale, web retail, manual retail, and additional investment")
    void testSectionYFinancialBalanceScenario() throws Exception {
        // 1. Initial Investment: ₹2,50,000
        Investment inv1 = new Investment();
        inv1.setInvestorName("Founder");
        inv1.setInvestmentType("OWNER_CAPITAL");
        inv1.setAmount(new BigDecimal("250000.00"));
        inv1.setInvestmentDate(LocalDate.of(2026, 9, 1));
        inv1.setStatus("ACTIVE");
        inv1.setCreatedBy("admin");
        investmentRepository.save(inv1);

        // 2. Expense: ₹50,000
        Expense exp1 = new Expense();
        exp1.setCategory("Fuel / Diesel");
        exp1.setAmount(new BigDecimal("50000.00"));
        exp1.setExpenseDate(LocalDate.of(2026, 9, 5));
        exp1.setPaymentMethod("CASH");
        exp1.setDescription("Diesel for crushing");
        exp1.setCreatedBy("admin");
        expenseRepository.save(exp1);

        // Mock Remote Offline Orders (Wholesale ₹52,000 + Manual Retail ₹10,500)
        FinanceOrderServiceClient.RemoteOfflineOrderDto wholesaleOrder = new FinanceOrderServiceClient.RemoteOfflineOrderDto();
        wholesaleOrder.id = UUID.randomUUID();
        wholesaleOrder.orderNumber = "OFF-20260910-001";
        wholesaleOrder.customerName = "Wholesaler A";
        wholesaleOrder.orderType = "WHOLESALE";
        wholesaleOrder.subtotalAmount = new BigDecimal("50000.00"); // 500 KG * ₹100
        wholesaleOrder.deliveryCharges = new BigDecimal("2000.00");
        wholesaleOrder.totalAmount = new BigDecimal("52000.00");
        wholesaleOrder.orderDate = Instant.parse("2026-09-10T10:00:00Z");
        wholesaleOrder.status = "CONFIRMED";

        FinanceOrderServiceClient.RemoteOfflineOrderDto manualRetailOrder = new FinanceOrderServiceClient.RemoteOfflineOrderDto();
        manualRetailOrder.id = UUID.randomUUID();
        manualRetailOrder.orderNumber = "RET-20260915-001";
        manualRetailOrder.customerName = "Walk-in Retail B";
        manualRetailOrder.orderType = "MANUAL_RETAIL";
        manualRetailOrder.subtotalAmount = new BigDecimal("10000.00"); // 100 KG * ₹100
        manualRetailOrder.deliveryCharges = new BigDecimal("500.00");
        manualRetailOrder.totalAmount = new BigDecimal("10500.00");
        manualRetailOrder.orderDate = Instant.parse("2026-09-15T10:00:00Z");
        manualRetailOrder.status = "CONFIRMED";

        given(orderServiceClient.getOfflineOrders()).willReturn(List.of(wholesaleOrder, manualRetailOrder));

        // Mock Remote Commerce Orders (Website Retail ₹30,000)
        FinanceOrderServiceClient.RemoteCommerceOrderDto webOrder = new FinanceOrderServiceClient.RemoteCommerceOrderDto();
        webOrder.id = UUID.randomUUID();
        webOrder.orderNumber = "WEB-20260912-001";
        webOrder.customerName = "Online Shopper C";
        webOrder.totalAmount = new BigDecimal("30000.00");
        webOrder.createdAt = Instant.parse("2026-09-12T10:00:00Z");
        webOrder.status = "DELIVERED";
        webOrder.paymentStatus = "PAID";

        given(orderServiceClient.getCommerceOrders()).willReturn(List.of(webOrder));

        // 3. Additional Investment: ₹25,000
        Investment inv2 = new Investment();
        inv2.setInvestorName("Angel Partner");
        inv2.setInvestmentType("PARTNER_EQUITY");
        inv2.setAmount(new BigDecimal("25000.00"));
        inv2.setInvestmentDate(LocalDate.of(2026, 9, 20));
        inv2.setStatus("ACTIVE");
        inv2.setCreatedBy("admin");
        investmentRepository.save(inv2);

        // Mock Stock (Finished Goods 500 KG @ ₹100 = ₹50,000)
        FinanceOrderServiceClient.RemoteInventoryItemDto stockItem = new FinanceOrderServiceClient.RemoteInventoryItemDto();
        stockItem.id = UUID.randomUUID();
        stockItem.sku = "RR-JAG-500KG";
        stockItem.itemName = "Organic Jaggery Powder";
        stockItem.itemType = "FINISHED_GOOD";
        stockItem.currentQuantity = new BigDecimal("500.000");
        stockItem.unitOfMeasure = "KG";

        FinanceOrderServiceClient.RemoteProductDto prod = new FinanceOrderServiceClient.RemoteProductDto();
        prod.sku = "RR-JAG-500KG";
        prod.name = "Organic Jaggery Powder";
        prod.retailPrice = new BigDecimal("100.00");

        given(orderServiceClient.getInventoryItems()).willReturn(List.of(stockItem));
        given(orderServiceClient.getCommerceProducts()).willReturn(List.of(prod));

        // Perform GET /api/v1/finance/balance
        mockMvc.perform(get("/api/v1/finance/balance?year=2026&month=9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.initialInvestment").value(250000.00))
                .andExpect(jsonPath("$.data.additionalInvestments").value(25000.00))
                .andExpect(jsonPath("$.data.totalInvestments").value(275000.00))
                .andExpect(jsonPath("$.data.totalExpenses").value(50000.00))
                .andExpect(jsonPath("$.data.wholesaleOrderIncome").value(52000.00))
                .andExpect(jsonPath("$.data.websiteRetailOrderIncome").value(30000.00))
                .andExpect(jsonPath("$.data.manualRetailOrderIncome").value(10500.00))
                .andExpect(jsonPath("$.data.totalOrderIncome").value(92500.00))
                .andExpect(jsonPath("$.data.currentOperationalBalance").value(317500.00))
                .andExpect(jsonPath("$.data.finishedGoodsStockValue").value(50000.00))
                .andExpect(jsonPath("$.data.totalStockValue").value(50000.00))
                .andExpect(jsonPath("$.data.totalBusinessPosition").value(367500.00))
                .andExpect(jsonPath("$.data.recentTransactions", hasSize(6)));
    }

    @Test
    @WithMockUser(username = "customer", roles = {"CUSTOMER"})
    @DisplayName("CUSTOMER role must be forbidden (403) from accessing financial balance")
    void testCustomerDeniedBalanceAccess() throws Exception {
        mockMvc.perform(get("/api/v1/finance/balance"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager", roles = {"MANAGER"})
    @DisplayName("MANAGER role is permitted to view financial balance")
    void testManagerPermittedBalanceAccess() throws Exception {
        mockMvc.perform(get("/api/v1/finance/balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
