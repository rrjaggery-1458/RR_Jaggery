package com.rrjaggery.auth.dto;

public class AdminStatsDto {

    private long totalUsers;
    private long adminCount;
    private long managerCount;
    private long retailCustomerCount;
    private long wholesaleCustomerCount;
    private long disabledCount;

    public AdminStatsDto() {}

    public AdminStatsDto(long totalUsers, long adminCount, long managerCount, long retailCustomerCount, long wholesaleCustomerCount, long disabledCount) {
        this.totalUsers = totalUsers;
        this.adminCount = adminCount;
        this.managerCount = managerCount;
        this.retailCustomerCount = retailCustomerCount;
        this.wholesaleCustomerCount = wholesaleCustomerCount;
        this.disabledCount = disabledCount;
    }

    public long getTotalUsers() { return totalUsers; }
    public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

    public long getAdminCount() { return adminCount; }
    public void setAdminCount(long adminCount) { this.adminCount = adminCount; }

    public long getManagerCount() { return managerCount; }
    public void setManagerCount(long managerCount) { this.managerCount = managerCount; }

    public long getRetailCustomerCount() { return retailCustomerCount; }
    public void setRetailCustomerCount(long retailCustomerCount) { this.retailCustomerCount = retailCustomerCount; }

    public long getWholesaleCustomerCount() { return wholesaleCustomerCount; }
    public void setWholesaleCustomerCount(long wholesaleCustomerCount) { this.wholesaleCustomerCount = wholesaleCustomerCount; }

    public long getDisabledCount() { return disabledCount; }
    public void setDisabledCount(long disabledCount) { this.disabledCount = disabledCount; }
}
