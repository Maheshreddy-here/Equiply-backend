package com.equiply;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
    }

    @GetMapping("/login") public String login() { return "login"; }
    @GetMapping("/signup") public String signup() { return "signup"; }

    @PostMapping("/signup")
    public String register(@RequestParam String fullName, @RequestParam String mobile,
            @RequestParam String email, @RequestParam String address, @RequestParam String collegeName,
            @RequestParam Double latitude, @RequestParam Double longitude, @RequestParam String password,
            Model model) {
        email = email.trim().toLowerCase();
        if (collegeName == null || collegeName.isBlank() || latitude == null || longitude == null
                || !Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            model.addAttribute("error", "Enter your college and a valid campus/pickup location.");
            return "signup";
        }
        if (password == null || password.length() < 6) {
            model.addAttribute("error", "Your password must contain at least 6 characters.");
            return "signup";
        }
        if (userRepository.existsByEmail(email)) {
            model.addAttribute("error", "An account with this email already exists.");
            return "signup";
        }
        User user = new User();
        user.setFullName(fullName.trim()); user.setMobile(mobile.trim()); user.setEmail(email);
        user.setAddress(address == null ? "" : address.trim()); user.setCollegeName(collegeName.trim());
        user.setLatitude(latitude); user.setLongitude(longitude);
        user.setPassword(passwordEncoder.encode(password)); user.setRole("USER");
        user.setEmailVerified(false);
        user.setEmailVerificationToken(UUID.randomUUID().toString());
        user.setEmailVerificationExpiresAt(LocalDateTime.now().plusHours(24));
        userRepository.save(user);
        try {
            if (emailVerificationService.sendVerification(user)) {
                model.addAttribute("success", "Account created. Check your inbox and click the verification link before logging in.");
            } else {
                model.addAttribute("error", "Your account is saved but email sending is not configured yet. Set up SMTP using EMAIL_SETUP.txt, then restart and create a new account with a different email.");
            }
        } catch (Exception ex) {
            model.addAttribute("error", "Your account is saved, but the verification email could not be sent. Check SMTP settings and server logs, then contact the site administrator.");
        }
        return "signup";
    }

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String token, Model model) {
        User user = userRepository.findByEmailVerificationToken(token).orElse(null);
        if (user == null) {
            model.addAttribute("error", "This verification link is invalid or has already been used.");
            return "login";
        }
        if (user.getEmailVerificationExpiresAt() == null || user.getEmailVerificationExpiresAt().isBefore(LocalDateTime.now())) {
            model.addAttribute("error", "This verification link has expired. Please contact the administrator to request a new link.");
            return "login";
        }
        user.setEmailVerified(true); user.setEmailVerificationToken(null); user.setEmailVerificationExpiresAt(null);
        userRepository.save(user);
        model.addAttribute("verified", "Email verified successfully. You can now log in to Equiply.");
        return "login";
    }
}
