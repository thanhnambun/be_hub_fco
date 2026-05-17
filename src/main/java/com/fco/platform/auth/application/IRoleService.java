package com.fco.platform.auth.application;

import com.fco.platform.auth.domain.Role;
import com.fco.platform.auth.domain.RoleName;

public interface IRoleService
{
    Role findByRoleName(RoleName roleName);
}
