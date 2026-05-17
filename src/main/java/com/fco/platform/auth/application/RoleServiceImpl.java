package com.fco.platform.auth.application;

import com.fco.platform.common.exception.HttpNotFound;
import com.fco.platform.auth.domain.Role;
import com.fco.platform.auth.domain.RoleName;
import com.fco.platform.auth.infrastructure.persistence.IRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements IRoleService
{
    private final IRoleRepository roleRepository;

    @Override
    public Role findByRoleName(RoleName roleName)
    {
        return roleRepository.findByRoleName(roleName).orElseThrow(() -> new HttpNotFound("role not found"));
    }
}
