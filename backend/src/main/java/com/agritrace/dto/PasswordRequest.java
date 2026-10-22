package com.agritrace.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordRequest(@NotBlank(message = "请输入原密码") String oldPassword,
                              @NotBlank(message = "请输入新密码") @Size(min = 6, max = 32, message = "新密码 6~32 位") String newPassword) {
}
