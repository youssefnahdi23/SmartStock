package com.smartstock.identity.repository;

import com.smartstock.identity.domain.model.Role;
import com.smartstock.identity.domain.repository.RoleRepository;
import com.smartstock.identity.integration.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RoleRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void findByName_seedData_systemAdminShouldExist() {
        Optional<Role> role = roleRepository.findByName("SYSTEM_ADMIN");
        assertThat(role).isPresent();
        assertThat(role.get().isSystemRole()).isTrue();
        assertThat(role.get().isActive()).isTrue();
    }

    @Test
    void findAllByActiveTrue_shouldReturnAllDefaultRoles() {
        List<Role> roles = roleRepository.findAllByActiveTrue();
        assertThat(roles).hasSizeGreaterThanOrEqualTo(6);
        List<String> names = roles.stream().map(Role::getName).toList();
        assertThat(names).contains("SYSTEM_ADMIN", "WAREHOUSE_MANAGER", "INVENTORY_OPERATOR",
                "SUPPLIER_MANAGER", "REPORTER", "AUDITOR");
    }

    @Test
    void existsByName_withExistingRole_shouldReturnTrue() {
        assertThat(roleRepository.existsByName("SYSTEM_ADMIN")).isTrue();
    }

    @Test
    void existsByName_withNonExistentRole_shouldReturnFalse() {
        assertThat(roleRepository.existsByName("NONEXISTENT_ROLE")).isFalse();
    }

    @Test
    void findByNameAndActive_shouldReturnActiveRole() {
        Optional<Role> role = roleRepository.findByNameAndActive("REPORTER");
        assertThat(role).isPresent();
        assertThat(role.get().isActive()).isTrue();
    }

    @Test
    void seededRoles_shouldHavePermissionsAssigned() {
        Role admin = roleRepository.findByName("SYSTEM_ADMIN").orElseThrow();
        assertThat(admin.getPermissions()).isNotEmpty();

        Role auditor = roleRepository.findByName("AUDITOR").orElseThrow();
        assertThat(auditor.getPermissions()).isNotEmpty();
        assertThat(auditor.getPermissions().stream()
                .anyMatch(p -> p.getName().equals("audit:read"))).isTrue();
    }
}
