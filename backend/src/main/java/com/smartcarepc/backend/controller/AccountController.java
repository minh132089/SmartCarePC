package com.smartcarepc.backend.controller;

import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.smartcarepc.backend.entity.AppUser;
import com.smartcarepc.backend.entity.enums.Role;
import com.smartcarepc.backend.repository.AppUserRepository;
import com.smartcarepc.backend.service.AdminTokens;

@RestController
@RequestMapping("/api/auth")
public class AccountController {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    /** Tài khoản quản trị đăng nhập bằng tên "admin"; trong database nó có email này */
    static final String ADMIN_EMAIL = "admin@smartcarepc.local";
    private final AppUserRepository users;
    private final AdminTokens adminTokens;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();

    public AccountController(AppUserRepository users, AdminTokens adminTokens) { this.users = users; this.adminTokens = adminTokens; }
    /** firstName / lastName chỉ dùng khi đăng ký, không bắt buộc (bản app cũ không gửi) */
    record Credentials(String email, String password, String firstName, String lastName) {}
    record User(String email, String name) {}

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public User register(@RequestBody Credentials input) {
        String email = normalize(input.email());
        if (input.password() == null || input.password().length() < 8 || input.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mật khẩu cần ít nhất 8 ký tự và không quá 72 byte.");
        // Tên miền nội bộ dành cho tài khoản quản trị, không cho đăng ký từ ngoài
        if (email.endsWith("@smartcarepc.local")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email không hợp lệ.");
        if (users.existsByEmail(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã được đăng ký.");
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwords.encode(input.password()));
        user.setFullName(fullName(input, email));
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

    /** Họ tên người dùng nhập; để trống thì lấy phần trước @ của email như trước */
    static String fullName(Credentials input, String email) {
        String name = (clean(input.firstName()) + " " + clean(input.lastName())).trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) return email.substring(0, email.indexOf('@'));
        return name.length() > 100 ? name.substring(0, 100).trim() : name;
    }

    private static String clean(String part) { return part == null ? "" : part.strip(); }

    record AdminLogin(String username, String password) {}
    record AdminSession(String token, long expiresAt, String name) {}

    /** Đăng nhập trang /feedback: chỉ tài khoản có quyền ADMIN. Trả vé xem dashboard 12 giờ (không tải được CSV). */
    @PostMapping("/admin")
    public AdminSession admin(@RequestBody AdminLogin input) {
        String name = input.username() == null ? "" : input.username().trim().toLowerCase(Locale.ROOT);
        String email = name.contains("@") ? name : name + "@smartcarepc.local";
        AppUser user = users.findByEmail(email).orElse(null);
        if (user == null || user.getRole() != Role.ADMIN || input.password() == null || !passwords.matches(input.password(), user.getPasswordHash()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect username or password.");
        var issued = adminTokens.issue(user.getEmail());
        return new AdminSession(issued.token(), issued.expiresAt(), user.getFullName());
    }

    private String normalize(String email) {
        if (email == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email không hợp lệ.");
        String result = email.trim().toLowerCase(Locale.ROOT);
        if (result.length() > 150 || !EMAIL.matcher(result).matches())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email không hợp lệ.");
        return result;
    }
}
