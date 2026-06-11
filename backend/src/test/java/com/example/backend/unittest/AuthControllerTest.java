package com.example.backend.unittest;

import com.example.backend.controllers.AuthController;
import com.example.backend.entities.Role;
import com.example.backend.entities.UserEntity;
import com.example.backend.enums.ERole;
import com.example.backend.exceptions.InsufficientBalanceException;
import com.example.backend.exceptions.UsernameAlreadyExistsException;
import com.example.backend.repositories.RoleRepository;
import com.example.backend.repositories.UserRepository;
import com.example.backend.security.jwt.JwtUtils;
import com.example.backend.security.payload.request.LoginRequest;
import com.example.backend.security.payload.request.SignupRequest;
import com.example.backend.security.payload.response.JwtResponse;
import com.example.backend.security.payload.response.SignupResponse;
import com.example.backend.security.service.UserDetailsImpl;
import com.example.backend.services.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    @InjectMocks
    private AuthController authController;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder encoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private AccountService accountService;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testAuthenticateUser_success() {
        LoginRequest loginRequest = new LoginRequest("username", "password");

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtUtils.generateJwtToken(authentication)).thenReturn("mock-jwt-token");
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("username");
        when(userDetails.getId()).thenReturn(1L);
        when(userDetails.getAuthorities()).thenReturn(new HashSet<>());

        ResponseEntity<JwtResponse> response = authController.authenticateUser(loginRequest);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("mock-jwt-token", response.getBody().getToken());
        assertEquals("username", response.getBody().getUsername());
    }

    @Test
    void testRegisterUser_success() {
        SignupRequest signUpRequest = new SignupRequest("newuser",
                Set.of("user"),
                "password123",
                "John Doe",
                1500.0);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(roleRepository.findByRoleName(ERole.ROLE_USER)).thenReturn(Optional.of(new Role(ERole.ROLE_USER)));
        when(accountService.generateAccountId()).thenReturn("ACC123");
        when(encoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<SignupResponse> response = authController.registerUser(signUpRequest);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("newuser", response.getBody().username());
        assertEquals("John Doe", response.getBody().holderName());
        assertEquals(1500.0, response.getBody().balance());
    }

    @Test
    void testRegisterUser_usernameAlreadyTaken() {
        SignupRequest signUpRequest = new SignupRequest("existinguser",
                null,
                "password123",
                "Jane Doe",
                1500.0);

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        UsernameAlreadyExistsException exception = assertThrows(
                UsernameAlreadyExistsException.class,
                () -> authController.registerUser(signUpRequest)
        );

        assertEquals("Error: Username is already taken!", exception.getMessage());
    }

    @Test
    void testRegisterUser_insufficientBalance() {
        SignupRequest signUpRequest = new SignupRequest("lowbalanceuser",
                null,
                "password123",
                "Low Balance",
                500.0);

        when(userRepository.existsByUsername("lowbalanceuser")).thenReturn(false);

        assertThrows(InsufficientBalanceException.class,
                () -> authController.registerUser(signUpRequest));
    }

    @Test
    void testRegisterUser_roleNotFound() {
        SignupRequest signUpRequest = new SignupRequest("newuser",
                Set.of("admin"),
                "password123",
                "John Doe",
                1500.0);

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(roleRepository.findByRoleName(ERole.ROLE_ADMIN)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authController.registerUser(signUpRequest));

        assertEquals("Error: Role is not found.", exception.getMessage());
    }
}
