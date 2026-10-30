package com.guardianai.api_gateway.service;

import com.guardianai.api_gateway.dto.LoginRequest;
import com.guardianai.api_gateway.dto.LoginResponse;
import com.guardianai.api_gateway.dto.RegisterRequest;
import com.guardianai.api_gateway.dto.UserResponse;
import com.guardianai.api_gateway.entity.User;
import com.guardianai.api_gateway.entity.UserSettings;
import com.guardianai.api_gateway.repository.UserRepository;
import com.guardianai.api_gateway.repository.UserSettingsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserSettingsRepository settingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final String dummyHash;

    public AuthService(UserRepository userRepository,
                       UserSettingsRepository settingsRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.settingsRepository = settingsRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        User user = new User();
        user.setName(request.name().trim());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user = userRepository.save(user);

        UserSettings settings = new UserSettings();
        settings.setUser(user);
        settingsRepository.save(settings);

        auditService.recordAfterCommit(AuditAction.REGISTER, user.getId(), null);
        return toResponse(user);
    }

    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        Optional<User> found = userRepository.findByEmail(email);

        // always run exactly one BCrypt comparison, whether or not the email exists
        String hash = found.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordOk = passwordEncoder.matches(request.password(), hash);

        if (found.isEmpty() || !found.get().isActive() || !passwordOk) {
            // same response for every kind of failure, but the audit log knows which account was targeted
            auditService.record(AuditAction.LOGIN_FAILED, found.map(User::getId).orElse(null), null);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        User user = found.get();
        auditService.record(AuditAction.LOGIN_SUCCESS, user.getId(), null);

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());
        return new LoginResponse(token, "Bearer", jwtService.getExpirySeconds(), toResponse(user));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}