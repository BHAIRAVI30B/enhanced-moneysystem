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
import com.example.backend.security.websocket.SessionWebSocketHandler;
import com.example.backend.entities.Account;
import com.example.backend.enums.AccountStatus;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuthControllerTest {

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
    private SessionWebSocketHandler sessionWebSocketHandler;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetailsImpl userDetails;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authController = new AuthController(
                authenticationManager,
                userRepository,
                roleRepository,
                encoder,
                jwtUtils,
                accountService,
                sessionWebSocketHandler
        );
    }

    @Test
    void testAuthenticateUser_success() {
        LoginRequest loginRequest = new LoginRequest("username", "password");

        Account account = new Account();
        account.setAccountId("ACC1234");
        account.setStatus(AccountStatus.ACTIVE);

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername("username");
        userEntity.setAccount(account);

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("username");
        when(userDetails.getId()).thenReturn(1L);
        when(userDetails.getAuthorities()).thenReturn(new HashSet<>());
        when(userRepository.findByUsername("username")).thenReturn(userEntity);
        when(jwtUtils.generateJwtToken(eq(authentication), anyString())).thenReturn("mock-jwt-token");

        ResponseEntity<JwtResponse> response = authController.authenticateUser(loginRequest);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("mock-jwt-token", response.getBody().getToken());
        assertEquals("username", response.getBody().getUsername());
        verify(sessionWebSocketHandler, times(1)).kickOtherSessions("username");
        verify(userRepository, times(1)).save(userEntity);
        assertNotNull(userEntity.getSessionId());
    }

    @Test
    void testAuthenticateUser_closedAccount_throwsAccountClosedException() {
        LoginRequest loginRequest = new LoginRequest("username", "password");

        Account account = new Account();
        account.setAccountId("ACC1234");
        account.setStatus(AccountStatus.CLOSED);

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername("username");
        userEntity.setAccount(account);

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("username");
        when(userRepository.findByUsername("username")).thenReturn(userEntity);

        assertThrows(com.example.backend.exceptions.AccountClosedException.class,
                () -> authController.authenticateUser(loginRequest));

        verify(userRepository, never()).save(any());
        verify(sessionWebSocketHandler, never()).kickOtherSessions(any());
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
    void testAuthenticateUser_nullAccount_skipsClosedCheckAndSucceeds() {
        // Covers the `user.getAccount() != null && ...` short-circuit branch where
        // getAccount() is null — the CLOSED check must be skipped, not throw an NPE.
        LoginRequest loginRequest = new LoginRequest("username", "password");

        UserEntity userEntity = new UserEntity();
        userEntity.setUsername("username");
        userEntity.setAccount(null);

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userDetails.getUsername()).thenReturn("username");
        when(userDetails.getId()).thenReturn(1L);
        when(userDetails.getAuthorities()).thenReturn(new HashSet<>());
        when(userRepository.findByUsername("username")).thenReturn(userEntity);
        when(jwtUtils.generateJwtToken(eq(authentication), anyString())).thenReturn("mock-jwt-token");

        ResponseEntity<JwtResponse> response = authController.authenticateUser(loginRequest);

        assertEquals(200, response.getStatusCode().value());
        verify(sessionWebSocketHandler, times(1)).kickOtherSessions("username");
        verify(userRepository, times(1)).save(userEntity);
    }

    @Test
    void testCheckUsernameAvailability_available() {
        when(userRepository.existsByUsername("freeUsername")).thenReturn(false);

        ResponseEntity<Boolean> response = authController.checkUsernameAvailability("freeUsername");

        assertEquals(200, response.getStatusCode().value());
        assertTrue(response.getBody());
    }

    @Test
    void testCheckUsernameAvailability_taken() {
        when(userRepository.existsByUsername("takenUsername")).thenReturn(true);

        ResponseEntity<Boolean> response = authController.checkUsernameAvailability("takenUsername");

        assertEquals(200, response.getStatusCode().value());
        assertFalse(response.getBody());
    }

    @Test
    void testLogout_clearsSessionIdAndSaves() {
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername("username");
        userEntity.setSessionId("some-existing-session-id");

        when(authentication.getName()).thenReturn("username");
        org.springframework.security.core.context.SecurityContextHolder
                .getContext().setAuthentication(authentication);
        when(userRepository.findByUsername("username")).thenReturn(userEntity);

        ResponseEntity<?> response = authController.logout();

        assertEquals(200, response.getStatusCode().value());
        assertEquals("Logged out successfully.", response.getBody());
        assertNull(userEntity.getSessionId());
        verify(userRepository, times(1)).save(userEntity);

        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }

    @Test
    void testRegisterUser_adminRoleFound_success() {
        // Covers the `if (role.equals("admin"))` branch where the role lookup succeeds —
        // existing testRegisterUser_roleNotFound only covers the not-found half of this branch.
        SignupRequest signUpRequest = new SignupRequest("newadmin",
                Set.of("admin"),
                "password123",
                "Admin Person",
                2000.0);

        when(userRepository.existsByUsername("newadmin")).thenReturn(false);
        when(roleRepository.findByRoleName(ERole.ROLE_ADMIN))
                .thenReturn(Optional.of(new Role(ERole.ROLE_ADMIN)));
        when(accountService.generateAccountId()).thenReturn("ACC999");
        when(encoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<SignupResponse> response = authController.registerUser(signUpRequest);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("newadmin", response.getBody().username());
        assertEquals("Admin Person", response.getBody().holderName());
    }
}