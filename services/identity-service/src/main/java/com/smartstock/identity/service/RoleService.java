package com.smartstock.identity.service;

import com.smartstock.identity.dto.RoleDto;

import java.util.List;

public interface RoleService {
    List<RoleDto> getAllRoles();
    RoleDto getRoleById(String roleId);
    RoleDto getRoleByName(String name);
}
