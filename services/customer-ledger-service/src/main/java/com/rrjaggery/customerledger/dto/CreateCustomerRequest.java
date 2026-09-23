package com.rrjaggery.customerledger.dto;

import com.rrjaggery.customerledger.entity.CustomerType;
import com.rrjaggery.customerledger.entity.PaymentTerms;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public class CreateCustomerRequest {

    @NotNull(message = "Customer type is required")
    private CustomerType customerType = CustomerType.RETAIL;

    private String businessName;

    @NotBlank(message = "Contact person name is required")
    private String contactPerson;

    @NotBlank(message = "Phone number is required")
    private String phone;

    private String email;

    private String gstin;

    private AddressDto billingAddress;

    private AddressDto shippingAddress;

    private PaymentTerms paymentTerms = PaymentTerms.IMMEDIATE_CASH;

    @DecimalMin(value = "0.00", message = "Credit limit cannot be negative")
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Min(value = 0, message = "Credit days cannot be negative")
    private int creditDays = 0;

    private UUID authUserId;

    public CreateCustomerRequest() {}

    public CustomerType getCustomerType() { return customerType; }
    public void setCustomerType(CustomerType customerType) { this.customerType = customerType; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public AddressDto getBillingAddress() { return billingAddress; }
    public void setBillingAddress(AddressDto billingAddress) { this.billingAddress = billingAddress; }

    public AddressDto getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(AddressDto shippingAddress) { this.shippingAddress = shippingAddress; }

    public PaymentTerms getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(PaymentTerms paymentTerms) { this.paymentTerms = paymentTerms; }

    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }

    public int getCreditDays() { return creditDays; }
    public void setCreditDays(int creditDays) { this.creditDays = creditDays; }

    public UUID getAuthUserId() { return authUserId; }
    public void setAuthUserId(UUID authUserId) { this.authUserId = authUserId; }
}
