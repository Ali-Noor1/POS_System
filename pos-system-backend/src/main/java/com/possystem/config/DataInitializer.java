package com.possystem.config;

import com.possystem.entity.Role;
import com.possystem.entity.User;
import com.possystem.repository.RoleRepository;
import com.possystem.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createRoleIfNotExists(
                "ADMIN",
                "Can manage users, products, stock, sales, and reports."
        );

        createRoleIfNotExists(
                "CASHIER",
                "Can process sales and view their own sales history."
        );

        createDefaultAdminIfNotExists();
        createDefaultCashierIfNotExists();
    }

    private void createRoleIfNotExists(String roleName, String description) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            Role role = new Role(roleName, description);
            roleRepository.save(role);
        }
    }

    private void createDefaultAdminIfNotExists() {
        String adminUsername = "admin";

        if (userRepository.findByUsername(adminUsername).isEmpty()) {

            Role adminRole = roleRepository.findByName("ADMIN")
                    .orElseThrow(() ->
                            new IllegalStateException("ADMIN role was not found.")
                    );

            User adminUser = new User(
                    "System Administrator",
                    "admin",
                    "admin@possystem.local",
                    passwordEncoder.encode("Admin@123"),
                    "ACTIVE",
                    adminRole
            );

            userRepository.save(adminUser);

            System.out.println("Default Admin user created: admin");
        }
    }

    private void createDefaultCashierIfNotExists() {
        String cashierUsername = "cashier";

        if (userRepository.findByUsername(cashierUsername).isEmpty()) {

            Role cashierRole = roleRepository.findByName("CASHIER")
                    .orElseThrow(() ->
                            new IllegalStateException("CASHIER role was not found.")
                    );

            User cashierUser = new User(
                    "Default Cashier",
                    "cashier",
                    "cashier@possystem.local",
                    passwordEncoder.encode("Cashier@123"),
                    "ACTIVE",
                    cashierRole
            );

            userRepository.save(cashierUser);

            System.out.println("Default Cashier user created: cashier");
        }
    }
}