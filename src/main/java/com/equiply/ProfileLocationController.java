package com.equiply;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileLocationController {
    private final UserRepository userRepository;

    public ProfileLocationController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/profile/location")
    public String editLocation(Authentication authentication, Model model) {
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        model.addAttribute("currentUser", user);
        return "profile-location";
    }

    @PostMapping("/profile/location")
    public String saveLocation(@RequestParam String collegeName,
                               @RequestParam Double latitude,
                               @RequestParam Double longitude,
                               Authentication authentication,
                               RedirectAttributes flash) {
        if (collegeName == null || collegeName.isBlank() || latitude == null || longitude == null
                || !Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            flash.addFlashAttribute("error", "Enter a college name and valid coordinates.");
            return "redirect:/profile/location";
        }
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        user.setCollegeName(collegeName.trim());
        user.setLatitude(latitude);
        user.setLongitude(longitude);
        userRepository.save(user);
        flash.addFlashAttribute("success", "Your college and pickup area have been saved.");
        return "redirect:/dashboard";
    }
}
