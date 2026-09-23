package com.rrjaggery.customerledger.dto;

import com.rrjaggery.customerledger.entity.PaymentTerms;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public class UpdateCustomerRequest {

    private String businessName;

    @NotBlank(message = "Contact person name is required")
    private String contactPerson;

    @NotBlank(message = "Phone number is required")
    private String phone;

    private String email;
    private String gstin;
    private AddressDto billingAddress;
    private AddressDto shippingAddress;
    private PaymentTerms paymentTerms;

    @DecimalMin(value = "0.00", message = "Credit limit cannot be negative")
    private BigDecimal creditLimit;

    @Min(value = 0, message = "Credit days cannot be negative")
    private Integer creditDays;

    private Boolean active;

    public UpdateCustomerRequest() {}

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

    public Integer getCreditDays() { return creditDays; }
    public void setCreditDays(Integer creditDays) { this.creditDays = creditDays; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
