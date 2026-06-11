package com.example.backend.entities;

import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    // NEW: tracks the active session — overwritten on each new login, nulled on logout
    @Column(name = "session_id")
    private String sessionId;

    // One account per user
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "account_id", referencedColumnName = "account_id")
    private Account account;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // Default constructor
    public UserEntity() {
    }

    // All-args constructor
    public UserEntity(Long id, String username, String password, Account account, Set<Role> roles) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.account = account;
        this.roles = roles;
    }

    // Constructor with username and password
    public UserEntity(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Account getAccount() {
        return account;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    @Override
    public String toString() {
        return "UserEntity{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", account=" + account +
                '}';
    }
}