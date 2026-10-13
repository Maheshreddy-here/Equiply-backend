package com.equiply;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
public class RentalBookingController {
    private final RentalBookingRepository bookingRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final RentalAccessService rentalAccessService;

    public RentalBookingController(RentalBookingRepository bookingRepository,
                                   EquipmentRepository equipmentRepository,
                                   UserRepository userRepository, RentalAccessService rentalAccessService) {
        this.bookingRepository = bookingRepository;
        this.equipmentRepository = equipmentRepository;
        this.userRepository = userRepository;
        this.rentalAccessService = rentalAccessService;
    }

    @PostMapping("/equipment/{id}/book")
    @Transactional
    public String requestBooking(@PathVariable Long id,
                                 @RequestParam LocalDate startDate,
                                 @RequestParam LocalDate endDate,
                                 Authentication authentication,
                                 RedirectAttributes flash) {
        try {
            User renter = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Your account could not be found."));
            Equipment equipment = equipmentRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Equipment not found."));

            if (equipment.getOwner() == null) {
                flash.addFlashAttribute("error", "This item has no owner assigned yet and cannot be booked.");
                return "redirect:/dashboard";
            }
            if (renter.getCollegeName() == null || renter.getLatitude() == null || renter.getLongitude() == null) {
                flash.addFlashAttribute("error", "Complete your college and pickup area in your profile before requesting rentals.");
                return "redirect:/profile/location";
            }
            if (!rentalAccessService.canAccess(renter, equipment.getOwner())) {
                flash.addFlashAttribute("error", "This listing is outside your local community. Nearby-college rentals must be within 10 km of the pickup area.");
                return "redirect:/dashboard";
            }
            if (equipment.getOwner().getId().equals(renter.getId())) {
                flash.addFlashAttribute("error", "You cannot request a rental for your own item.");
                return "redirect:/dashboard";
            }
            if (startDate == null || endDate == null || startDate.isBefore(LocalDate.now()) || endDate.isBefore(startDate)) {
                flash.addFlashAttribute("error", "Choose valid dates. The start date cannot be in the past, and the end date must be on or after the start date.");
                return "redirect:/dashboard";
            }
            if (equipment.getStatus() != EquipmentStatus.AVAILABLE) {
                flash.addFlashAttribute("error", "This item is currently unavailable.");
                return "redirect:/dashboard";
            }

            RentalBooking booking = new RentalBooking();
            booking.setEquipment(equipment);
            booking.setRenter(renter);
            booking.setStartDate(startDate);
            booking.setEndDate(endDate);
            booking.setStatus(RentalBooking.PENDING);
            bookingRepository.save(booking);
            flash.addFlashAttribute("success", "Rental request sent to the owner. You can track its status below.");
        } catch (Exception ex) {
            flash.addFlashAttribute("error", "Could not send rental request: " + ex.getMessage());
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/bookings/{id}/approve")
    @Transactional
    public String approve(@PathVariable Long id, Authentication authentication, RedirectAttributes flash) {
        try {
            User owner = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Your account could not be found."));
            RentalBooking booking = bookingRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Booking request not found."));
            Equipment lockedEquipment = equipmentRepository.findByIdForUpdate(booking.getEquipment().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Equipment not found."));

            if (lockedEquipment.getOwner() == null || !lockedEquipment.getOwner().getId().equals(owner.getId())) {
                flash.addFlashAttribute("error", "Only this item's owner can approve the request.");
                return "redirect:/dashboard";
            }
            if (!RentalBooking.PENDING.equals(booking.getStatus())) {
                flash.addFlashAttribute("error", "This request has already been processed.");
                return "redirect:/dashboard";
            }
            if (!rentalAccessService.canAccess(booking.getRenter(), owner)) {
                flash.addFlashAttribute("error", "The renter is no longer eligible for this local pickup area.");
                return "redirect:/dashboard";
            }
            if (lockedEquipment.getStatus() != EquipmentStatus.AVAILABLE ||
                    bookingRepository.countOverlappingApprovedBookings(lockedEquipment.getId(), booking.getStartDate(), booking.getEndDate()) > 0) {
                flash.addFlashAttribute("error", "These dates conflict with an existing booking or the item is unavailable.");
                return "redirect:/dashboard";
            }

            booking.setStatus(RentalBooking.APPROVED);
            bookingRepository.save(booking);
            flash.addFlashAttribute("success", "Booking approved. The renter can now see the approval on their dashboard.");
        } catch (Exception ex) {
            flash.addFlashAttribute("error", "Could not approve booking: " + ex.getMessage());
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/bookings/{id}/reject")
    @Transactional
    public String reject(@PathVariable Long id, Authentication authentication, RedirectAttributes flash) {
        try {
            User owner = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Your account could not be found."));
            RentalBooking booking = bookingRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Booking request not found."));
            if (booking.getEquipment().getOwner() == null || !booking.getEquipment().getOwner().getId().equals(owner.getId())) {
                flash.addFlashAttribute("error", "Only this item's owner can reject the request.");
                return "redirect:/dashboard";
            }
            if (!RentalBooking.PENDING.equals(booking.getStatus())) {
                flash.addFlashAttribute("error", "This request has already been processed.");
                return "redirect:/dashboard";
            }
            booking.setStatus(RentalBooking.REJECTED);
            bookingRepository.save(booking);
            flash.addFlashAttribute("success", "Booking request rejected.");
        } catch (Exception ex) {
            flash.addFlashAttribute("error", "Could not reject booking: " + ex.getMessage());
        }
        return "redirect:/dashboard";
    }
}
