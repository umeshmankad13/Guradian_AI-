package com.guardianai.api_gateway.service;

import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.guardianai.api_gateway.dto.ProfileResponse;
import com.guardianai.api_gateway.dto.SettingsResponse;
import com.guardianai.api_gateway.dto.UpdateProfileRequest;
import com.guardianai.api_gateway.dto.UpdateSettingsRequest;
import com.guardianai.api_gateway.entity.User;
import com.guardianai.api_gateway.entity.UserSettings;
import com.guardianai.api_gateway.repository.UserRepository;
import com.guardianai.api_gateway.repository.UserSettingsRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserSettingsRepository settingsRepository;

    public UserService(UserRepository userRepository,
                       UserSettingsRepository settingsRepository) {
        this.userRepository = userRepository;
        this.settingsRepository = settingsRepository;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID userId) {
        return toProfile(findUser(userId));
    }

    @Transactional
    public ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        user.setName(request.name().trim());   // saved automatically at the end of the transaction
        return toProfile(user);
    }

    @Transactional
    public SettingsResponse getSettings(UUID userId) {
        return toSettings(findOrCreateSettings(userId));
    }

    @Transactional
    public SettingsResponse updateSettings(UUID userId, UpdateSettingsRequest request) {
        UserSettings s = findOrCreateSettings(userId);
        s.setScanOnBrowse(request.scanOnBrowse());
        s.setShowNotifications(request.showNotifications());
        s.setRiskThreshold(request.riskThreshold());
        s.setTheme(request.theme());
        s.setLanguage(request.language());
        return toSettings(s);
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private UserSettings findOrCreateSettings(UUID userId) {
        return settingsRepository.findByUserId(userId).orElseGet(() -> {
            UserSettings s = new UserSettings();
            s.setUser(findUser(userId));
            return settingsRepository.save(s);
        });
    }

    private ProfileResponse toProfile(User u) {
        return new ProfileResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(),
                u.getCreatedAt().toInstant(ZoneOffset.UTC));
    }

    private SettingsResponse toSettings(UserSettings s) {
        return new SettingsResponse(s.isScanOnBrowse(), s.isShowNotifications(),
                s.getRiskThreshold(), s.getTheme(), s.getLanguage());
    }
}