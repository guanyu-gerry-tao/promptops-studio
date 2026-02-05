package com.promptops.platformapi.repository;

import com.promptops.platformapi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserRepository - Data access layer for User entity.
 *
 * By extending JpaRepository, Spring Data JPA automatically provides:
 * - save(User user) - save or update a user
 * - findById(Long id) - find a user by ID
 * - findAll() - find all users
 * - delete(User user) - delete a user
 * - deleteById(Long id) - delete a user by ID
 * - count() - count total users
 * - existsById(Long id) - check if a user exists
 *
 * We only need to define additional custom query methods below.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by username.
     *
     * Spring Data JPA auto-generates the query:
     * SELECT * FROM users WHERE username = ?
     *
     * @param username the username to search for
     * @return Optional containing the User if found, empty otherwise
     */
    Optional<User> findByUsername(String username);

    /**
     * Find a user by email.
     *
     * Spring Data JPA auto-generates the query:
     * SELECT * FROM users WHERE email = ?
     *
     * @param email the email address to search for
     * @return Optional containing the User if found, empty otherwise
     */
    Optional<User> findByEmail(String email);
}
