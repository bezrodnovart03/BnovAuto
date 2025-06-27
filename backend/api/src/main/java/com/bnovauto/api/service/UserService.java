package com.bnovauto.api.service;

import com.bnovauto.api.model.User;
import com.bnovauto.api.model.Role;
import com.bnovauto.api.repository.UserRepository;
import com.bnovauto.api.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
    }
    
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
    
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
    
    @Transactional
    public User createUser(User user) {
        // Проверка на существование пользователя
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("Username is already taken!");
        }
        
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email is already in use!");
        }
        
        // Кодирование пароля
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        
        return userRepository.save(user);
    }
    
    @Transactional
    public User updateUser(Long id, User userDetails) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        
        // Обновление данных пользователя
        user.setFullName(userDetails.getFullName());
        
        // Если меняется email, проверяем на уникальность
        if (!user.getEmail().equals(userDetails.getEmail())) {
            if (userRepository.existsByEmail(userDetails.getEmail())) {
                throw new RuntimeException("Email is already in use!");
            }
            user.setEmail(userDetails.getEmail());
        }
        
        // Если передан новый пароль, кодируем его
        if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        }
        
        return userRepository.save(user);
    }
    
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        
        userRepository.delete(user);
    }
    
    @Transactional
    public User addRoleToUser(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found with name: " + roleName));
        
        Set<Role> roles = user.getRoles();
        roles.add(role);
        user.setRoles(roles);
        
        return userRepository.save(user);
    }
    
    @Transactional
    public User removeRoleFromUser(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found with name: " + roleName));
        
        Set<Role> roles = user.getRoles();
        roles.remove(role);
        user.setRoles(roles);
        
        return userRepository.save(user);
    }
    
    public List<User> getUsersByCompany(Long companyId) {
        return userRepository.findByCompanyId(companyId);
    }
    
    public List<User> getUsersByRole(String roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found with name: " + roleName));
        
        return userRepository.findByRolesContaining(role);
    }
}
