package com.rrjaggery.finance.repository;

import com.rrjaggery.finance.entity.DeductionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeductionTypeRepository extends JpaRepository<DeductionType, UUID> {
    List<DeductionType> findByActiveTrueOrderByNameAsc();
    Optional<DeductionType> findByCodeIgnoreCase(String code);
}
