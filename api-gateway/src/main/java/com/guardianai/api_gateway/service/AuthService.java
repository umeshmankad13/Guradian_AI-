package com.guardianai.api_gateway.service;

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

    public AuthService(UserRepository userRepository,
                       UserSettingsRepository settingsRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.settingsRepository = settingsRepository;
        this.passwordEncoder = passwordEncoder;
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

        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}