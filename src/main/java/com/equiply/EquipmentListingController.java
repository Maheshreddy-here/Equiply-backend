package com.equiply;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Controller
public class EquipmentListingController {
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;
    private final PricingRecommendationService pricingService;
    private final ProductImageService productImageService;

    public EquipmentListingController(EquipmentRepository equipmentRepository, UserRepository userRepository,
                                      PricingRecommendationService pricingService, ProductImageService productImageService) {
        this.equipmentRepository = equipmentRepository;
        this.userRepository = userRepository;
        this.pricingService = pricingService;
        this.productImageService = productImageService;
    }

    @GetMapping("/equipment/new")
    public String newListing(Authentication authentication) {
        User owner = userRepository.findByEmail(authentication.getName()).orElse(null);
        if (owner == null || owner.getCollegeName() == null || owner.getCollegeName().isBlank()
                || owner.getLatitude() == null || owner.getLongitude() == null) {
            return "redirect:/profile/location";
        }
        return "equipment-form";
    }

    @PostMapping("/equipment/create")
    @Transactional
    public String create(@RequestParam String name, @RequestParam String category,
                         @RequestParam(required = false) String description,
                         @RequestParam(required = false) String imageUrl,
                         @RequestParam(required = false) MultipartFile imageFile,
                         @RequestParam BigDecimal estimatedValue,
                         @RequestParam String conditionGrade,
                         @RequestParam(required = false) BigDecimal customRent,
                         @RequestParam(required = false) BigDecimal customDeposit,
                         Authentication authentication, RedirectAttributes flash) {
        try {
            User owner = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Your user account could not be found."));
            if (owner.getCollegeName() == null || owner.getCollegeName().isBlank()
                    || owner.getLatitude() == null || owner.getLongitude() == null) {
                flash.addFlashAttribute("error", "Complete your college and pickup area before listing equipment.");
                return "redirect:/profile/location";
            }
            PricingRecommendationService.Recommendation rec = pricingService.recommend(estimatedValue, category, conditionGrade);
            Equipment item = new Equipment();
            item.setName(name.trim());
            item.setCategory(category.trim());
            item.setDescription(description == null ? "" : description.trim());
            String selectedImage;
            if (imageFile != null && !imageFile.isEmpty()) {
                selectedImage = storeUploadedImage(imageFile);
            } else if (imageUrl != null && !imageUrl.isBlank()) {
                // Keep compatibility with listings submitted by an older version of the form.
                selectedImage = imageUrl.trim();
            } else {
                selectedImage = productImageService.findImage(name, category);
            }
            item.setImageUrl(selectedImage);
            item.setOwner(owner);
            item.setEstimatedValue(estimatedValue);
            item.setConditionGrade(conditionGrade);
            item.setRecommendedRentPerDay(rec.rentPerDay());
            item.setRecommendedDeposit(rec.deposit());
            item.setRentalPricePerDay(validOr(customRent, rec.rentPerDay()));
            item.setDepositAmount(validOr(customDeposit, rec.deposit()));
            item.setStatus(EquipmentStatus.AVAILABLE);
            equipmentRepository.save(item);
            flash.addFlashAttribute("success", "Listing added. Suggested rent: ₹" + rec.rentPerDay()
                    + "/day and suggested deposit: ₹" + rec.deposit() + ". You can change your prices any time.");
            return "redirect:/dashboard";
        } catch (Exception ex) {
            flash.addFlashAttribute("error", "Could not create listing: " + ex.getMessage());
            return "redirect:/equipment/new";
        }
    }

    @PostMapping("/equipment/{id}/pricing")
    @Transactional
    public String updatePricing(@PathVariable Long id,
                                @RequestParam BigDecimal rentalPricePerDay,
                                @RequestParam BigDecimal depositAmount,
                                Authentication authentication,
                                RedirectAttributes flash) {
        try {
            User user = userRepository.findByEmail(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Your user account could not be found."));
            Equipment item = equipmentRepository.findByIdForUpdate(id)
                    .orElseThrow(() -> new IllegalArgumentException("Listing not found."));
            if (item.getOwner() == null || !item.getOwner().getId().equals(user.getId())) {
                flash.addFlashAttribute("error", "Only the listing owner can change its rent or deposit.");
                return "redirect:/dashboard";
            }
            if (rentalPricePerDay.compareTo(BigDecimal.ZERO) <= 0 || depositAmount.compareTo(BigDecimal.ZERO) < 0) {
                flash.addFlashAttribute("error", "Rent must be greater than zero and deposit cannot be negative.");
                return "redirect:/dashboard";
            }
            item.setRentalPricePerDay(rentalPricePerDay);
            item.setDepositAmount(depositAmount);
            equipmentRepository.save(item);
            flash.addFlashAttribute("success", "Your rent and deposit have been updated.");
        } catch (Exception ex) {
            flash.addFlashAttribute("error", "Could not update pricing: " + ex.getMessage());
        }
        return "redirect:/dashboard";
    }


    private String storeUploadedImage(MultipartFile imageFile) throws IOException {
        if (imageFile.getSize() > 5L * 1024 * 1024) {
            throw new IllegalArgumentException("Image must be 5 MB or smaller.");
        }
        String contentType = imageFile.getContentType() == null ? "" : imageFile.getContentType().toLowerCase();
        String extension = switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new IllegalArgumentException("Upload a JPG, PNG, or WEBP image.");
        };
        Path directory = Path.of("uploads", "equipment").toAbsolutePath().normalize();
        Files.createDirectories(directory);
        String filename = UUID.randomUUID() + extension;
        Path destination = directory.resolve(filename).normalize();
        if (!destination.startsWith(directory)) throw new IllegalArgumentException("Invalid image filename.");
        Files.copy(imageFile.getInputStream(), destination);
        return "/uploads/equipment/" + filename;
    }

    private BigDecimal validOr(BigDecimal supplied, BigDecimal recommended) {
        return supplied == null || supplied.compareTo(BigDecimal.ZERO) <= 0 ? recommended : supplied;
    }
}
