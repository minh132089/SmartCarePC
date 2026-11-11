package com.smartcarepc.backend.controller;

import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.smartcarepc.backend.entity.AppUser;
import com.smartcarepc.backend.repository.AppUserRepository;

@RestController
@RequestMapping("/api/auth")
public class AccountController {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final AppUserRepository users;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();

    public AccountController(AppUserRepository users) { this.users = users; }
    record Credentials(String email, String password) {}
    record User(String email, String name) {}

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public User register(@RequestBody Credentials input) {
        String email = normalize(input.email());
        if (input.password() == null || input.password().length() < 8 || input.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu cần ít nhất 8 ký tự và không quá 72 byte.");
        if (users.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã được đăng ký.");
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwords.encode(input.password()));
        user.setFullName(email.substring(0, email.indexOf('@')));
        try { users.saveAndFlush(user); }
        catch (DataIntegrityViolationException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã được đăng ký."); }
        return new User(user.getEmail(), user.getFullName());
    }

    @PostMapping("/login")
    public User login(@RequestBody Credentials input) {
        String email = normalize(input.email());
        AppUser user = users.findByEmail(email).orElse(null);
        if (user == null || input.password() == null || !passwords.matches(input.password(), user.getPasswordHash()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không chính xác.");
        return new User(user.getEmail(), user.getFullName());
    }

    private String normalize(String email) {
        if (email == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email không hợp lệ.");
        String result = email.trim().toLowerCase(Locale.ROOT);
        if (result.length() > 150 || !EMAIL.matcher(result).matches())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email không hợp lệ.");
        return result;
    }
}
