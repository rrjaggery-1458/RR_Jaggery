package com.rrjaggery.finance.repository;

import com.rrjaggery.finance.entity.AdvanceType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdvanceTypeRepository extends JpaRepository<AdvanceType, UUID> {
    List<AdvanceType> findByActiveTrueOrderByNameAsc();
    Optional<AdvanceType> findByCodeIgnoreCase(String code);
}
