package com.promptops.platformapi.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * User entity class.
 *
 * Maps to the "users" table in the database.
 * Each User object represents one row in the table.
 */
@Entity
@Table(name = "users")
@Data
public class User {

    /**
     * User ID - primary key, auto-increment.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Username - unique, not null.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /**
     * Email - unique, not null.
     */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /**
     * Password hash - not null.
     * Never store plain-text passwords. Use BCrypt or similar hashing algorithms.
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * Display name - nullable.
     */
    @Column(name = "display_name", length = 100)
    private String displayName;

    /**
     * Avatar URL - nullable.
     */
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    /**
     * Status - defaults to ACTIVE.
     * Possible values: ACTIVE, INACTIVE, BANNED.
     */
    @Column(length = 20)
    private String status = "ACTIVE";

    /**
     * Created timestamp - auto-generated on insert.
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /**
     * Updated timestamp - auto-updated on modification.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;
}
