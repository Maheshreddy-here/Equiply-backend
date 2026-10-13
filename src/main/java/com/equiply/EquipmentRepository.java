
package com.equiply;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    // All equipment, including archived items.
    List<Equipment> findAllByOrderByIdDesc();

    List<Equipment> findAllByArchivedFalseOrderByIdDesc();

    // Only equipment that has not been archived.
    List<Equipment> findAllByArchivedFalse();

    // Lock an item while changing its details or processing a rental.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Equipment e where e.id = :id")
    Optional<Equipment> findByIdForUpdate(@Param("id") Long id);

    // Find an item only if it is not archived.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Equipment e where e.id = :id and e.archived = false")
    Optional<Equipment> findActiveByIdForUpdate(@Param("id") Long id);
}
