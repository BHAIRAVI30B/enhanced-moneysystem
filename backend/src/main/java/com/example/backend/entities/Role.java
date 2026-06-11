package com.example.backend.entities;

import com.example.backend.enums.ERole;
import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ERole roleName;

    // Default constructor
    public Role() {
    }

    // All-args constructor
    public Role(Integer id, ERole roleName) {
        this.id = id;
        this.roleName = roleName;
    }

    // Constructor with only roleName
    public Role(ERole roleName) {
        this.roleName = roleName;
    }

    // Getters
    public Integer getId() {
        return id;
    }

    public ERole getRoleName() {
        return roleName;
    }

    // Setters
    public void setId(Integer id) {
        this.id = id;
    }

    public void setRoleName(ERole roleName) {
        this.roleName = roleName;
    }

    @Override
    public String toString() {
        return "Role{" +
                "id=" + id +
                ", roleName=" + roleName +
                '}';
    }
}
