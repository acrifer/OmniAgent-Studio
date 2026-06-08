package com.devmind.studio.config;

import com.devmind.studio.entity.User;
import com.devmind.studio.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner initTestUser(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            if (!users.existsByUsername("demo")) {
                User user = new User();
                user.setUsername("demo");
                user.setEmail("demo@devmind.local");
                user.setPasswordHash(encoder.encode("demo123456"));
                users.save(user);
            }
        };
    }
}
