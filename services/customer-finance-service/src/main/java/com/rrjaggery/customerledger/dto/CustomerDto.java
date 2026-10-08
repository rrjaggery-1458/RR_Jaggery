package com.rrjaggery.customerledger.dto;

import com.rrjaggery.customerledger.entity.Customer;
import com.rrjaggery.customerledger.entity.CustomerType;
import com.rrjaggery.customerledger.entity.PaymentTerms;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class CustomerDto {

    private UUID id;
    private CustomerType customerType;
    private String businessName;
    private String contactPerson;
    private String phone;
    private String email;
    private String gstin;
    private AddressDto billingAddress;
    private AddressDto shippingAddress;
    private PaymentTerms paymentTerms;
    private BigDecimal creditLimit;
    private int creditDays;
    private BigDecimal currentOutstanding;
    private BigDecimal overdueAmount;
    private boolean active;
    private UUID authUserId;
    private Instant createdAt;
    private Instant updatedAt;

    public CustomerDto() {}

    public static CustomerDto fromEntity(Customer c, BigDecimal overdueAmount) {
        CustomerDto dto = new CustomerDto();
        dto.setId(c.getId());
        dto.setCustomerType(c.getCustomerType());
        dto.setBusinessName(c.getBusinessName());
        dto.setContactPerson(c.getContactPerson());
        dto.setPhone(c.getPhone());
        dto.setEmail(c.getEmail());
        dto.setGstin(c.getGstin());
        dto.setBillingAddress(new AddressDto(c.getBillingStreet(), c.getBillingCity(), c.getBillingState(), c.getBillingPostalCode()));
        dto.setShippingAddress(new AddressDto(c.getShippingStreet(), c.getShippingCity(), c.getShippingState(), c.getShippingPostalCode()));
        dto.setPaymentTerms(c.getPaymentTerms());
        dto.setCreditLimit(c.getCreditLimit());
        dto.setCreditDays(c.getCreditDays());
        dto.setCurrentOutstanding(c.getCurrentOutstanding());
        dto.setOverdueAmount(overdueAmount != null ? overdueAmount : BigDecimal.ZERO);
        dto.setActive(c.isActive());
        dto.setAuthUserId(c.getAuthUserId());
        dto.setCreatedAt(c.getCreatedAt());
        dto.setUpdatedAt(c.getUpdatedAt());
        return dto;
    }

    public static CustomerDto fromEntity(Customer c) {
        return fromEntity(c, BigDecimal.ZERO);
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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

    public BigDecimal getCurrentOutstanding() { return currentOutstanding; }
    public void setCurrentOutstanding(BigDecimal currentOutstanding) { this.currentOutstanding = currentOutstanding; }

    public BigDecimal getOverdueAmount() { return overdueAmount; }
    public void setOverdueAmount(BigDecimal overdueAmount) { this.overdueAmount = overdueAmount; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public UUID getAuthUserId() { return authUserId; }
    public void setAuthUserId(UUID authUserId) { this.authUserId = authUserId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
