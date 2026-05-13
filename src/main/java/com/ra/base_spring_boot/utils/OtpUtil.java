package com.ra.base_spring_boot.utils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OtpUtil {
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String ALL_CHARS = UPPERCASE + LOWERCASE + DIGITS;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateOtp(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALL_CHARS.charAt(RANDOM.nextInt(ALL_CHARS.length())));
        }
        return sb.toString();
    }

    public static String generatePassword() {
        List<Character> password = new ArrayList<>();
        
        password.add(UPPERCASE.charAt(RANDOM.nextInt(UPPERCASE.length())));
        password.add(LOWERCASE.charAt(RANDOM.nextInt(LOWERCASE.length())));
        password.add(DIGITS.charAt(RANDOM.nextInt(DIGITS.length())));
        
        for (int i = 3; i < 8; i++) {
            password.add(ALL_CHARS.charAt(RANDOM.nextInt(ALL_CHARS.length())));
        }
        
        Collections.shuffle(password, RANDOM);
        
        StringBuilder result = new StringBuilder(8);
        for (Character c : password) {
            result.append(c);
        }
        return result.toString();
    }
}

