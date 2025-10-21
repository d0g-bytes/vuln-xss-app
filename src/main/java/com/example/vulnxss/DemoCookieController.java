// src/main/java/com/example/vulnxss/DemoCookieController.java
package com.example.vulnxss;

import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;

@RestController
public class DemoCookieController {

    @GetMapping("/set-demo-cookie")
    public ResponseEntity<String> setDemoCookie(HttpServletResponse response) {
        // Demo cookie: NOT HttpOnly so JavaScript can read it (for demo only)
        ResponseCookie cookie = ResponseCookie.from("session", "FAKE_FOR_DEMO")
                .path("/")
                .httpOnly(false)      // important for demo: readable by document.cookie
                .secure(false)        // set true only if serving over HTTPS
                .sameSite("Lax")
                .maxAge(60 * 60)      // 1 hour
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
        // redirect or return a page — here we return a simple confirmation
        return ResponseEntity.ok("Demo cookie set. document.cookie now contains session=FAKE_FOR_DEMO");
    }
}
