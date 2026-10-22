package com.agritrace.service;

import com.agritrace.common.BusinessException;
import com.agritrace.common.PageResult;
import com.agritrace.common.Roles;
import com.agritrace.entity.SysUser;
import com.agritrace.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class UserService extends ServiceImpl<SysUserMapper, SysUser> {
    public static final String DEFAULT_PASSWORD = "123456";
    private final BCryptPasswordEncoder encoder;

    public PageResult<SysUser> page(int page, int size, String keyword, String role) {
        LambdaQueryWrapper<SysUser> q = new LambdaQueryWrapper<SysUser>()
                .eq(StringUtils.hasText(role), SysUser::getRole, role)
                .and(StringUtils.hasText(keyword), w -> w.like(SysUser::getUsername, keyword)
                        .or().like(SysUser::getRealName, keyword).or().like(SysUser::getPhone, keyword))
                .orderByAsc(SysUser::getId);
        return PageResult.of(page(new Page<>(page, size), q));
    }

    public SysUser create(SysUser u) {
        checkRole(u.getRole());
        u.setId(null);
        u.setUsername(u.getUsername().trim());
        String pwd = StringUtils.hasText(u.getPassword()) ? u.getPassword() : DEFAULT_PASSWORD;
        if (pwd.length() < 6) {
            throw new BusinessException("密码至少 6 位");
        }
        u.setPassword(encoder.encode(pwd));
        if (u.getStatus() == null) {
            u.setStatus(1);
        }
        if (exists(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, u.getUsername()))) {
            throw new BusinessException("用户名已存在");
        }
        save(u);
        return u;
    }

    public void update(Long id, SysUser u, Long operatorId) {
        checkRole(u.getRole());
        SysUser old = getById(id);
        if (old == null) {
            throw new BusinessException("用户不存在");
        }
        if (id.equals(operatorId) && (!Roles.ADMIN.equals(u.getRole()) || (u.getStatus() != null && u.getStatus() != 1))) {
            throw new BusinessException("不能修改自己的角色或停用自己");
        }
        u.setId(id);
        u.setUsername(null);   // 用户名不可改
        u.setPassword(null);   // 改密码走重置
        updateById(u);
    }

    public void resetPassword(Long id) {
        SysUser u = new SysUser();
        u.setId(id);
        u.setPassword(encoder.encode(DEFAULT_PASSWORD));
        if (!updateById(u)) {
            throw new BusinessException("用户不存在");
        }
    }

    public void delete(Long id, Long operatorId) {
        if (id.equals(operatorId)) {
            throw new BusinessException("不能删除自己");
        }
        removeById(id);
    }

    private void checkRole(String role) {
        if (!Roles.valid(role)) {
            throw new BusinessException("角色不正确");
        }
    }
}
