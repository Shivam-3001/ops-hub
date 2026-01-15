package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.*;
import com.company.ops_hub_api.repository.*;
import com.company.ops_hub_api.util.EncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeederService {

    private final ClusterRepository clusterRepository;
    private final CircleRepository circleRepository;
    private final ZoneRepository zoneRepository;
    private final AreaRepository areaRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionUtil encryptionUtil;
    private final AuditLogService auditLogService;

    @Bean
    @Transactional
    public CommandLineRunner seedData() {
        return args -> {
            if (clusterRepository.count() > 0) {
                log.info("Data already exists. Skipping seed data.");
                return;
            }

            log.info("Starting data seeding...");

            // Create Clusters
            Cluster bupCluster = createCluster("BUP", "Bihar UP");
            Cluster mhCluster = createCluster("MH", "Maharashtra");
            Cluster dlCluster = createCluster("DL", "Delhi");

            // Create Circles
            Circle upCircle = createCircle("UP", "Uttar Pradesh", bupCluster);
            Circle brCircle = createCircle("BR", "Bihar", bupCluster);
            Circle mumbaiCircle = createCircle("MUM", "Mumbai", mhCluster);
            Circle puneCircle = createCircle("PUN", "Pune", mhCluster);
            Circle delhiCircle = createCircle("DEL", "Delhi NCR", dlCluster);

            // Create Zones
            Zone ghaziabadZone = createZone("GZB", "Ghaziabad", upCircle);
            Zone lucknowZone = createZone("LKO", "Lucknow", upCircle);
            Zone patnaZone = createZone("PTN", "Patna", brCircle);
            Zone mumbaiZone = createZone("MUM", "Mumbai City", mumbaiCircle);
            Zone naviMumbaiZone = createZone("NVM", "Navi Mumbai", mumbaiCircle);
            Zone puneZone = createZone("PUN", "Pune City", puneCircle);
            Zone northDelhiZone = createZone("NDL", "North Delhi", delhiCircle);
            Zone southDelhiZone = createZone("SDL", "South Delhi", delhiCircle);

            // Create Areas
            Area behrampurArea = createArea("BHR", "Behrampur", ghaziabadZone);
            Area meerutArea = createArea("MRT", "Meerut", ghaziabadZone);
            Area aligarhArea = createArea("ALG", "Aligarh", ghaziabadZone);
            Area gomtiArea = createArea("GOM", "Gomti Nagar", lucknowZone);
            Area hazratganjArea = createArea("HZG", "Hazratganj", lucknowZone);
            Area danapurArea = createArea("DNP", "Danapur", patnaZone);
            Area kankarbaghArea = createArea("KKB", "Kankarbagh", patnaZone);
            Area andheriArea = createArea("AND", "Andheri", mumbaiZone);
            Area bandraArea = createArea("BND", "Bandra", mumbaiZone);
            Area vashiArea = createArea("VSH", "Vashi", naviMumbaiZone);
            Area khargharArea = createArea("KHG", "Kharghar", naviMumbaiZone);
            Area hinjewadiArea = createArea("HJD", "Hinjewadi", puneZone);
            Area banerArea = createArea("BNR", "Baner", puneZone);
            Area rohiniArea = createArea("RHN", "Rohini", northDelhiZone);
            Area pitampuraArea = createArea("PTM", "Pitampura", northDelhiZone);
            Area saketArea = createArea("SKT", "Saket", southDelhiZone);
            Area vasantKunjArea = createArea("VSK", "Vasant Kunj", southDelhiZone);

            // Create Users with Test Credentials
            createTestUser("EMP001", "shivam", "Shivam Kumar", "shivam@example.com", 
                    "9876543210", "AREA_LEAD", "MANAGER", behrampurArea, "password123", true);
            
            createTestUser("EMP002", "rahul", "Rahul Sharma", "rahul@example.com", 
                    "9876543211", "ZONE_LEAD", "MANAGER", behrampurArea, "password123", true);
            
            createTestUser("EMP003", "priya", "Priya Singh", "priya@example.com", 
                    "9876543212", "CIRCLE_LEAD", "MANAGER", behrampurArea, "password123", true);
            
            createTestUser("EMP004", "admin", "Admin User", "admin@example.com", 
                    "9876543213", "ADMIN", "ADMIN", behrampurArea, "admin123", true);
            
            createTestUser("EMP005", "analyst1", "Analyst One", "analyst1@example.com", 
                    "9876543214", "ANALYST", "ANALYST", meerutArea, "password123", true);
            
            createTestUser("EMP006", "manager1", "Manager One", "manager1@example.com", 
                    "9876543215", "ZONE_LEAD", "MANAGER", meerutArea, "password123", true);

            // Assign Managers
            User shivam = userRepository.findByEmployeeId("EMP001").orElse(null);
            User rahul = userRepository.findByEmployeeId("EMP002").orElse(null);
            User priya = userRepository.findByEmployeeId("EMP003").orElse(null);

            if (shivam != null) {
                behrampurArea.setManager(shivam);
                areaRepository.save(behrampurArea);
            }

            if (rahul != null) {
                ghaziabadZone.setManager(rahul);
                zoneRepository.save(ghaziabadZone);
            }

            if (priya != null) {
                upCircle.setManager(priya);
                circleRepository.save(upCircle);
            }

            log.info("Data seeding completed successfully!");
            log.info("Test Credentials:");
            log.info("  Employee ID: EMP001, Password: password123 (Area Lead)");
            log.info("  Employee ID: EMP002, Password: password123 (Zone Lead)");
            log.info("  Employee ID: EMP003, Password: password123 (Circle Lead)");
            log.info("  Employee ID: EMP004, Password: admin123 (Admin)");
            log.info("  Employee ID: EMP005, Password: password123 (Analyst)");
            log.info("  Employee ID: EMP006, Password: password123 (Zone Lead)");
        };
    }

    private Cluster createCluster(String code, String name) {
        Cluster cluster = new Cluster();
        cluster.setCode(code);
        cluster.setName(name);
        cluster.setDescription("Cluster: " + name);
        cluster.setActive(true);
        return clusterRepository.save(cluster);
    }

    private Circle createCircle(String code, String name, Cluster cluster) {
        Circle circle = new Circle();
        circle.setCode(code);
        circle.setName(name);
        circle.setDescription("Circle: " + name);
        circle.setCluster(cluster);
        circle.setActive(true);
        return circleRepository.save(circle);
    }

    private Zone createZone(String code, String name, Circle circle) {
        Zone zone = new Zone();
        zone.setCode(code);
        zone.setName(name);
        zone.setDescription("Zone: " + name);
        zone.setCircle(circle);
        zone.setActive(true);
        return zoneRepository.save(zone);
    }

    private Area createArea(String code, String name, Zone zone) {
        Area area = new Area();
        area.setCode(code);
        area.setName(name);
        area.setDescription("Area: " + name);
        area.setZone(zone);
        area.setActive(true);
        return areaRepository.save(area);
    }

    private User createTestUser(String employeeId, String username, String fullName,
                                String email, String phone, String userType, String role,
                                Area area, String plainPassword, boolean active) {
        User user = new User();
        user.setEmployeeId(employeeId);
        user.setUsername(username);
        user.setFullName(fullName);
        
        // Encrypt email and phone
        user.setEmail(encryptionUtil.encrypt(email));
        if (phone != null) {
            user.setPhone(encryptionUtil.encrypt(phone));
        }
        
        // Hash password with BCrypt
        user.setPasswordHash(passwordEncoder.encode(plainPassword));
        
        user.setUserType(userType);
        user.setRole(role);
        user.setArea(area);
        user.setActive(active);
        user.setTwoFactorEnabled(false);

        User savedUser = userRepository.save(user);
        logUserAudit(savedUser, active);
        return savedUser;
    }

    private void logUserAudit(User user, boolean active) {
        if (user == null || user.getId() == null) {
            return;
        }
        java.util.Map<String, Object> newValues = new java.util.HashMap<>();
        newValues.put("employeeId", user.getEmployeeId());
        newValues.put("username", user.getUsername());
        newValues.put("fullName", user.getFullName());
        newValues.put("userType", user.getUserType());
        newValues.put("role", user.getRole());
        newValues.put("areaId", user.getArea() != null ? user.getArea().getId() : null);
        newValues.put("active", user.getActive());

        auditLogService.logActionForUser(user.getId(), "CREATE", "USER", user.getId(), null, newValues, null);
        if (Boolean.TRUE.equals(active)) {
            auditLogService.logActionForUser(user.getId(), "ACTIVATE", "USER", user.getId(), null, newValues, null);
        } else {
            auditLogService.logActionForUser(user.getId(), "DEACTIVATE", "USER", user.getId(), null, newValues, null);
        }
    }

    /**
     * Seed users only (useful when clusters exist but users don't)
     */
    @Transactional
    public void seedUsersOnly() {
        log.info("Starting user-only seeding...");
        
        // Check if users already exist
        if (userRepository.count() > 0) {
            log.info("Users already exist. Skipping user seeding.");
            return;
        }

        // Find or create required areas (we need at least one area for users)
        Area behrampurArea = areaRepository.findByCode("BHR")
                .orElseGet(() -> {
                    // If area doesn't exist, we need to create the hierarchy
                    log.warn("Required area 'BHR' not found. Creating minimal hierarchy...");
                    
                    // Find or create cluster
                    Cluster bupCluster = clusterRepository.findByCode("BUP")
                            .orElseGet(() -> createCluster("BUP", "Bihar UP"));
                    
                    // Find or create circle
                    Circle upCircle = circleRepository.findByCode("UP")
                            .orElseGet(() -> createCircle("UP", "Uttar Pradesh", bupCluster));
                    
                    // Find or create zone
                    Zone ghaziabadZone = zoneRepository.findByCode("GZB")
                            .orElseGet(() -> createZone("GZB", "Ghaziabad", upCircle));
                    
                    // Create area
                    return createArea("BHR", "Behrampur", ghaziabadZone);
                });

        Area meerutArea = areaRepository.findByCode("MRT")
                .orElseGet(() -> {
                    Zone ghaziabadZone = zoneRepository.findByCode("GZB")
                            .orElseGet(() -> {
                                Cluster bupCluster = clusterRepository.findByCode("BUP")
                                        .orElseGet(() -> createCluster("BUP", "Bihar UP"));
                                Circle upCircle = circleRepository.findByCode("UP")
                                        .orElseGet(() -> createCircle("UP", "Uttar Pradesh", bupCluster));
                                return createZone("GZB", "Ghaziabad", upCircle);
                            });
                    return createArea("MRT", "Meerut", ghaziabadZone);
                });

        // Create Users with Test Credentials
        createTestUser("EMP001", "shivam", "Shivam Kumar", "shivam@example.com", 
                "9876543210", "AREA_LEAD", "MANAGER", behrampurArea, "password123", true);
        
        createTestUser("EMP002", "rahul", "Rahul Sharma", "rahul@example.com", 
                "9876543211", "ZONE_LEAD", "MANAGER", behrampurArea, "password123", true);
        
        createTestUser("EMP003", "priya", "Priya Singh", "priya@example.com", 
                "9876543212", "CIRCLE_LEAD", "MANAGER", behrampurArea, "password123", true);
        
        createTestUser("EMP004", "admin", "Admin User", "admin@example.com", 
                "9876543213", "ADMIN", "ADMIN", behrampurArea, "admin123", true);
        
        createTestUser("EMP005", "analyst1", "Analyst One", "analyst1@example.com", 
                "9876543214", "ANALYST", "ANALYST", meerutArea, "password123", true);
        
        createTestUser("EMP006", "manager1", "Manager One", "manager1@example.com", 
                "9876543215", "ZONE_LEAD", "MANAGER", meerutArea, "password123", true);

        log.info("User seeding completed successfully!");
        log.info("Test Credentials:");
        log.info("  Employee ID: EMP001, Password: password123 (Area Lead)");
        log.info("  Employee ID: EMP002, Password: password123 (Zone Lead)");
        log.info("  Employee ID: EMP003, Password: password123 (Circle Lead)");
        log.info("  Employee ID: EMP004, Password: admin123 (Admin)");
        log.info("  Employee ID: EMP005, Password: password123 (Analyst)");
        log.info("  Employee ID: EMP006, Password: password123 (Zone Lead)");
    }
}
