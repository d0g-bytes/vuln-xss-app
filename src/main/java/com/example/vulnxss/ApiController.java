// src/main/java/com/example/vulnxss/ApiController.java
package com.example.vulnxss;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Map;

@RestController
public class ApiController {

    // Thread-safe in-memory store for comments
    private final List<Map<String, String>> comments = new CopyOnWriteArrayList<>();

    // Reflected endpoint: echoes q param as JSON (client-side will inject it)
    @GetMapping("/api/echo")
    public ResponseEntity<Map<String,String>> echo(@RequestParam(name="q", required=false, defaultValue="") String q) {
        return ResponseEntity.ok(Map.of("q", q));
    }

    // List comments (stored data)
    @GetMapping("/api/comments")
    public List<Map<String,String>> listComments() {
        return comments;
    }

    // Post comment (stored XSS demo)
    @PostMapping("/api/comments")
    public ResponseEntity<?> addComment(@RequestBody Map<String, String> payload) {
        String text = payload.getOrDefault("text", "");
        comments.add(Map.of("text", text)); // VULNERABLE: storing raw text
        return ResponseEntity.ok(Map.of("ok","true"));
    }
}
