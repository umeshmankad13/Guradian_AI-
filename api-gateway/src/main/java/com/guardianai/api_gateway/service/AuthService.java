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

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserSettingsRepository settingsRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       UserSettingsRepository settingsRepository,
                       PasswordEncoder passwordEncoder,
                        JwtService jwtService) {
        this.userRepository = userRepository;
        this.settingsRepository = settingsRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService=jwtService;
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

        return toResponse(user);
        // return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }

     @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        // same message for "no such user" and "wrong password", so attackers can't tell which emails exist
        User user = userRepository.findByEmail(email)
                .filter(User::isActive)
                .filter(u -> passwordEncoder.matches(request.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        String token = jwtService.generateToken(user.getId(), user.getEmail(), user.getRole());

        return new LoginResponse(token, "Bearer", jwtService.getExpirySeconds(), toResponse(user));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}