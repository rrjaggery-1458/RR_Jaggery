package com.rrjaggery.finance.dto;

import com.rrjaggery.finance.entity.AttendanceStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

public class RecordAttendanceRequest {

    @NotNull(message = "Employee ID is required")
    private UUID employeeId;

    @NotNull(message = "Attendance date is required")
    private LocalDate attendanceDate = LocalDate.now(ZoneId.of("Asia/Kolkata"));

    @NotNull(message = "Status is required")
    private AttendanceStatus status = AttendanceStatus.PRESENT;

    @DecimalMin(value = "0.00", message = "Overtime hours cannot be negative")
    private BigDecimal overtimeHours = BigDecimal.ZERO;

    private String notes;

    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public void setWorkerId(UUID workerId) { if (this.employeeId == null) this.employeeId = workerId; }

    public LocalDate getAttendanceDate() { return attendanceDate; }
    public void setAttendanceDate(LocalDate attendanceDate) { this.attendanceDate = attendanceDate; }
    public void setDate(LocalDate date) { if (this.attendanceDate == null) this.attendanceDate = date; }

    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus status) { this.status = status; }

    public BigDecimal getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(BigDecimal overtimeHours) { this.overtimeHours = overtimeHours; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
