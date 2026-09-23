package com.rrjaggery.auth.dto;

import com.rrjaggery.auth.entity.CustomerType;
import com.rrjaggery.auth.entity.Role;
import com.rrjaggery.auth.entity.User;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class UserDto {

    private UUID id;
    private String email;
    private String fullName;
    private String phone;
    private CustomerType customerType;
    private String businessName;
    private String gstin;
    private Set<String> roles;
    private boolean enabled;

    public UserDto() {}

    public static UserDto fromEntity(User user) {
        UserDto dto = new UserDto();
        dto.id = user.getId();
        dto.email = user.getEmail();
        dto.fullName = user.getFullName();
        dto.phone = user.getPhone();
        dto.customerType = user.getCustomerType();
        dto.businessName = user.getBusinessName();
        dto.gstin = user.getGstin();
        dto.roles = user.getRoles().stream().map(Role::name).collect(Collectors.toSet());
        dto.enabled = user.isEnabled();
        return dto;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public CustomerType getCustomerType() { return customerType; }
    public String getBusinessName() { return businessName; }
    public String getGstin() { return gstin; }
    public Set<String> getRoles() { return roles; }
    public boolean isEnabled() { return enabled; }
}
