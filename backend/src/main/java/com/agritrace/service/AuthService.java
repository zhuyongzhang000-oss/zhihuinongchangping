package com.agritrace.service;

import com.agritrace.common.BusinessException;
import com.agritrace.dto.LoginResponse;
import com.agritrace.entity.SysUser;
import com.agritrace.mapper.SysUserMapper;
import com.agritrace.security.JwtUtil;
import com.agritrace.security.LoginUser;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final SysUserMapper userMapper;
    private final BCryptPasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(String username, String password) {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null || !encoder.matches(password, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号已停用，请联系管理员");
        }
        String token = jwtUtil.create(new LoginUser(user.getId(), user.getUsername(), user.getRealName(), user.getRole()));
        return new LoginResponse(token, user);
    }

    public SysUser current(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(401, "账号不存在，请重新登录");
        }
        return user;
    }

    public void changePassword(Long id, String oldPassword, String newPassword) {
        SysUser user = current(id);
        if (!encoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        SysUser upd = new SysUser();
        upd.setId(id);
        upd.setPassword(encoder.encode(newPassword));
        userMapper.updateById(upd);
    }

    /** 个人资料只允许改姓名和电话 */
    public SysUser updateProfile(Long id, String realName, String phone) {
        if (realName == null || realName.isBlank()) {
            throw new BusinessException("姓名不能为空");
        }
        SysUser upd = new SysUser();
        upd.setId(id);
        upd.setRealName(realName.trim());
        upd.setPhone(phone == null ? "" : phone.trim());
        userMapper.updateById(upd);
        return current(id);
    }
}
