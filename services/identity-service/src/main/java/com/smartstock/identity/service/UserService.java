package com.smartstock.identity.service;

import com.smartstock.identity.dto.UserCreateRequest;
import com.smartstock.identity.dto.UserResponse;
import com.smartstock.identity.entity.User;

import java.util.List;

public interface UserService {
    UserResponse createUser(UserCreateRequest request);
    UserResponse getUserById(String userId);
    UserResponse getUserByUsername(String username);
    List<UserResponse> getAllUsers();
    UserResponse updateUser(String userId, UserCreateRequest request);
    void deleteUser(String userId);
    void assignRoleToUser(String userId, String roleName);
    void removeRoleFromUser(String userId, String roleName);
    User getUserEntityById(String userId);
}
