package com.equiply;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BorrowLogRepository extends JpaRepository<BorrowLog, Long> {
    Optional<BorrowLog> findFirstByEquipmentAndReturnedAtIsNullOrderByBorrowedAtDesc(Equipment equipment);
}
