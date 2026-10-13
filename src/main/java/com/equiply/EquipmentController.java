package com.equiply;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Controller
public class EquipmentController {

    private final EquipmentRepository equipmentRepository;
    private final BorrowLogRepository borrowLogRepository;
    private final UserRepository userRepository;

    public EquipmentController(EquipmentRepository equipmentRepository,
                               BorrowLogRepository borrowLogRepository,
                               UserRepository userRepository) {
        this.equipmentRepository = equipmentRepository;
        this.borrowLogRepository = borrowLogRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/equipment/{id}/borrow")
    @Transactional
    public String borrow(@PathVariable Long id,
                         Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Logged-in user was not found."));

            Equipment equipment = equipmentRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new IllegalArgumentException("Equipment not found."));

            if (equipment.getStatus() != EquipmentStatus.AVAILABLE) {
                redirectAttributes.addFlashAttribute("error", equipment.getName() + " is already borrowed.");
                return "redirect:/dashboard";
            }

            LocalDateTime now = LocalDateTime.now();
            equipment.setStatus(EquipmentStatus.BORROWED);
            equipment.setCurrentHolder(user);
            equipment.setDueDate(now.plusDays(7));
            equipmentRepository.save(equipment);

            BorrowLog log = new BorrowLog();
            log.setEquipment(equipment);
            log.setUser(user);
            log.setBorrowedAt(now);
            borrowLogRepository.save(log);

            redirectAttributes.addFlashAttribute("success", equipment.getName() + " borrowed successfully. Due in 7 days.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Unable to borrow equipment: " + ex.getMessage());
        }

        return "redirect:/dashboard";
    }

    @PostMapping("/equipment/{id}/return")
    @Transactional
    public String returnEquipment(@PathVariable Long id,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        try {
            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Logged-in user was not found."));

            Equipment equipment = equipmentRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new IllegalArgumentException("Equipment not found."));

            if (equipment.getStatus() != EquipmentStatus.BORROWED) {
                redirectAttributes.addFlashAttribute("error", equipment.getName() + " is already available.");
                return "redirect:/dashboard";
            }

            if (equipment.getCurrentHolder() == null || !equipment.getCurrentHolder().getId().equals(user.getId())) {
                redirectAttributes.addFlashAttribute("error", "Only the current borrower can return this equipment.");
                return "redirect:/dashboard";
            }

            BorrowLog log = borrowLogRepository
                    .findFirstByEquipmentAndReturnedAtIsNullOrderByBorrowedAtDesc(equipment)
                    .orElseThrow(() -> new IllegalStateException("Active borrow record not found."));

            log.setReturnedAt(LocalDateTime.now());
            borrowLogRepository.save(log);

            equipment.setStatus(EquipmentStatus.AVAILABLE);
            equipment.setCurrentHolder(null);
            equipment.setDueDate(null);
            equipmentRepository.save(equipment);

            redirectAttributes.addFlashAttribute("success", equipment.getName() + " returned successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Unable to return equipment: " + ex.getMessage());
        }

        return "redirect:/dashboard";
    }
}
