package com.example.backend.repositories;

import com.example.backend.entities.Role;
import com.example.backend.enums.ERole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByRoleName(ERole roleName);
}
