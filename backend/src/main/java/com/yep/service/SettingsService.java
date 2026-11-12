package com.yep.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yep.domain.platform.SystemSetting;
import com.yep.repository.SystemSettingRepository;

@Service
public class SettingsService {

    public static final String COUNTRY_LIST = "country_list";
    public static final String DEFAULT_PASS_MARK = "default_pass_mark";
    public static final String DEFAULT_ATTENDANCE_THRESHOLD = "default_attendance_threshold";
    public static final String LEVEL_TWO_CAPACITY = "level_two_capacity";
    public static final String PLATFORM_NAME = "platform_name";

    public static final String DEFAULT_COUNTRIES = "Uganda,Kenya,Tanzania,Rwanda,Burundi,South Sudan,"
            + "DR Congo,Somalia,Ethiopia,Djibouti,Other";

    private final SystemSettingRepository settingRepository;

    public SettingsService(SystemSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    public String value(String key, String fallback) {
        return settingRepository.findById(key).map(SystemSetting::getSettingValue).orElse(fallback);
    }

    public int intValue(String key, int fallback) {
        try {
            return Integer.parseInt(value(key, String.valueOf(fallback)).trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public List<String> countryList() {
        return Arrays.stream(value(COUNTRY_LIST, DEFAULT_COUNTRIES).split(","))
                .map(String::trim)
                .filter(country -> !country.isEmpty())
                .toList();
    }

    @Transactional
    public void seedDefaults() {
        seed(PLATFORM_NAME, "YEP - Youth Empowerment Platform");
        seed(COUNTRY_LIST, DEFAULT_COUNTRIES);
        seed(DEFAULT_PASS_MARK, "50");
        seed(DEFAULT_ATTENDANCE_THRESHOLD, "75");
        seed(LEVEL_TWO_CAPACITY, "30");
    }

    private void seed(String key, String value) {
        if (!settingRepository.existsById(key)) {
            settingRepository.save(new SystemSetting(key, value));
        }
    }
}
