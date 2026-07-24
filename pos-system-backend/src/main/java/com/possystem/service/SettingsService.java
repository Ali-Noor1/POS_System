package com.possystem.service;

import com.possystem.dto.AppSettingsResponse;
import com.possystem.dto.ChangePasswordRequest;
import com.possystem.dto.ReceiptSettingsRequest;
import com.possystem.dto.ReceiptSettingsResponse;
import com.possystem.dto.StoreSettingsRequest;
import com.possystem.dto.StoreSettingsResponse;
import com.possystem.entity.AppSetting;
import com.possystem.entity.User;
import com.possystem.repository.AppSettingRepository;
import com.possystem.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class SettingsService {

    private static final String STORE_NAME = "store.name";
    private static final String STORE_ADDRESS = "store.address";
    private static final String STORE_PHONE = "store.phone";
    private static final String STORE_EMAIL = "store.email";
    private static final String STORE_LOGO_URL = "store.logoUrl";
    private static final String RECEIPT_HEADER = "receipt.headerText";
    private static final String RECEIPT_FOOTER = "receipt.footerText";
    private static final String RECEIPT_TAX_PERCENTAGE = "receipt.taxPercentage";
    private static final String RECEIPT_CURRENCY_SYMBOL = "receipt.currencySymbol";
    private static final String RECEIPT_SHOW_CASHIER = "receipt.showCashierName";
    private static final String RECEIPT_SHOW_CUSTOMER = "receipt.showCustomerInfo";
    private static final long MAX_LOGO_SIZE_BYTES = 2 * 1024 * 1024;
    private static final Set<String> ALLOWED_LOGO_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final AppSettingRepository appSettingRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final Path storeUploadDirectory;

    public SettingsService(
            AppSettingRepository appSettingRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService,
            @Value("${app.upload.store-dir}") String storeUploadDirectory
    ) {
        this.appSettingRepository = appSettingRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.storeUploadDirectory = Paths.get(storeUploadDirectory)
                .toAbsolutePath()
                .normalize();
    }

    @Transactional(readOnly = true)
    public AppSettingsResponse getSettings() {
        Map<String, AppSetting> settingsByKey = appSettingRepository
                .findAll()
                .stream()
                .collect(Collectors.toMap(AppSetting::getKey, Function.identity()));

        return AppSettingsResponse.builder()
                .store(buildStoreSettings(settingsByKey))
                .receipt(buildReceiptSettings(settingsByKey))
                .build();
    }

    @Transactional
    public StoreSettingsResponse updateStoreSettings(StoreSettingsRequest request) {
        saveSetting(STORE_NAME, cleanRequired(request.getStoreName()));
        saveSetting(STORE_ADDRESS, cleanOptional(request.getAddress()));
        saveSetting(STORE_PHONE, cleanOptional(request.getPhone()));
        saveSetting(STORE_EMAIL, cleanOptional(request.getEmail()));
        saveSetting(STORE_LOGO_URL, cleanOptional(request.getLogoUrl()));

        auditLogService.record(
                "STORE_SETTINGS_UPDATED",
                "SETTINGS",
                null,
                "Store profile settings updated"
        );

        return getSettings().getStore();
    }

    @Transactional
    public StoreSettingsResponse uploadStoreLogo(MultipartFile image) {
        validateStoreLogo(image);

        String contentType = image.getContentType();
        String fileExtension = switch (contentType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };

        String generatedFileName = UUID.randomUUID() + fileExtension;

        try {
            Files.createDirectories(storeUploadDirectory);

            Path targetFile = storeUploadDirectory
                    .resolve(generatedFileName)
                    .normalize();

            if (!targetFile.startsWith(storeUploadDirectory)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid logo file path"
                );
            }

            try (InputStream inputStream = image.getInputStream()) {
                Files.copy(
                        inputStream,
                        targetFile,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            saveSetting(STORE_LOGO_URL, "/uploads/store/" + generatedFileName);

            auditLogService.record(
                    "STORE_LOGO_UPDATED",
                    "SETTINGS",
                    null,
                    "Store logo updated"
            );

            return getSettings().getStore();
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not save store logo"
            );
        }
    }

    @Transactional
    public ReceiptSettingsResponse updateReceiptSettings(ReceiptSettingsRequest request) {
        saveSetting(RECEIPT_HEADER, cleanOptional(request.getHeaderText()));
        saveSetting(RECEIPT_FOOTER, cleanOptional(request.getFooterText()));
        saveSetting(
                RECEIPT_TAX_PERCENTAGE,
                request.getTaxPercentage().stripTrailingZeros().toPlainString()
        );
        saveSetting(RECEIPT_CURRENCY_SYMBOL, cleanRequired(request.getCurrencySymbol()));
        saveSetting(RECEIPT_SHOW_CASHIER, request.getShowCashierName().toString());
        saveSetting(RECEIPT_SHOW_CUSTOMER, request.getShowCustomerInfo().toString());

        auditLogService.record(
                "RECEIPT_SETTINGS_UPDATED",
                "SETTINGS",
                null,
                "Receipt settings updated"
        );

        return getSettings().getReceipt();
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request, Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication is required"
            );
        }

        User admin = userRepository
                .findByUsername(principal.getName())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Admin user was not found"
                ));

        if (!"ADMIN".equalsIgnoreCase(admin.getRole().getName())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only admins can change settings password"
            );
        }

        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                admin.getPasswordHash()
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Current password is incorrect"
            );
        }

        admin.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        User updatedAdmin = userRepository.save(admin);

        auditLogService.record(
                "ADMIN_PASSWORD_CHANGED",
                "USER",
                updatedAdmin.getId(),
                "Admin password changed: " + updatedAdmin.getUsername()
        );
    }

    private StoreSettingsResponse buildStoreSettings(
            Map<String, AppSetting> settingsByKey
    ) {
        return StoreSettingsResponse.builder()
                .storeName(setting(settingsByKey, STORE_NAME, "Retail POS"))
                .address(setting(settingsByKey, STORE_ADDRESS, ""))
                .phone(setting(settingsByKey, STORE_PHONE, ""))
                .email(setting(settingsByKey, STORE_EMAIL, ""))
                .logoUrl(setting(settingsByKey, STORE_LOGO_URL, ""))
                .build();
    }

    private ReceiptSettingsResponse buildReceiptSettings(
            Map<String, AppSetting> settingsByKey
    ) {
        return ReceiptSettingsResponse.builder()
                .headerText(setting(settingsByKey, RECEIPT_HEADER, "Thank you for shopping with us."))
                .footerText(setting(settingsByKey, RECEIPT_FOOTER, "Goods once sold are returnable only by store policy."))
                .taxPercentage(decimalSetting(settingsByKey, RECEIPT_TAX_PERCENTAGE, "0.00"))
                .currencySymbol(setting(settingsByKey, RECEIPT_CURRENCY_SYMBOL, "Rs"))
                .showCashierName(booleanSetting(settingsByKey, RECEIPT_SHOW_CASHIER, true))
                .showCustomerInfo(booleanSetting(settingsByKey, RECEIPT_SHOW_CUSTOMER, true))
                .build();
    }

    private String setting(
            Map<String, AppSetting> settingsByKey,
            String key,
            String fallback
    ) {
        AppSetting setting = settingsByKey.get(key);
        return setting == null || setting.getValue() == null
                ? fallback
                : setting.getValue();
    }

    private BigDecimal decimalSetting(
            Map<String, AppSetting> settingsByKey,
            String key,
            String fallback
    ) {
        try {
            return new BigDecimal(setting(settingsByKey, key, fallback));
        } catch (NumberFormatException exception) {
            return new BigDecimal(fallback);
        }
    }

    private Boolean booleanSetting(
            Map<String, AppSetting> settingsByKey,
            String key,
            boolean fallback
    ) {
        return Boolean.parseBoolean(
                setting(settingsByKey, key, Boolean.toString(fallback))
        );
    }

    private void saveSetting(String key, String value) {
        AppSetting setting = appSettingRepository
                .findById(key)
                .orElseGet(() -> new AppSetting(key, null));
        setting.setValue(value);
        appSettingRepository.save(setting);
    }

    private String cleanRequired(String value) {
        return value.trim();
    }

    private String cleanOptional(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "";
        }

        return value.trim();
    }

    private void validateStoreLogo(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Store logo file is required"
            );
        }

        if (image.getSize() > MAX_LOGO_SIZE_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Store logo must not exceed 2 MB"
            );
        }

        String contentType = image.getContentType();

        if (contentType == null
                || !ALLOWED_LOGO_TYPES.contains(
                contentType.toLowerCase(Locale.ROOT)
        )) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only JPG, PNG, and WEBP logo files are allowed"
            );
        }
    }
}
