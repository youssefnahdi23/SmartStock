package com.smartstock.identity.service;

import com.smartstock.identity.api.dto.request.CreateRoleRequest;
import com.smartstock.identity.api.dto.response.RoleResponse;
import com.smartstock.identity.domain.model.Role;
import com.smartstock.identity.domain.repository.PermissionRepository;
import com.smartstock.identity.domain.repository.RoleRepository;
import com.smartstock.identity.exception.BusinessException;
import com.smartstock.identity.exception.RoleNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock private RoleRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;

    @InjectMocks
    private RoleService roleService;

    @Test
    void findAll_shouldReturnPagedRoles() {
        Role role = buildRole("role-1", "WAREHOUSE_MANAGER");
        Page<Role> page = new PageImpl<>(List.of(role));
        when(roleRepository.findAllByActiveTrue(any())).thenReturn(page);

        Page<RoleResponse> result = roleService.findAll(PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("WAREHOUSE_MANAGER");
    }

    @Test
    void findById_withValidId_shouldReturnRole() {
        Role role = buildRole("role-1", "SYSTEM_ADMIN");
        when(roleRepository.findByIdAndActive("role-1")).thenReturn(Optional.of(role));

        RoleResponse response = roleService.findById("role-1");
        assertThat(response.getName()).isEqualTo("SYSTEM_ADMIN");
    }

    @Test
    void findById_withInvalidId_shouldThrowRoleNotFound() {
        when(roleRepository.findByIdAndActive("bad-id")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> roleService.findById("bad-id"))
                .isInstanceOf(RoleNotFoundException.class);
    }

    @Test
    void createRole_withUniqueName_shouldSucceed() {
        when(roleRepository.existsByName("NEW_ROLE")).thenReturn(false);
        Role savedRole = buildRole("role-new", "NEW_ROLE");
        when(roleRepository.save(any())).thenReturn(savedRole);

        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("NEW_ROLE");
        request.setDescription("A new test role");

        RoleResponse response = roleService.createRole(request);
        assertThat(response.getName()).isEqualTo("NEW_ROLE");
        verify(roleRepository).save(any(Role.class));
    }

    @Test
    void createRole_withDuplicateName_shouldThrow() {
        when(roleRepository.existsByName("EXISTING_ROLE")).thenReturn(true);

        CreateRoleRequest request = new CreateRoleRequest();
        request.setName("EXISTING_ROLE");

        assertThatThrownBy(() -> roleService.createRole(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    // --------------------------------------------------------

    private Role buildRole(String id, String name) {
        return Role.builder()
                .id(id)
                .name(name)
                .description("Test role")
                .systemRole(false)
                .active(true)
                .permissions(new HashSet<>())
                .build();
    }
}
