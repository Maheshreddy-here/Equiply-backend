package com.equiply;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface RentalBookingRepository extends JpaRepository<RentalBooking, Long> {
    List<RentalBooking> findByRenter_IdOrderByRequestedAtDesc(Long renterId);
    List<RentalBooking> findAllByOrderByRequestedAtDesc();
    List<RentalBooking> findByEquipment_Owner_IdAndStatusOrderByRequestedAtDesc(Long ownerId, String status);

    @Query("select count(b) from RentalBooking b where b.equipment.id = :equipmentId " +
           "and b.status = 'APPROVED' and b.startDate <= :endDate and b.endDate >= :startDate")
    long countOverlappingApprovedBookings(@Param("equipmentId") Long equipmentId,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);
}
