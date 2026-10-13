package com.equiply;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminController {

    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final RentalBookingRepository bookingRepository;

    public AdminController(
            EquipmentRepository equipmentRepository,
            UserRepository userRepository,
            RentalBookingRepository bookingRepository) {
        this.equipmentRepository = equipmentRepository;
        this.userRepository = userRepository;
        this.bookingRepository = bookingRepository;
    }

    @GetMapping("/admin")
    public String adminDashboard(Model model) {

        List<User> users = userRepository.findAllByOrderByIdDesc();
        List<Equipment> equipment =
                equipmentRepository.findAllByOrderByIdDesc();
        List<RentalBooking> bookings =
                bookingRepository.findAllByOrderByRequestedAtDesc();

        long studentCount = users.stream()
                .filter(u -> !"ADMIN".equalsIgnoreCase(u.getRole()))
                .count();

        long verifiedCount = users.stream()
                .filter(u -> !"ADMIN".equalsIgnoreCase(u.getRole()))
                .filter(User::isEmailVerified)
                .count();

        long itemCount = equipment.stream()
                .filter(item -> !item.isArchived())
                .count();

        model.addAttribute("users", users);
        model.addAttribute("equipment", equipment);
        model.addAttribute("bookings", bookings);
        model.addAttribute(
                "colleges",
                userRepository.findDistinctCollegeNames()
        );

        model.addAttribute("studentCount", studentCount);
        model.addAttribute("verifiedCount", verifiedCount);
        model.addAttribute("itemCount", itemCount);
        model.addAttribute("bookingCount", bookings.size());

        return "admin";
    }

    @PostMapping("/admin/equipment/{id}/remove")
    public String removeEquipment(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        Equipment item = equipmentRepository.findById(id).orElse(null);

        if (item == null) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Equipment not found."
            );
        } else if (item.isArchived()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "This equipment has already been removed."
            );
        } else {
            item.setArchived(true);
            equipmentRepository.save(item);

            redirectAttributes.addFlashAttribute(
                    "adminMessage",
                    "Equipment removed successfully."
            );
        }

        return "redirect:/admin";
    }

    @PostMapping("/admin/students/{id}/toggle")
    public String toggleStudentAccount(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes) {

        User user = userRepository.findById(id).orElse(null);

        if (user == null) {
            redirectAttributes.addFlashAttribute(
                    "adminMessage",
                    "Student account not found."
            );
        } else if ("ADMIN".equalsIgnoreCase(user.getRole())) {
            redirectAttributes.addFlashAttribute(
                    "adminMessage",
                    "Administrator accounts cannot be changed here."
            );
        } else {
            user.setAccountActive(!user.isAccountActive());
            userRepository.save(user);

            redirectAttributes.addFlashAttribute(
                    "adminMessage",
                    user.isAccountActive()
                            ? "Student account reactivated successfully."
                            : "Student account suspended successfully."
            );
        }

        return "redirect:/admin";
    }
}