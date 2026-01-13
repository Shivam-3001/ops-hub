package com.company.ops_hub_api.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Utility to generate BCrypt password hashes for SQL seeding
 * Run this main method to get the hashes for your SQL script
 */
public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        System.out.println("=== BCrypt Password Hashes ===");
        System.out.println();
        System.out.println("Password: admin123");
        System.out.println("Hash: " + encoder.encode("admin123"));
        System.out.println();
        System.out.println("Password: password123");
        System.out.println("Hash: " + encoder.encode("password123"));
        System.out.println();
        System.out.println("=== Copy these hashes to your SQL script ===");
    }
}
