package com.promptops.platformapi.service;

import com.promptops.platformapi.entity.User;

/**
 * User service interface - defines business operations for user management.
 */
public interface UserService {

    /**
     * Register a new user.
     *
     * @param username    desired username
     * @param email       user's email address
     * @param password    plain-text password (will be hashed)
     * @param displayName optional display name
     * @return the newly created User entity
     */
    User register(String username, String email, String password, String displayName);

    /**
     * Login and return a JWT token.
     *
     * @param username the username
     * @param password the plain-text password to verify
     * @return a JWT token string
     */
    String login(String username, String password);

    /**
     * Find a user by ID.
     *
     * @param id the user ID
     * @return the User entity
     */
    User findById(Long id);
}
