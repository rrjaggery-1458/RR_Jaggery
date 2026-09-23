package com.rrjaggery.auth.entity;

public enum Role {
    ADMIN,
    MANAGER,
    CUSTOMER,
    @Deprecated
    PRODUCTION_MANAGER,
    @Deprecated
    EMPLOYEE
}
