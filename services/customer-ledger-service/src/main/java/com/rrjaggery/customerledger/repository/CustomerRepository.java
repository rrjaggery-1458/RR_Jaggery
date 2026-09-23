package com.rrjaggery.customerledger.repository;

import com.rrjaggery.customerledger.entity.Customer;
import com.rrjaggery.customerledger.entity.CustomerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    List<Customer> findByCustomerTypeOrderByCreatedAtDesc(CustomerType customerType);

    List<Customer> findAllByOrderByCreatedAtDesc();

    Optional<Customer> findByAuthUserId(UUID authUserId);

    Optional<Customer> findByPhone(String phone);

    Optional<Customer> findByEmail(String email);

    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.contactPerson) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "(c.businessName IS NOT NULL AND LOWER(c.businessName) LIKE LOWER(CONCAT('%', :q, '%'))) OR " +
           "c.phone LIKE CONCAT('%', :q, '%') OR " +
           "(c.gstin IS NOT NULL AND LOWER(c.gstin) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Customer> searchCustomers(@Param("q") String query);
}
