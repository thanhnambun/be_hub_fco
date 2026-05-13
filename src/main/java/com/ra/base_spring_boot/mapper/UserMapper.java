package com.ra.base_spring_boot.mapper;

import com.ra.base_spring_boot.dto.resp.UserResponse;
import com.ra.base_spring_boot.model.User;
import org.mapstruct.Mapper;

@Mapper(config = MapStructCentralConfig.class)
public interface UserMapper {
    UserResponse toResponse(User user);
}
