package com.equiply;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {
    private final UserRepository userRepository;

    public ProfileController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        model.addAttribute("currentUser", user);
        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam String fullName,
                                @RequestParam String mobile,
                                @RequestParam(required = false) String address,
                                @RequestParam(required = false) String collegeName,
                                @RequestParam(required = false) Double latitude,
                                @RequestParam(required = false) Double longitude,
                                @RequestParam(defaultValue = "🧑‍🎓") String profileAvatar,
                                Authentication authentication,
                                RedirectAttributes flash) {
        if (fullName == null || fullName.isBlank() || mobile == null || mobile.isBlank()) {
            flash.addFlashAttribute("error", "Full name and mobile number are required.");
            return "redirect:/profile";
        }
        if (latitude != null && (!Double.isFinite(latitude) || latitude < -90 || latitude > 90)
                || longitude != null && (!Double.isFinite(longitude) || longitude < -180 || longitude > 180)) {
            flash.addFlashAttribute("error", "Enter valid latitude and longitude values.");
            return "redirect:/profile";
        }
        if ((latitude == null) != (longitude == null)) {
            flash.addFlashAttribute("error", "Enter both latitude and longitude, or leave both blank.");
            return "redirect:/profile";
        }
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        user.setFullName(fullName.trim());
        user.setMobile(mobile.trim());
        user.setAddress(address == null ? "" : address.trim());
        user.setCollegeName(collegeName == null ? "" : collegeName.trim());
        user.setLatitude(latitude);
        user.setLongitude(longitude);
        java.util.Set<String> allowedAvatars = java.util.Set.of("🧑‍🎓", "👩‍🎓", "👨‍🎓", "🧑🏻‍💻", "👩🏻‍💻", "👨🏻‍💻", "🦊", "🐼", "🐸", "🐯", "🐻", "🐱");
        user.setProfileAvatar(allowedAvatars.contains(profileAvatar) ? profileAvatar : "🧑‍🎓");
        userRepository.save(user);
        flash.addFlashAttribute("success", "Your profile details have been updated.");
        return "redirect:/profile";
    }
}
