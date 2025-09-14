
package com.smartscheduler.user.web;

import com.smartscheduler.user.model.User;
import com.smartscheduler.user.service.UserService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final UserService userService;

    @Value("${jwt.secret:changeit}")
    private String jwtSecret;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        String name = body.getOrDefault("name", "");
        String phone = body.getOrDefault("phone", "");
        User u = userService.register(email, password, name, phone);
        Map<String, Object> res = new HashMap<>();
        res.put("id", u.getId());
        res.put("email", u.getEmail());
        return ResponseEntity.ok(res);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        User user = userService.findByEmail(email).orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (!userService.verifyPassword(user, password)) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
        String token = Jwts.builder()
                .setSubject(user.getId().toString())
                .setIssuedAt(new Date())
                .signWith(SignatureAlgorithm.HS256, jwtSecret.getBytes())
                .compact();
        Map<String, String> res = new HashMap<>();
        res.put("token", token);
        return ResponseEntity.ok(res);
    }
}
