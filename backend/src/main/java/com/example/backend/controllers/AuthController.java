package com.example.backend.controllers;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.example.backend.entities.Account;
import com.example.backend.enums.AccountStatus;
import com.example.backend.exceptions.UsernameAlreadyExistsException;
import com.example.backend.services.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.example.backend.enums.ERole;
import com.example.backend.entities.Role;
import com.example.backend.entities.UserEntity;
import com.example.backend.exceptions.InsufficientBalanceException;
import com.example.backend.repositories.RoleRepository;
import com.example.backend.repositories.UserRepository;
import com.example.backend.security.jwt.JwtUtils;
import com.example.backend.security.payload.request.LoginRequest;
import com.example.backend.security.payload.request.SignupRequest;
import com.example.backend.security.payload.response.JwtResponse;
import com.example.backend.security.payload.response.SignupResponse;
import com.example.backend.security.service.UserDetailsImpl;

import jakarta.validation.Valid;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder encoder;
    private final JwtUtils jwtUtils;
    private final AccountService accountService;
    private static final String ROLE_NOT_FOUND_ERROR = "Error: Role is not found.";

    public AuthController(AuthenticationManager authenticationManager,
                          UserRepository userRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder encoder,
                          JwtUtils jwtUtils,
                          AccountService accountService) {

        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.encoder = encoder;
        this.jwtUtils = jwtUtils;
        this.accountService = accountService;
    }

    @GetMapping("/check-username")
    public ResponseEntity<Boolean> checkUsernameAvailability(@RequestParam String username) {
        boolean isAvailable = !userRepository.existsByUsername(username);
        return ResponseEntity.ok(isAvailable);
    }

    @PostMapping("/signin")
    public ResponseEntity<JwtResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(),
                        loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // Generate a fresh sessionId — this silently kills any existing session
        String sessionId = UUID.randomUUID().toString();

        // findByUsername returns UserEntity directly (not Optional) in this repo
        UserEntity user = userRepository.findByUsername(userDetails.getUsername());
        user.setSessionId(sessionId);
        userRepository.save(user);

        String jwt = jwtUtils.generateJwtToken(authentication, sessionId);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return ResponseEntity.ok(new JwtResponse(jwt,
                userDetails.getId(),
                userDetails.getUsername(),
                roles));
    }

    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> logout() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        UserEntity user = userRepository.findByUsername(username);
        user.setSessionId(null);
        userRepository.save(user);
        return ResponseEntity.ok("Logged out successfully.");
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {

        if (Boolean.TRUE.equals(userRepository.existsByUsername(signUpRequest.getUsername()))) {
            throw new UsernameAlreadyExistsException("Error: Username is already taken!");
        }

        if (signUpRequest.getMinBalance() == null || signUpRequest.getMinBalance() < 1000.0) {
            throw new InsufficientBalanceException("Minimum balance must be at least 1000");
        }

        UserEntity user = new UserEntity(
                signUpRequest.getUsername(),
                encoder.encode(signUpRequest.getPassword())
        );

        Account account = new Account();
        account.setBalance(signUpRequest.getMinBalance());
        account.setHolderName(signUpRequest.getHolderName());
        account.setStatus(AccountStatus.ACTIVE);
        account.setVersion(1);
        account.setAccountId(accountService.generateAccountId());
        account.setLastUpdated(LocalDateTime.now());

        user.setAccount(account);

        Set<String> strRoles = signUpRequest.getRole();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            Role userRole = roleRepository.findByRoleName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException(ROLE_NOT_FOUND_ERROR));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                if (role.equals("admin")) {
                    Role adminRole = roleRepository.findByRoleName(ERole.ROLE_ADMIN)
                            .orElseThrow(() -> new RuntimeException(ROLE_NOT_FOUND_ERROR));
                    roles.add(adminRole);
                } else {
                    Role userRole = roleRepository.findByRoleName(ERole.ROLE_USER)
                            .orElseThrow(() -> new RuntimeException(ROLE_NOT_FOUND_ERROR));
                    roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);
        userRepository.save(user);

        return ResponseEntity.ok(
                new SignupResponse(
                        user.getId(),
                        user.getUsername(),
                        user.getAccount().getAccountId(),
                        user.getAccount().getHolderName(),
                        user.getAccount().getBalance()
                )
        );
    }
}
