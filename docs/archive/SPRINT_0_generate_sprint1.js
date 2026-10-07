const fs = require('fs');
const path = require('path');

function writeFile(relPath, content) {
  const fullPath = path.resolve(__dirname, relPath);
  fs.mkdirSync(path.dirname(fullPath), { recursive: true });
  fs.writeFileSync(fullPath, content.trim() + '\n', 'utf8');
  console.log('Created: ' + relPath);
}

// ==========================================
// 1. COMMON LIBRARY
// ==========================================

writeFile('services/common-library/src/main/java/com/rrjaggery/common/security/JwtTokenProvider.java', `
package com.rrjaggery.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtTokenProvider(
            @Value("\${app.jwt.secret:" + SecurityConstants.DEFAULT_JWT_SECRET + "}") String secret,
            @Value("\${app.jwt.expiration-ms:" + SecurityConstants.DEFAULT_EXPIRATION_MS + "}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String userId, String email, List<String> roles, String customerType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userId)
                .claim(SecurityConstants.CLAIM_EMAIL, email)
                .claim(SecurityConstants.CLAIM_ROLES, roles)
                .claim(SecurityConstants.CLAIM_CUSTOMER_TYPE, customerType)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public String getUserIdFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public String getEmailFromToken(String token) {
        return getClaims(token).get(SecurityConstants.CLAIM_EMAIL, String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        return getClaims(token).get(SecurityConstants.CLAIM_ROLES, List.class);
    }
}
`);

writeFile('services/common-library/src/main/java/com/rrjaggery/common/security/JwtAuthenticationFilter.java', `
package com.rrjaggery.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
            String email = tokenProvider.getEmailFromToken(token);
            List<String> roles = tokenProvider.getRolesFromToken(token);

            if (roles != null) {
                List<SimpleGrantedAuthority> authorities = roles.stream()
                        .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                        .map(SimpleGrantedAuthority::new)
                        .collect(Collectors.toList());

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(email, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(SecurityConstants.AUTH_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return bearerToken.substring(SecurityConstants.TOKEN_PREFIX.length());
        }
        return null;
    }
}
`);

// ==========================================
// 2. AUTH SERVICE
// ==========================================

writeFile('services/auth-service/pom.xml', `
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.rrjaggery</groupId>
        <artifactId>rr-jaggery-services</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>auth-service</artifactId>
    <packaging>jar</packaging>
    <name>Auth Service</name>
    <description>Authentication, JWT Tokens and Role-Based Access Control</description>

    <dependencies>
        <dependency>
            <groupId>com.rrjaggery</groupId>
            <artifactId>common-library</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
`);

writeFile('services/auth-service/src/main/resources/application.yml', `
server:
  port: \${AUTH_SERVICE_PORT:8081}

spring:
  application:
    name: auth-service
  datasource:
    url: \${DB_URL:jdbc:postgresql://\${DB_HOST:localhost}:\${DB_PORT:5432}/\${DB_NAME:rr_jaggery_db}?currentSchema=auth_schema}
    username: \${DB_USER:postgres}
    password: \${DB_PASSWORD:postgres_dev_password}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        default_schema: auth_schema
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: false
    show-sql: false

app:
  jwt:
    secret: \${JWT_SECRET:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}
    expiration-ms: \${JWT_EXPIRATION_MS:86400000}

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always
`);

writeFile('services/auth-service/src/test/resources/application.yml', `
server:
  port: 8081

spring:
  application:
    name: auth-service
  datasource:
    url: jdbc:h2:mem:auth_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
    driver-class-name: org.h2.Driver
    username: sa
    password: sa
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
`);

writeFile('services/auth-service/src/main/java/com/rrjaggery/auth/entity/Role.java', `
package com.rrjaggery.auth.entity;

public enum Role {
    ADMIN,
    CUSTOMER,
    PRODUCTION_MANAGER,
    EMPLOYEE
}
`);

writeFile('services/auth-service/src/main/java/com/rrjaggery/auth/entity/CustomerType.java', `
package com.rrjaggery.auth.entity;

public enum CustomerType {
    RETAIL,
    REGISTERED_WHOLESALE,
    INTERNAL
}
`);

writeFile('services/auth-service/src/main/java/com/rrjaggery/auth/entity/User.java', `
package com.rrjaggery.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users", schema = "auth_schema")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", length = 30)
    private CustomerType customerType = CustomerType.RETAIL;

    @Column(name = "business_name", length = 150)
    private String businessName;

    @Column(name = "gstin", length = 20)
    private String gstin;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", schema = "auth_schema", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 50)
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public User() {}

    public User(String email, String passwordHash, String fullName, String phone, CustomerType customerType) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.phone = phone;
        this.customerType = customerType;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public CustomerType getCustomerType() { return customerType; }
    public void setCustomerType(CustomerType customerType) { this.customerType = customerType; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }

    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
`);

writeFile('services/auth-service/src/main/java/com/rrjaggery/auth/dto/RegisterRequest.java', `
package com.rrjaggery.auth.dto;

import com.rrjaggery.auth.entity.CustomerType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName;

    private String phone;
    private CustomerType customerType = CustomerType.RETAIL;
    private String businessName;
    private String gstin;

    public RegisterRequest() {}

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public CustomerType getCustomerType() { return customerType; }
    public void setCustomerType(CustomerType customerType) { this.customerType = customerType; }

    public String getBusinessName() { return businessName; }
    public void setBusinessName(String businessName) { this.businessName = businessName; }

    public String getGstin() { return gstin; }
    public void setGstin(String gstin) { this.gstin = gstin; }
}
`);

writeFile('services/auth-service/src/main/java/com/rrjaggery/auth/dto/LoginRequest.java', `
package com.rrjaggery.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    public LoginRequest() {}

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
`);

writeFile('services/auth-service/src/main/java/com/rrjaggery/auth/dto/UserDto.java', `
package com.rrjaggery.auth.dto;

import com.rrjaggery.auth.entity.CustomerType;
import com.rrjaggery.auth.entity.Role;
import com.rrjaggery.auth.entity.User;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class UserDto {

    private UUID id;
    private String email;
    private String fullName;
    private String phone;
    private CustomerType customerType;
    private String businessName;
    private String gstin;
    private Set<String> roles;
    private boolean enabled;

    public UserDto() {}

    public static UserDto fromEntity(User user) {
        UserDto dto = new UserDto();
        dto.id = user.getId();
        dto.email = user.getEmail();
        dto.fullName = user.getFullName();
        dto.phone = user.getPhone();
        dto.customerType = user.getCustomerType();
        dto.businessName = user.getBusinessName();
        dto.gstin = user.getGstin();
        dto.roles = user.getRoles().stream().map(Role::name).collect(Collectors.toSet());
        dto.enabled = user.isEnabled();
        return dto;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public CustomerType getCustomerType() { return customerType; }
    public String getBusinessName() { return businessName; }
    public String getGstin() { return gstin; }
    public Set<String> getRoles() { return roles; }
    public boolean isEnabled() { return enabled; }
}
`);

writeFile('services/auth-service/src/main/java/com/rrjaggery/auth/dto/AuthResponse.java', `
package com.rrjaggery.auth.dto;

public class AuthResponse {

    private String token;
    private String tokenType = "Bearer";
    private long expiresInMs;
    private UserDto user;

    public AuthResponse() {}

    public AuthResponse(String token, long expiresInMs, UserDto user) {
        this.token = token;
        this.expiresInMs = expiresInMs;
        this.user = user;
    }

    public String getToken() { return token; }
    public String getTokenType() { return tokenType; }
    public long getExpiresInMs() { return expiresInMs; }
    public UserDto getUser() { return user; }
}
`);

writeFile('services/auth-service/src/main/java/com/rrjaggery/auth/repository/UserRepository.java', `
package com.rrjaggery.auth.repository;

import com.rrjaggery.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
}
`);

writeFile('services/auth-service/src/main/java/com/rrjaggery/auth/service/AuthService.java', `
package com.rrjaggery.auth.service;

import com.rrjaggery.auth.dto.AuthResponse;
import com.rrjaggery.auth.dto.LoginRequest;
import com.rrjaggery.auth.dto.RegisterRequest;
import com.rrjaggery.auth.dto.UserDto;
import com.rrjaggery.auth.entity.CustomerType;
import com.rrjaggery.auth.entity.Role;
import com.rrjaggery.auth.entity.User;
import com.rrjaggery.auth.repository.UserRepository;
import com.rrjaggery.common.security.JwtTokenProvider;
import com.rrjaggery.common.security.SecurityConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final long expirationMs;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider tokenProvider,
            @Value("\${app.jwt.expiration-ms:" + SecurityConstants.DEFAULT_EXPIRATION_MS + "}") long expirationMs) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.expirationMs = expirationMs;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new IllegalArgumentException("User with email already exists: " + request.getEmail());
        }

        User user = new User(
                request.getEmail().toLowerCase().trim(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName().trim(),
                request.getPhone(),
                request.getCustomerType() != null ? request.getCustomerType() : CustomerType.RETAIL
        );

        user.setBusinessName(request.getBusinessName());
        user.setGstin(request.getGstin());
        user.setRoles(Set.of(Role.CUSTOMER));

        User saved = userRepository.save(user);
        return generateAuthResponse(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new IllegalStateException("Account is disabled. Please contact support.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        return generateAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUser(String email) {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
        return UserDto.fromEntity(user);
    }

    private AuthResponse generateAuthResponse(User user) {
        List<String> roleNames = user.getRoles().stream()
                .map(r -> "ROLE_" + r.name())
                .collect(Collectors.toList());

        String token = tokenProvider.generateToken(
                user.getId().toString(),
                user.getEmail(),
                roleNames,
                user.getCustomerType() != null ? user.getCustomerType().name() : CustomerType.RETAIL.name()
        );

        return new AuthResponse(token, expirationMs, UserDto.fromEntity(user));
    }
}
`);

writeFile('services/auth-service/src/main/java/com/rrjaggery/auth/controller/AuthController.java', `
package com.rrjaggery.auth.controller;

import com.rrjaggery.auth.dto.AuthResponse;
import com.rrjaggery.auth.dto.LoginRequest;
import com.rrjaggery.auth.dto.RegisterRequest;
import com.rrjaggery.auth.dto.UserDto;
import com.rrjaggery.auth.service.AuthService;
import com.rrjaggery.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response, "User registered successfully"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response, "Login successful"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getMe(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Unauthenticated", "AUTH_REQUIRED"));
        }
        UserDto userDto = authService.getCurrentUser(authentication.getName());
        return ResponseEntity.ok(ApiResponse.ok(userDto));
    }
}
`);

writeFile('services/auth-service/src/main/java/com/rrjaggery/auth/config/SecurityConfig.java', `
package com.rrjaggery.auth.config;

import com.rrjaggery.common.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/health",
                                "/api/v1/auth/login",
                                "/api/v1/auth/register",
                                "/actuator/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
`);

writeFile('services/auth-service/src/main/java/com/rrjaggery/auth/config/DataInitializer.java', `
package com.rrjaggery.auth.config;

import com.rrjaggery.auth.entity.CustomerType;
import com.rrjaggery.auth.entity.Role;
import com.rrjaggery.auth.entity.User;
import com.rrjaggery.auth.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Seed default Admin
        if (!userRepository.existsByEmail("admin@rrjaggery.com")) {
            User admin = new User(
                    "admin@rrjaggery.com",
                    passwordEncoder.encode("Admin@123"),
                    "RR Master Admin",
                    "+91 98765 43210",
                    CustomerType.INTERNAL
            );
            admin.setRoles(Set.of(Role.ADMIN, Role.CUSTOMER));
            userRepository.save(admin);
            System.out.println("Seeded Default Admin: admin@rrjaggery.com / Admin@123");
        }

        // Seed demo Retail Customer
        if (!userRepository.existsByEmail("retail@example.com")) {
            User retail = new User(
                    "retail@example.com",
                    passwordEncoder.encode("Retail@123"),
                    "Rohan Sharma",
                    "+91 91234 56789",
                    CustomerType.RETAIL
            );
            retail.setRoles(Set.of(Role.CUSTOMER));
            userRepository.save(retail);
            System.out.println("Seeded Demo Retail Customer: retail@example.com / Retail@123");
        }

        // Seed demo Wholesale Customer
        if (!userRepository.existsByEmail("wholesale@example.com")) {
            User wholesale = new User(
                    "wholesale@example.com",
                    passwordEncoder.encode("Wholesale@123"),
                    "Karnataka Sweet Mart",
                    "+91 98450 12345",
                    CustomerType.REGISTERED_WHOLESALE
            );
            wholesale.setBusinessName("Karnataka Sweet Mart Pvt Ltd");
            wholesale.setGstin("29AAAAA0000A1Z5");
            wholesale.setRoles(Set.of(Role.CUSTOMER));
            userRepository.save(wholesale);
            System.out.println("Seeded Demo Wholesale Customer: wholesale@example.com / Wholesale@123");
        }
    }
}
`);

writeFile('services/auth-service/src/test/java/com/rrjaggery/auth/controller/AuthControllerTest.java', `
package com.rrjaggery.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rrjaggery.auth.dto.LoginRequest;
import com.rrjaggery.auth.dto.RegisterRequest;
import com.rrjaggery.auth.entity.CustomerType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/auth/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void testAdminLoginSuccess() throws Exception {
        LoginRequest req = new LoginRequest("admin@rrjaggery.com", "Admin@123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.user.email").value("admin@rrjaggery.com"));
    }

    @Test
    void testInvalidLoginFailure() throws Exception {
        LoginRequest req = new LoginRequest("admin@rrjaggery.com", "WrongPassword");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void testRegisterNewCustomer() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("newuser" + System.currentTimeMillis() + "@example.com");
        req.setPassword("Password@123");
        req.setFullName("Test User");
        req.setPhone("+91 99999 88888");
        req.setCustomerType(CustomerType.RETAIL);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists());
    }
}
`);

// ==========================================
// 3. COMMERCE SERVICE
// ==========================================

writeFile('services/commerce-service/pom.xml', `
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.rrjaggery</groupId>
        <artifactId>rr-jaggery-services</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>commerce-service</artifactId>
    <packaging>jar</packaging>
    <name>Commerce Service</name>
    <description>Product Catalogue, Categories, and Orders</description>

    <dependencies>
        <dependency>
            <groupId>com.rrjaggery</groupId>
            <artifactId>common-library</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
`);

writeFile('services/commerce-service/src/main/resources/application.yml', `
server:
  port: \${COMMERCE_SERVICE_PORT:8082}

spring:
  application:
    name: commerce-service
  datasource:
    url: \${DB_URL:jdbc:postgresql://\${DB_HOST:localhost}:\${DB_PORT:5432}/\${DB_NAME:rr_jaggery_db}?currentSchema=commerce_schema}
    username: \${DB_USER:postgres}
    password: \${DB_PASSWORD:postgres_dev_password}
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        default_schema: commerce_schema
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: false
    show-sql: false

app:
  jwt:
    secret: \${JWT_SECRET:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}
    expiration-ms: \${JWT_EXPIRATION_MS:86400000}

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always
`);

writeFile('services/commerce-service/src/test/resources/application.yml', `
server:
  port: 8082

spring:
  application:
    name: commerce-service
  datasource:
    url: jdbc:h2:mem:commerce_db;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
    driver-class-name: org.h2.Driver
    username: sa
    password: sa
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate:
        dialect: org.hibernate.dialect.H2Dialect
`);

writeFile('services/commerce-service/src/main/java/com/rrjaggery/commerce/entity/ProductGrade.java', `
package com.rrjaggery.commerce.entity;

public enum ProductGrade {
    GRADE_A_TRADITIONAL,
    PREMIUM_POWDER,
    EXPORT_CUBES,
    ORGANIC_LIQUID,
    COMMERCIAL_BULK
}
`);

writeFile('services/commerce-service/src/main/java/com/rrjaggery/commerce/entity/PackageType.java', `
package com.rrjaggery.commerce.entity;

public enum PackageType {
    BOX,
    POUCH,
    JAR,
    BAG,
    BUCKET,
    TIN
}
`);

writeFile('services/commerce-service/src/main/java/com/rrjaggery/commerce/entity/Category.java', `
package com.rrjaggery.commerce.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "categories", schema = "commerce_schema")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(length = 500)
    private String description;

    @Column(name = "display_order", nullable = false)
    private int displayOrder = 0;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Category() {}

    public Category(String name, String slug, String description, int displayOrder) {
        this.name = name;
        this.slug = slug;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
`);

writeFile('services/commerce-service/src/main/java/com/rrjaggery/commerce/entity/Product.java', `
package com.rrjaggery.commerce.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "products", schema = "commerce_schema")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, unique = true, length = 180)
    private String slug;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ProductGrade grade = ProductGrade.GRADE_A_TRADITIONAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "package_type", nullable = false, length = 30)
    private PackageType packageType = PackageType.BOX;

    @Column(name = "unit_weight_kg", nullable = false, precision = 10, scale = 3)
    private BigDecimal unitWeightKg = BigDecimal.ONE;

    @Column(name = "retail_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal retailPrice;

    @Column(name = "wholesale_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal wholesalePrice;

    @Column(name = "wholesale_moq", nullable = false)
    private int wholesaleMoq = 10;

    @Column(name = "image_url", length = 300)
    private String imageUrl;

    @Column(nullable = false)
    private boolean featured = false;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Product() {}

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ProductGrade getGrade() { return grade; }
    public void setGrade(ProductGrade grade) { this.grade = grade; }

    public PackageType getPackageType() { return packageType; }
    public void setPackageType(PackageType packageType) { this.packageType = packageType; }

    public BigDecimal getUnitWeightKg() { return unitWeightKg; }
    public void setUnitWeightKg(BigDecimal unitWeightKg) { this.unitWeightKg = unitWeightKg; }

    public BigDecimal getRetailPrice() { return retailPrice; }
    public void setRetailPrice(BigDecimal retailPrice) { this.retailPrice = retailPrice; }

    public BigDecimal getWholesalePrice() { return wholesalePrice; }
    public void setWholesalePrice(BigDecimal wholesalePrice) { this.wholesalePrice = wholesalePrice; }

    public int getWholesaleMoq() { return wholesaleMoq; }
    public void setWholesaleMoq(int wholesaleMoq) { this.wholesaleMoq = wholesaleMoq; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
`);

writeFile('services/commerce-service/src/main/java/com/rrjaggery/commerce/dto/CategoryDto.java', `
package com.rrjaggery.commerce.dto;

import com.rrjaggery.commerce.entity.Category;
import java.util.UUID;

public class CategoryDto {
    private UUID id;
    private String name;
    private String slug;
    private String description;
    private int displayOrder;
    private boolean active;

    public CategoryDto() {}

    public static CategoryDto fromEntity(Category category) {
        CategoryDto dto = new CategoryDto();
        dto.id = category.getId();
        dto.name = category.getName();
        dto.slug = category.getSlug();
        dto.description = category.getDescription();
        dto.displayOrder = category.getDisplayOrder();
        dto.active = category.isActive();
        return dto;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getDescription() { return description; }
    public int getDisplayOrder() { return displayOrder; }
    public boolean isActive() { return active; }
}
`);

writeFile('services/commerce-service/src/main/java/com/rrjaggery/commerce/dto/CreateCategoryRequest.java', `
package com.rrjaggery.commerce.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateCategoryRequest {

    @NotBlank(message = "Category name is required")
    private String name;

    private String slug;
    private String description;
    private int displayOrder = 0;

    public CreateCategoryRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
}
`);

writeFile('services/commerce-service/src/main/java/com/rrjaggery/commerce/dto/ProductDto.java', `
package com.rrjaggery.commerce.dto;

import com.rrjaggery.commerce.entity.PackageType;
import com.rrjaggery.commerce.entity.Product;
import com.rrjaggery.commerce.entity.ProductGrade;

import java.math.BigDecimal;
import java.util.UUID;

public class ProductDto {

    private UUID id;
    private UUID categoryId;
    private String categoryName;
    private String name;
    private String sku;
    private String slug;
    private String description;
    private ProductGrade grade;
    private PackageType packageType;
    private BigDecimal unitWeightKg;
    private BigDecimal retailPrice;
    private BigDecimal wholesalePrice;
    private int wholesaleMoq;
    private String imageUrl;
    private boolean featured;
    private boolean active;

    public ProductDto() {}

    public static ProductDto fromEntity(Product product, String categoryName) {
        ProductDto dto = new ProductDto();
        dto.id = product.getId();
        dto.categoryId = product.getCategoryId();
        dto.categoryName = categoryName;
        dto.name = product.getName();
        dto.sku = product.getSku();
        dto.slug = product.getSlug();
        dto.description = product.getDescription();
        dto.grade = product.getGrade();
        dto.packageType = product.getPackageType();
        dto.unitWeightKg = product.getUnitWeightKg();
        dto.retailPrice = product.getRetailPrice();
        dto.wholesalePrice = product.getWholesalePrice();
        dto.wholesaleMoq = product.getWholesaleMoq();
        dto.imageUrl = product.getImageUrl();
        dto.featured = product.isFeatured();
        dto.active = product.isActive();
        return dto;
    }

    public UUID getId() { return id; }
    public UUID getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public String getName() { return name; }
    public String getSku() { return sku; }
    public String getSlug() { return slug; }
    public String getDescription() { return description; }
    public ProductGrade getGrade() { return grade; }
    public PackageType getPackageType() { return packageType; }
    public BigDecimal getUnitWeightKg() { return unitWeightKg; }
    public BigDecimal getRetailPrice() { return retailPrice; }
    public BigDecimal getWholesalePrice() { return wholesalePrice; }
    public int getWholesaleMoq() { return wholesaleMoq; }
    public String getImageUrl() { return imageUrl; }
    public boolean featured() { return featured; }
    public boolean isActive() { return active; }
}
`);

writeFile('services/commerce-service/src/main/java/com/rrjaggery/commerce/dto/CreateProductRequest.java', `
package com.rrjaggery.commerce.dto;

import com.rrjaggery.commerce.entity.PackageType;
import com.rrjaggery.commerce.entity.ProductGrade;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class CreateProductRequest {

    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    @NotBlank(message = "Product name is required")
    private String name;

    @NotBlank(message = "SKU is required")
    private String sku;

    private String slug;
    private String description;

    @NotNull(message = "Grade is required")
    private ProductGrade grade = ProductGrade.GRADE_A_TRADITIONAL;

    @NotNull(message = "Package type is required")
    private PackageType packageType = PackageType.BOX;

    @NotNull(message = "Unit weight is required")
    @DecimalMin(value = "0.01", message = "Weight must be positive")
    private BigDecimal unitWeightKg = BigDecimal.ONE;

    @NotNull(message = "Retail price is required")
    @DecimalMin(value = "0.01", message = "Retail price must be positive")
    private BigDecimal retailPrice;

    @NotNull(message = "Wholesale price is required")
    @DecimalMin(value = "0.01", message = "Wholesale price must be positive")
    private BigDecimal wholesalePrice;

    @Min(value = 1, message = "Wholesale MOQ must be at least 1")
    private int wholesaleMoq = 10;

    private String imageUrl;
    private boolean featured = false;
    private boolean active = true;

    public CreateProductRequest() {}

    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public ProductGrade getGrade() { return grade; }
    public void setGrade(ProductGrade grade) { this.grade = grade; }

    public PackageType getPackageType() { return packageType; }
    public void setPackageType(PackageType packageType) { this.packageType = packageType; }

    public BigDecimal getUnitWeightKg() { return unitWeightKg; }
    public void setUnitWeightKg(BigDecimal unitWeightKg) { this.unitWeightKg = unitWeightKg; }

    public BigDecimal getRetailPrice() { return retailPrice; }
    public void setRetailPrice(BigDecimal retailPrice) { this.retailPrice = retailPrice; }

    public BigDecimal getWholesalePrice() { return wholesalePrice; }
    public void setWholesalePrice(BigDecimal wholesalePrice) { this.wholesalePrice = wholesalePrice; }

    public int getWholesaleMoq() { return wholesaleMoq; }
    public void setWholesaleMoq(int wholesaleMoq) { this.wholesaleMoq = wholesaleMoq; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
`);

writeFile('services/commerce-service/src/main/java/com/rrjaggery/commerce/repository/CategoryRepository.java', `
package com.rrjaggery.commerce.repository;

import com.rrjaggery.commerce.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    Optional<Category> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Category> findByActiveTrueOrderByDisplayOrderAsc();
}
`);

writeFile('services/commerce-service/src/main/java/com/rrjaggery/commerce/repository/ProductRepository.java', `
package com.rrjaggery.commerce.repository;

import com.rrjaggery.commerce.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findBySlug(String slug);
    Optional<Product> findBySku(String sku);
    boolean existsBySku(String sku);
    boolean existsBySlug(String slug);

    List<Product> findByActiveTrue();
    List<Product> findByCategoryIdAndActiveTrue(UUID categoryId);

    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Product> searchActiveProducts(@Param("query") String query);
}
`);

writeFile('services/commerce-service/src/main/java/com/rrjaggery/commerce/service/CategoryService.java', `
package com.rrjaggery.commerce.service;

import com.rrjaggery.commerce.dto.CategoryDto;
import com.rrjaggery.commerce.dto.CreateCategoryRequest;
import com.rrjaggery.commerce.entity.Category;
import com.rrjaggery.commerce.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private static final Pattern NONLATIN = Pattern.compile("[^\\\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\\\s]");

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAsc().stream()
                .map(CategoryDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CategoryDto getById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
        return CategoryDto.fromEntity(category);
    }

    @Transactional
    public CategoryDto createCategory(CreateCategoryRequest request) {
        String slug = request.getSlug() != null && !request.getSlug().isBlank()
                ? toSlug(request.getSlug())
                : toSlug(request.getName());

        if (categoryRepository.existsBySlug(slug)) {
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 6);
        }

        Category category = new Category(request.getName(), slug, request.getDescription(), request.getDisplayOrder());
        Category saved = categoryRepository.save(category);
        return CategoryDto.fromEntity(saved);
    }

    @Transactional
    public CategoryDto updateCategory(UUID id, CreateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setDisplayOrder(request.getDisplayOrder());

        Category updated = categoryRepository.save(category);
        return CategoryDto.fromEntity(updated);
    }

    @Transactional
    public void deleteCategory(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
        category.setActive(false);
        categoryRepository.save(category);
    }

    private String toSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}
`);

writeFile('services/commerce-service/src/main/java/com/rrjaggery/commerce/service/ProductService.java', `
package com.rrjaggery.commerce.service;

import com.rrjaggery.commerce.dto.CreateProductRequest;
import com.rrjaggery.commerce.dto.ProductDto;
import com.rrjaggery.commerce.entity.Category;
import com.rrjaggery.commerce.entity.Product;
import com.rrjaggery.commerce.repository.CategoryRepository;
import com.rrjaggery.commerce.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    private static final Pattern NONLATIN = Pattern.compile("[^\\\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\\\s]");

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getActiveProducts(UUID categoryId, String query) {
        Map<UUID, String> categoryNames = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));

        List<Product> products;
        if (query != null && !query.isBlank()) {
            products = productRepository.searchActiveProducts(query.trim());
        } else if (categoryId != null) {
            products = productRepository.findByCategoryIdAndActiveTrue(categoryId);
        } else {
            products = productRepository.findByActiveTrue();
        }

        return products.stream()
                .map(p -> ProductDto.fromEntity(p, categoryNames.getOrDefault(p.getCategoryId(), "General")))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getAllProductsAdmin() {
        Map<UUID, String> categoryNames = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getId, Category::getName, (a, b) -> a));

        return productRepository.findAll().stream()
                .map(p -> ProductDto.fromEntity(p, categoryNames.getOrDefault(p.getCategoryId(), "General")))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProductDto getById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        String catName = categoryRepository.findById(product.getCategoryId())
                .map(Category::getName).orElse("General");
        return ProductDto.fromEntity(product, catName);
    }

    @Transactional(readOnly = true)
    public ProductDto getBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with slug: " + slug));
        String catName = categoryRepository.findById(product.getCategoryId())
                .map(Category::getName).orElse("General");
        return ProductDto.fromEntity(product, catName);
    }

    @Transactional
    public ProductDto createProduct(CreateProductRequest request) {
        if (productRepository.existsBySku(request.getSku().toUpperCase().trim())) {
            throw new IllegalArgumentException("Product SKU already exists: " + request.getSku());
        }

        String slug = request.getSlug() != null && !request.getSlug().isBlank()
                ? toSlug(request.getSlug())
                : toSlug(request.getName());

        if (productRepository.existsBySlug(slug)) {
            slug = slug + "-" + UUID.randomUUID().toString().substring(0, 6);
        }

        if (!categoryRepository.existsById(request.getCategoryId())) {
            throw new IllegalArgumentException("Invalid category ID: " + request.getCategoryId());
        }

        Product product = new Product();
        product.setCategoryId(request.getCategoryId());
        product.setName(request.getName().trim());
        product.setSku(request.getSku().toUpperCase().trim());
        product.setSlug(slug);
        product.setDescription(request.getDescription());
        product.setGrade(request.getGrade());
        product.setPackageType(request.getPackageType());
        product.setUnitWeightKg(request.getUnitWeightKg());
        product.setRetailPrice(request.getRetailPrice());
        product.setWholesalePrice(request.getWholesalePrice());
        product.setWholesaleMoq(request.getWholesaleMoq());
        product.setImageUrl(request.getImageUrl());
        product.setFeatured(request.isFeatured());
        product.setActive(request.isActive());

        Product saved = productRepository.save(product);
        String catName = categoryRepository.findById(saved.getCategoryId()).map(Category::getName).orElse("General");
        return ProductDto.fromEntity(saved, catName);
    }

    @Transactional
    public ProductDto updateProduct(UUID id, CreateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        product.setCategoryId(request.getCategoryId());
        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setGrade(request.getGrade());
        product.setPackageType(request.getPackageType());
        product.setUnitWeightKg(request.getUnitWeightKg());
        product.setRetailPrice(request.getRetailPrice());
        product.setWholesalePrice(request.getWholesalePrice());
        product.setWholesaleMoq(request.getWholesaleMoq());
        product.setImageUrl(request.getImageUrl());
        product.setFeatured(request.isFeatured());
        product.setActive(request.isActive());

        Product updated = productRepository.save(product);
        String catName = categoryRepository.findById(updated.getCategoryId()).map(Category::getName).orElse("General");
        return ProductDto.fromEntity(updated, catName);
    }

    @Transactional
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
        product.setActive(false);
        productRepository.save(product);
    }

    private String toSlug(String input) {
        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}
`);

writeFile('services/commerce-service/src/main/java/com/rrjaggery/commerce/controller/PublicCatalogueController.java', `
package com.rrjaggery.commerce.controller;

import com.rrjaggery.commerce.dto.CategoryDto;
import com.rrjaggery.commerce.dto.ProductDto;
import com.rrjaggery.commerce.service.CategoryService;
import com.rrjaggery.commerce.service.ProductService;
import com.rrjaggery.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/commerce")
public class PublicCatalogueController {

    private final CategoryService categoryService;
    private final ProductService productService;

    public PublicCatalogueController(CategoryService categoryService, ProductService productService) {
        this.categoryService = categoryService;
        this.productService = productService;
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getActiveCategories()));
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getProducts(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String query) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getActiveProducts(categoryId, query)));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> getProductById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getById(id)));
    }

    @GetMapping("/products/slug/{slug}")
    public ResponseEntity<ApiResponse<ProductDto>> getProductBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getBySlug(slug)));
    }
}
`);

writeFile('services/commerce-service/src/main/java/com/rrjaggery/commerce/controller/AdminCatalogueController.java', `
package com.rrjaggery.commerce.controller;

import com.rrjaggery.commerce.dto.CategoryDto;
import com.rrjaggery.commerce.dto.CreateCategoryRequest;
import com.rrjaggery.commerce.dto.CreateProductRequest;
import com.rrjaggery.commerce.dto.ProductDto;
import com.rrjaggery.commerce.service.CategoryService;
import com.rrjaggery.commerce.service.ProductService;
import com.rrjaggery.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v1/commerce/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCatalogueController {

    private final CategoryService categoryService;
    private final ProductService productService;

    public AdminCatalogueController(CategoryService categoryService, ProductService productService) {
        this.categoryService = categoryService;
        this.productService = productService;
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getAllCategories()));
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<CategoryDto>> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryDto dto = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(dto, "Category created successfully"));
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryDto>> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCategoryRequest request) {
        CategoryDto dto = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.ok(dto, "Category updated successfully"));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Category deactivated successfully"));
    }

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductDto>>> getAllProducts() {
        return ResponseEntity.ok(ApiResponse.ok(productService.getAllProductsAdmin()));
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductDto>> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductDto dto = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(dto, "Product created successfully"));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductDto>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody CreateProductRequest request) {
        ProductDto dto = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.ok(dto, "Product updated successfully"));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.ok(null, "Product deactivated successfully"));
    }
}
`);

writeFile('services/commerce-service/src/main/java/com/rrjaggery/commerce/config/SecurityConfig.java', `
package com.rrjaggery.commerce.config;

import com.rrjaggery.common.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/commerce/health",
                                "/api/v1/commerce/categories/**",
                                "/api/v1/commerce/products/**",
                                "/actuator/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**"
                        ).permitAll()
                        .requestMatchers("/api/v1/commerce/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
`);

writeFile('services/commerce-service/src/main/java/com/rrjaggery/commerce/config/CommerceDataInitializer.java', `
package com.rrjaggery.commerce.config;

import com.rrjaggery.commerce.entity.Category;
import com.rrjaggery.commerce.entity.PackageType;
import com.rrjaggery.commerce.entity.Product;
import com.rrjaggery.commerce.entity.ProductGrade;
import com.rrjaggery.commerce.repository.CategoryRepository;
import com.rrjaggery.commerce.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class CommerceDataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CommerceDataInitializer(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Override
    public void run(String... args) {
        if (categoryRepository.count() == 0) {
            Category blocks = categoryRepository.save(new Category("Traditional Jaggery Blocks", "traditional-blocks", "Pure organic sugarcane solid blocks crafted with heritage clarification", 1));
            Category powder = categoryRepository.save(new Category("Organic Jaggery Powder", "jaggery-powder", "Fine crystal granulated chemical-free unrefined sugar substitute", 2));
            Category cubes = categoryRepository.save(new Category("Artisanal Jaggery Cubes", "jaggery-cubes", "Uniform moulded tea/coffee sweetener cubes for instant dissolving", 3));
            Category liquid = categoryRepository.save(new Category("Pure Liquid Jaggery (JONNA)", "liquid-jaggery", "Rich iron syrup extract ideal for Ayurvedic preparations", 4));

            System.out.println("Seeded Jaggery Categories.");

            // Product 1: 10KG Traditional Block
            Product p1 = new Product();
            p1.setCategoryId(blocks.getId());
            p1.setName("Mandya Organic Traditional Block Jaggery");
            p1.setSku("RR-BLK-10KG");
            p1.setSlug("mandya-organic-traditional-block-jaggery-10kg");
            p1.setDescription("Authentic solid jaggery block produced from high-sucrose Mandya sugarcane. 100% natural, free from chemical clarifiers or bleaching agents.");
            p1.setGrade(ProductGrade.GRADE_A_TRADITIONAL);
            p1.setPackageType(PackageType.BOX);
            p1.setUnitWeightKg(new BigDecimal("10.000"));
            p1.setRetailPrice(new BigDecimal("550.00"));
            p1.setWholesalePrice(new BigDecimal("420.00"));
            p1.setWholesaleMoq(20);
            p1.setImageUrl("https://images.unsplash.com/photo-1599940824399-b87987ceb72a?auto=format&fit=crop&w=600&q=80");
            p1.setFeatured(true);
            p1.setActive(true);
            productRepository.save(p1);

            // Product 2: 1KG Powder Pouch
            Product p2 = new Product();
            p2.setCategoryId(powder.getId());
            p2.setName("Sulphur-Free Granular Jaggery Powder");
            p2.setSku("RR-POW-01KG");
            p2.setSlug("sulphur-free-granular-jaggery-powder-1kg");
            p2.setDescription("Premium quality pulverized jaggery granules, easy to measure for daily cooking, tea, and confectionery. Retains natural minerals.");
            p2.setGrade(ProductGrade.PREMIUM_POWDER);
            p2.setPackageType(PackageType.POUCH);
            p2.setUnitWeightKg(new BigDecimal("1.000"));
            p2.setRetailPrice(new BigDecimal("85.00"));
            p2.setWholesalePrice(new BigDecimal("62.00"));
            p2.setWholesaleMoq(50);
            p2.setImageUrl("https://images.unsplash.com/photo-1589301760014-d929f3979dbc?auto=format&fit=crop&w=600&q=80");
            p2.setFeatured(true);
            p2.setActive(true);
            productRepository.save(p2);

            // Product 3: 500g Cubes Jar
            Product p3 = new Product();
            p3.setCategoryId(cubes.getId());
            p3.setName("Artisanal Sugarcane Moulded Cubes");
            p3.setSku("RR-CUB-500G");
            p3.setSlug("artisanal-sugarcane-moulded-cubes-500g");
            p3.setDescription("Export grade uniform bite-sized cubes, crafted for clean handling and exact portion sweetening for tea, coffee, and desserts.");
            p3.setGrade(ProductGrade.EXPORT_CUBES);
            p3.setPackageType(PackageType.JAR);
            p3.setUnitWeightKg(new BigDecimal("0.500"));
            p3.setRetailPrice(new BigDecimal("65.00"));
            p3.setWholesalePrice(new BigDecimal("48.00"));
            p3.setWholesaleMoq(30);
            p3.setImageUrl("https://images.unsplash.com/photo-1606787366850-de6330128bfc?auto=format&fit=crop&w=600&q=80");
            p3.setFeatured(true);
            p3.setActive(true);
            productRepository.save(p3);

            // Product 4: 5KG Bulk Bucket
            Product p4 = new Product();
            p4.setCategoryId(liquid.getId());
            p4.setName("Pure Ayurvedic Jonnaguda Liquid Jaggery Syrup");
            p4.setSku("RR-LIQ-05KG");
            p4.setSlug("pure-ayurvedic-jonnaguda-liquid-jaggery-syrup-5kg");
            p4.setDescription("Thick concentrated sugarcane nectar rich in iron, zinc, and magnesium. Widely used in traditional recipes and Ayurvedic tonics.");
            p4.setGrade(ProductGrade.ORGANIC_LIQUID);
            p4.setPackageType(PackageType.BUCKET);
            p4.setUnitWeightKg(new BigDecimal("5.000"));
            p4.setRetailPrice(new BigDecimal("380.00"));
            p4.setWholesalePrice(new BigDecimal("290.00"));
            p4.setWholesaleMoq(15);
            p4.setImageUrl("https://images.unsplash.com/photo-1546069901-ba9599a7e63c?auto=format&fit=crop&w=600&q=80");
            p4.setFeatured(false);
            p4.setActive(true);
            productRepository.save(p4);

            System.out.println("Seeded Mandya Jaggery Products.");
        }
    }
}
`);

writeFile('services/commerce-service/src/test/java/com/rrjaggery/commerce/controller/PublicCatalogueControllerTest.java', `
package com.rrjaggery.commerce.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PublicCatalogueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetCategoriesPublic() throws Exception {
        mockMvc.perform(get("/api/v1/commerce/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testGetProductsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/commerce/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void testUnauthorizedAdminEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/commerce/admin/products"))
                .andExpect(status().isForbidden());
    }
}
`);

console.log('Sprint 1 backend files generated successfully!');
