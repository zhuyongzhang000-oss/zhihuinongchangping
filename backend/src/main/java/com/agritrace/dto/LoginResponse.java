package com.agritrace.dto;

import com.agritrace.entity.SysUser;

public record LoginResponse(String token, SysUser user) {
}
