package com.agritrace.security;

/** 当前登录用户（从 JWT 中解析出来） */
public record LoginUser(Long id, String username, String realName, String role) {
}
