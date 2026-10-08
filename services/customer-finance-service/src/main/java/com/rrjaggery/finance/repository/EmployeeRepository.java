package com.rrjaggery.finance.repository;

import com.rrjaggery.finance.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    List<Employee> findByActiveTrueOrderByFullNameAsc();
    Optional<Employee> findByEmployeeCodeIgnoreCase(String employeeCode);
    boolean existsByEmployeeCodeIgnoreCase(String employeeCode);
}
