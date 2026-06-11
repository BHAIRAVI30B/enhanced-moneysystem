package com.example.backend.security.service;

import com.example.backend.entities.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class UserDetailsImpl implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;
    private final String accountId;

    public UserDetailsImpl(Long id, String username, String password,
                           Collection<? extends GrantedAuthority> authorities,
                           String accountId) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.accountId = accountId;
    }

    public static UserDetailsImpl build(UserEntity user) {
        List<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> (GrantedAuthority) () -> role.getRoleName().name())
                .toList();

        return new UserDetailsImpl(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                authorities,
                user.getAccount() != null ? user.getAccount().getAccountId() : null
        );
    }

    // Getters
    public Long getId() {
        return id;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    public String getAccountId() {
        return accountId;
    }

    // Setters (for non-final fields, but here all are final so no setters are needed)
    // If you want mutability, remove 'final' and add setters accordingly.

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserDetailsImpl that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
