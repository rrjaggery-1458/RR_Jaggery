package com.rrjaggery.procurement.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "suppliers", schema = "procurement_schema",
        uniqueConstraints = @UniqueConstraint(name = "uk_supplier_code", columnNames = "supplier_code"),
        indexes = {
                @Index(name = "idx_supplier_name", columnList = "supplier_name"),
                @Index(name = "idx_supplier_status", columnList = "status")
        })
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "supplier_code", nullable = false, length = 60)
    private String supplierCode;

    @Column(name = "supplier_name", nullable = false, length = 180)
    private String supplierName;

    @Column(name = "contact_name", length = 120)
    private String contactName;

    @Column(name = "email", length = 120)
    private String email;

    @Column(name = "phone", length = 40)
    private String phone;

    @Column(name = "gstin", length = 60)
    private String gstin;

    @Column(name = "payment_terms_days")
    private Integer paymentTermsDays = 30;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SupplierStatus status = SupplierStatus.ACTIVE;

    @Column(name = "current_outstanding", nullable = false, precision = 15, scale = 2)
    private java.math.BigDecimal currentOutstanding = java.math.BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Supplier() {
    }

    public Supplier(String supplierCode, String supplierName, String contactName, String email,
                   String phone, String gstin, Integer paymentTermsDays) {
        this.supplierCode = supplierCode;
        this.supplierName = supplierName;
        this.contactName = contactName;
        this.email = email;
        this.phone = phone;
        this.gstin = gstin;
        this.paymentTermsDays = paymentTermsDays == null ? 30 : paymentTermsDays;
        this.status = SupplierStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSupplierCode() {
        return supplierCode;
    }

    public void setSupplierCode(String supplierCode) {
        this.supplierCode = supplierCode;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getGstin() {
        return gstin;
    }

    public void setGstin(String gstin) {
        this.gstin = gstin;
    }

    public Integer getPaymentTermsDays() {
        return paymentTermsDays;
    }

    public void setPaymentTermsDays(Integer paymentTermsDays) {
        this.paymentTermsDays = paymentTermsDays;
    }

    public SupplierStatus getStatus() {
        return status;
    }

    public void setStatus(SupplierStatus status) {
        this.status = status;
    }

    public java.math.BigDecimal getCurrentOutstanding() {
        return currentOutstanding == null ? java.math.BigDecimal.ZERO : currentOutstanding;
    }

    public void setCurrentOutstanding(java.math.BigDecimal currentOutstanding) {
        this.currentOutstanding = currentOutstanding == null ? java.math.BigDecimal.ZERO : currentOutstanding;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
