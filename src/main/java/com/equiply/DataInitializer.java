package com.equiply;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedEquipment(EquipmentRepository repository, UserRepository users, PasswordEncoder encoder,
            @Value("${equiply.admin.email:}") String adminEmail,
            @Value("${equiply.admin.password:}") String adminPassword,
            @Value("${equiply.admin.name:Equiply Administrator}") String adminName) {
        return args -> {
            if (adminEmail != null && !adminEmail.isBlank() && adminPassword != null && !adminPassword.isBlank()) {
                String normalized = adminEmail.trim().toLowerCase();
                User existing = users.findByEmail(normalized).orElse(null);
                if (existing == null) {
                    if (adminPassword.length() < 12) throw new IllegalStateException("Initial admin password must be at least 12 characters.");
                    User admin = new User();
                    admin.setFullName(adminName); admin.setMobile("Not provided"); admin.setEmail(normalized);
                    admin.setAddress("Private"); admin.setCollegeName("Platform Administration");
                    admin.setPassword(encoder.encode(adminPassword)); admin.setRole("ADMIN");
                    admin.setEmailVerified(true); admin.setAccountActive(true); users.save(admin);
                    System.out.println("Equiply initial admin created for: " + normalized);
                } else if (!"ADMIN".equalsIgnoreCase(existing.getRole())) {
                    System.out.println("Initial admin email already belongs to a non-admin account. No automatic role change was made.");
                }
            }

            if (repository.count() > 0) {
                return;
            }

            repository.save(create("Professional Camera", "Photography",
                    "High-quality camera for projects, events and documentation.",
                    "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?auto=format&fit=crop&w=900&q=80"));

            repository.save(create("HD Projector", "Presentation",
                    "HD projector for classrooms, presentations and club events.",
                    "https://images.unsplash.com/photo-1478720568477-152d9b164e26?auto=format&fit=crop&w=900&q=80"));

            repository.save(create("Engineering Laptop", "Electronics",
                    "Shared laptop for engineering projects, coding and demonstrations.",
                    "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?auto=format&fit=crop&w=900&q=80"));

            repository.save(create("Tripod Stand", "Photography",
                    "Adjustable tripod for cameras, phones and project recording.",
                    "https://images.unsplash.com/photo-1606986628253-38d4a7a3c2c0?auto=format&fit=crop&w=900&q=80"));

            repository.save(create("Cordless Drill", "Tools",
                    "Cordless drill for workshop and makerspace projects.",
                    "https://images.unsplash.com/photo-1504148455328-c376907d081c?auto=format&fit=crop&w=900&q=80"));

            repository.save(create("Wireless Microphone", "Audio",
                    "Wireless microphone for presentations, events and recordings.",
                    "https://images.unsplash.com/photo-1590602847861-f357a9332bbc?auto=format&fit=crop&w=900&q=80"));
        };
    }

    private Equipment create(String name, String category, String description, String imageUrl) {
        Equipment equipment = new Equipment();
        equipment.setName(name);
        equipment.setCategory(category);
        equipment.setDescription(description);
        equipment.setImageUrl(imageUrl);
        equipment.setStatus(EquipmentStatus.AVAILABLE);
        return equipment;
    }
}
