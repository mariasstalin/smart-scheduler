
package com.smartscheduler.user.service;

import com.smartscheduler.user.model.User;
import com.smartscheduler.user.repo.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository repo;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public User register(String email, String password, String name, String phone) {
        if (repo.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(password));
        u.setName(name);
        u.setPhone(phone);
        return repo.save(u);
    }

    public Optional<User> findByEmail(String email) {
        return repo.findByEmail(email);
    }

    public Optional<User> findById(Long id) {
        return repo.findById(id);
    }

    public boolean verifyPassword(User user, String rawPassword) {
        return encoder.matches(rawPassword, user.getPasswordHash());
    }
}
