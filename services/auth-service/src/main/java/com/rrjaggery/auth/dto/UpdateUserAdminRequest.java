package com.rrjaggery.auth.dto;

import com.rrjaggery.auth.entity.Role;
import java.util.Set;

public class UpdateUserAdminRequest {

    private String fullName;
    private String phone;
    private String businessName;
    private String gstin;
    private Set<Role> roles;
    private Boolean enabled;

    public UpdateUserAdminRequest() {}

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
