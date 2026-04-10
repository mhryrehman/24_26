package com.keycloak.otp.util;

import jakarta.xml.bind.ValidationException;
import org.jboss.logging.Logger;

import java.util.regex.Pattern;

public final class PhoneValidator {

    private static final Logger LOG = Logger.getLogger(PhoneValidator.class);

    private static final Pattern ALL_SAME_DIGITS = Pattern.compile("^(\\d)\\1+$");

    private PhoneValidator() {
    }

    public static String validateAndNormalizeUs(String raw) throws ValidationException {
        LOG.info("Validating phone number: " + raw);
        if (raw == null || raw.trim().isEmpty()) {
            LOG.info("Phone number is null");
            throw new ValidationException("Phone number is required");
        }

        String trimmed = raw.trim();

        if (trimmed.matches(".*[A-Za-z].*")) {
            LOG.info("Phone number contains letters: " + trimmed);
            throw new ValidationException("Phone number must not contain letters");
        }

        String digits = trimmed.replaceAll("\\D", "");

        if (digits.length() == 11 && digits.startsWith("1")) {
            digits = digits.substring(1);
        }

        if (digits.length() < 10) {
            LOG.info("Phone number must contain 10 digits after normalization: " + digits);
            throw new ValidationException("Phone number must contain 10 digits");
        }

        if (digits.length() > 15) {
            LOG.info("Phone number is too long: " + digits);
            throw new ValidationException("Phone number should not be greater than 15 digits");
        }

        if (ALL_SAME_DIGITS.matcher(digits).matches()) {
            LOG.info("Phone number contains repeated digits: " + digits);
            throw new ValidationException("Phone number cannot contain repeated digits");
        }

        if (isSimpleSequence(digits)) {
            LOG.info("Phone number is a simple sequence: " + digits);
            throw new ValidationException("Phone number cannot be a simple sequence");
        }

        if (digits.charAt(0) == '0' || digits.charAt(0) == '1') {
            LOG.info("Phone number has invalid area code: " + digits);
            throw new ValidationException("Invalid US area code");
        }

        if (digits.charAt(3) == '0' || digits.charAt(3) == '1') {
            LOG.info("Phone number has invalid exchange code: " + digits);
            throw new ValidationException("Invalid US exchange code");
        }

        return "+1" + digits;
    }

    private static boolean isSimpleSequence(String digits) {
        if (digits == null || digits.length() < 7) {
            return false;
        }

        boolean ascending = true;
        boolean descending = true;

        for (int i = 1; i < digits.length(); i++) {
            int prev = digits.charAt(i - 1) - '0';
            int curr = digits.charAt(i) - '0';

            int ascExpected = (prev + 1) % 10;
            int descExpected = (prev + 9) % 10; // same as prev - 1 with wrap

            if (curr != ascExpected) {
                ascending = false;
            }
            if (curr != descExpected) {
                descending = false;
            }

            if (!ascending && !descending) {
                return false;
            }
        }
        return true;
    }
}