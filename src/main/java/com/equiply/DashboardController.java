package com.equiply;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final RentalBookingRepository bookingRepository;
    private final RentalAccessService rentalAccessService;

    public DashboardController(EquipmentRepository equipmentRepository, UserRepository userRepository,
                               RentalBookingRepository bookingRepository, RentalAccessService rentalAccessService) {
        this.equipmentRepository = equipmentRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
        this.rentalAccessService = rentalAccessService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication, Model model) {
        User currentUser = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (currentUser == null || currentUser.getCollegeName() == null || currentUser.getCollegeName().isBlank()
                || currentUser.getLatitude() == null || currentUser.getLongitude() == null) {
            return "redirect:/profile/location";
        }

        // Hide legacy sample rows without owners and only show listings from the
        // same college or a pickup area within 10 km.
        var equipment = equipmentRepository.findAllByArchivedFalse().stream()
        .filter(item -> item.getOwner() != null)
        .filter(item -> rentalAccessService.canAccess(currentUser, item.getOwner()))
        .toList();

        long total = equipment.size();
        long available = equipment.stream().filter(e -> e.getStatus() == EquipmentStatus.AVAILABLE).count();
        long borrowed = equipment.stream().filter(e -> e.getStatus() == EquipmentStatus.BORROWED).count();
        long overdue = equipment.stream()
                .filter(e -> e.getStatus() == EquipmentStatus.BORROWED && e.getDueDate() != null && e.getDueDate().isBefore(java.time.LocalDateTime.now()))
                .count();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("equipment", equipment);
        model.addAttribute("total", total);
        model.addAttribute("available", available);
        model.addAttribute("borrowed", borrowed);
        model.addAttribute("overdue", overdue);
        model.addAttribute("myBookings", currentUser == null ? java.util.List.of()
                : bookingRepository.findByRenter_IdOrderByRequestedAtDesc(currentUser.getId()));
        model.addAttribute("pendingOwnerBookings", currentUser == null ? java.util.List.of()
                : bookingRepository.findByEquipment_Owner_IdAndStatusOrderByRequestedAtDesc(
                        currentUser.getId(), RentalBooking.PENDING));

        return "dashboard";
    }
}
