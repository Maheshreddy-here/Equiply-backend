package com.equiply;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/** Best-effort public image lookup using Wikimedia's public API (no API key required). */
@Service
public class ProductImageService {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3)).build();

    public ProductImageService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String findImage(String productName, String category) {
        String query = ((productName == null ? "" : productName.trim()) + " "
                + (category == null ? "" : category.trim())).trim();
        if (query.isBlank()) return fallback(category);
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://en.wikipedia.org/w/api.php?action=query&generator=search&gsrsearch="
                    + encoded + "&gsrnamespace=0&gsrlimit=5&prop=pageimages&piprop=thumbnail&pithumbsize=900&format=json";
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(4))
                    .header("User-Agent", "EquiplyStudentRental/1.0 (product image lookup)")
                    .GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode pages = objectMapper.readTree(response.body()).path("query").path("pages");
                if (pages.isObject()) {
                    for (JsonNode page : pages) {
                        String thumbnail = page.path("thumbnail").path("source").asText("");
                        if (thumbnail.startsWith("https://") || thumbnail.startsWith("http://")) return thumbnail;
                    }
                }
            }
        } catch (Exception ignored) {
            // Listing should still work if the public image lookup is unavailable.
        }
        return fallback(category);
    }

    private String fallback(String category) {
        String value = category == null ? "" : category.toLowerCase();
        if (value.contains("camera") || value.contains("photo")) return "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?auto=format&fit=crop&w=900&q=80";
        if (value.contains("sport")) return "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?auto=format&fit=crop&w=900&q=80";
        if (value.contains("book") || value.contains("academic")) return "https://images.unsplash.com/photo-1507842217343-583bb7270b66?auto=format&fit=crop&w=900&q=80";
        if (value.contains("tool")) return "https://images.unsplash.com/photo-1504148455328-c376907d081c?auto=format&fit=crop&w=900&q=80";
        if (value.contains("electronic") || value.contains("presentation")) return "https://images.unsplash.com/photo-1496181133206-80ce9b88a853?auto=format&fit=crop&w=900&q=80";
        return "https://images.unsplash.com/photo-1581235720704-06d3acfcb36f?auto=format&fit=crop&w=900&q=80";
    }
}
