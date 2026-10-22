package com.agritrace.controller;

import com.agritrace.common.PageResult;
import com.agritrace.common.Result;
import com.agritrace.common.Roles;
import com.agritrace.entity.SysUser;
import com.agritrace.security.RequireRole;
import com.agritrace.security.UserContext;
import com.agritrace.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 用户管理，仅管理员 */
@RestController
@RequestMapping("/api/users")
@RequireRole(Roles.ADMIN)
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping
    public Result<PageResult<SysUser>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String role) {
        return Result.ok(userService.page(page, size, keyword, role));
    }

    @PostMapping
    public Result<SysUser> create(@Valid @RequestBody SysUser u) {
        return Result.ok(userService.create(u));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody SysUser u) {
        userService.update(id, u, UserContext.userId());
        return Result.ok();
    }

    @PutMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id, UserContext.userId());
        return Result.ok();
    }
}
