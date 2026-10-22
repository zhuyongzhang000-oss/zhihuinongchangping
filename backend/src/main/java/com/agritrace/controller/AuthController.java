package com.agritrace.controller;

import com.agritrace.common.Result;
import com.agritrace.dto.LoginRequest;
import com.agritrace.dto.LoginResponse;
import com.agritrace.dto.PasswordRequest;
import com.agritrace.entity.SysUser;
import com.agritrace.security.UserContext;
import com.agritrace.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(authService.login(req.username().trim(), req.password()));
    }

    @GetMapping("/me")
    public Result<SysUser> me() {
        return Result.ok(authService.current(UserContext.userId()));
    }

    @PutMapping("/profile")
    public Result<SysUser> profile(@RequestBody Map<String, String> body) {
        return Result.ok(authService.updateProfile(UserContext.userId(), body.get("realName"), body.get("phone")));
    }

    @PutMapping("/password")
    public Result<Void> password(@Valid @RequestBody PasswordRequest req) {
        authService.changePassword(UserContext.userId(), req.oldPassword(), req.newPassword());
        return Result.ok();
    }
}
