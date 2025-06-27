package com.bnovauto.api.controller;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bnovauto.api.model.ERole;
import com.bnovauto.api.model.Role;
import com.bnovauto.api.model.User;
import com.bnovauto.api.payload.request.LoginRequest;
import com.bnovauto.api.payload.request.SignupRequest;
import com.bnovauto.api.payload.response.JwtResponse;
import com.bnovauto.api.payload.response.MessageResponse;
import com.bnovauto.api.repository.RoleRepository;
import com.bnovauto.api.repository.UserRepository;
import com.bnovauto.api.security.JwtUtils;
import com.bnovauto.api.security.UserDetailsImpl;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
  @Autowired
  AuthenticationManager authenticationManager;

  @Autowired
  UserRepository userRepository;

  @Autowired
  RoleRepository roleRepository;

  @Autowired
  PasswordEncoder encoder;

  @Autowired
  JwtUtils jwtUtils;

  @PostMapping("/signin")
  public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

    Authentication authentication = authenticationManager.authenticate(
        new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

    SecurityContextHolder.getContext().setAuthentication(authentication);
    String jwt = jwtUtils.generateJwtToken(authentication);
    
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();    
    List<String> roles = userDetails.getAuthorities().stream()
        .map(item -> item.getAuthority())
        .collect(Collectors.toList());

    return ResponseEntity.ok(new JwtResponse(jwt, 
                         userDetails.getId(), 
                         userDetails.getUsername(), 
                         userDetails.getEmail(), 
                         roles));
  }

  @PostMapping("/signup")
  public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
    if (userRepository.existsByUsername(signUpRequest.getUsername())) {
      return ResponseEntity
          .badRequest()
          .body(new MessageResponse("Ошибка: Имя пользователя уже занято!"));
    }

    if (userRepository.existsByEmail(signUpRequest.getEmail())) {
      return ResponseEntity
          .badRequest()
          .body(new MessageResponse("Ошибка: Email уже используется!"));
    }

    // Создаем нового пользователя
    User user = new User();
    user.setUsername(signUpRequest.getUsername());
    user.setEmail(signUpRequest.getEmail());
    user.setPassword(encoder.encode(signUpRequest.getPassword()));

    Set<String> strRoles = signUpRequest.getRole();
    Set<Role> roles = new HashSet<>();

    if (strRoles == null) {
      Role userRole = roleRepository.findByName(ERole.ROLE_DRIVER)
          .orElseThrow(() -> new RuntimeException("Ошибка: Роль не найдена."));
      roles.add(userRole);
    } else {
      strRoles.forEach(role -> {
        switch (role) {
        case "director":
          Role adminRole = roleRepository.findByName(ERole.ROLE_DIRECTOR)
              .orElseThrow(() -> new RuntimeException("Ошибка: Роль не найдена."));
          roles.add(adminRole);
          break;
        case "support":
          Role modRole = roleRepository.findByName(ERole.ROLE_SUPPORT)
              .orElseThrow(() -> new RuntimeException("Ошибка: Роль не найдена."));
          roles.add(modRole);
          break;
        default:
          Role userRole = roleRepository.findByName(ERole.ROLE_DRIVER)
              .orElseThrow(() -> new RuntimeException("Ошибка: Роль не найдена."));
          roles.add(userRole);
        }
      });
    }

    user.setRoles(roles);
    userRepository.save(user);

    return ResponseEntity.ok(new MessageResponse("Пользователь успешно зарегистрирован!"));
  }
}
