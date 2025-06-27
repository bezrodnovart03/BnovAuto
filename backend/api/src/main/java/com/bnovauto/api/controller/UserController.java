package com.bnovauto.api.controller;

import com.bnovauto.api.model.User;
import com.bnovauto.api.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('DIRECTOR') or hasRole('SUPPORT')")
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('DIRECTOR') or hasRole('SUPPORT') or @userSecurity.isCurrentUser(#id)")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DIRECTOR') or hasRole('SUPPORT') or @userSecurity.isCurrentUser(#id)")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User userDetails) {
        User updatedUser = userService.updateUser(id, userDetails);
        return new ResponseEntity<>(updatedUser, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DIRECTOR') or hasRole('SUPPORT')")
    public ResponseEntity<HttpStatus> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/company/{companyId}")
    @PreAuthorize("hasRole('DIRECTOR') or hasRole('SUPPORT')")
    public ResponseEntity<List<User>> getUsersByCompany(@PathVariable Long companyId) {
        List<User> users = userService.getUsersByCompany(companyId);
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("/role/{roleName}")
    @PreAuthorize("hasRole('DIRECTOR') or hasRole('SUPPORT')")
    public ResponseEntity<List<User>> getUsersByRole(@PathVariable String roleName) {
        List<User> users = userService.getUsersByRole(roleName);
        return new ResponseEntity<>(users, HttpStatus.OK);
    }
}
