package com.promptops.platformapi.service;

import static org.junit.jupiter.api.Assertions.*;

import com.promptops.platformapi.entity.User;
import com.promptops.platformapi.exception.BusinessException;
import com.promptops.platformapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserServiceTest {

  @Autowired
  private UserService userService;

  @Autowired
  private UserRepository userRepository;

  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  private User user1;
  private String user1Pw;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();

    this.user1Pw = "newUser1";

    this.user1 = new User();
    this.user1.setUsername("newUser1");
    this.user1.setEmail("newUser1@test.com");
    this.user1.setPasswordHash(passwordEncoder.encode(this.user1Pw));
    this.user1.setDisplayName("New User One");
    userRepository.save(user1);
  }

  @Test
  void register_fail_duplicate_username() {
    assertThrows(BusinessException.class,
        () -> userService.register("newUser1",
            "notdupemail@test.com",
            this.user1Pw,
            "User Name Dup Test User"
        )
    );
  }

  @Test
  void register_fail_duplicate_email() {
    assertThrows(BusinessException.class,
        () -> userService.register(
            "notDupUsername",
            user1.getEmail(),
            this.user1Pw,
            "User Email Dup Test User"
        )
    );
  }

  @Test
  void register() {
    userService.register(
        "newRegUsername",
        "newRegEm",
        "newUser1",
        "newRegDisplayName"
    );

    assertNotNull(userRepository.findByUsername("newRegUsername"));
  }

  @Test
  void login_fail_userNameNotExist() {
    assertThrows(BusinessException.class, () -> userService.login("nonexistent", "newUser1"));
  }

  @Test
  void login_fail_passwordWrong() {
    assertThrows(BusinessException.class, () -> userService.login(user1.getUsername(), "wrongpw"));
  }

  @Test
  void login() {
    String newLoginToken = userService.login(user1.getUsername(), this.user1Pw);
    assertNotNull(newLoginToken);
  }

  @Test
  void findById() {
    User findedUser = userService.findById(user1.getId());
    assertEquals(user1.getId(), findedUser.getId());
  }
}