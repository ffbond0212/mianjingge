package com.mianjingge.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mianjingge.entity.SysUser;
import com.mianjingge.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 预置管理员账号：初始密码来自配置 mianjingge.admin.init-password
 * （本地开发默认值见 application-dev.yml / .env.example；生产未配置则不预置）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${mianjingge.admin.init-password:}")
    private String initPassword;

    @Override
    public void run(String... args) {
        Long count = sysUserMapper.selectCount(new LambdaQueryWrapper<>());
        if (count != null && count > 0) {
            return;
        }
        if (initPassword == null || initPassword.isBlank()) {
            log.warn("未配置初始管理员密码（mianjingge.admin.init-password），跳过预置账号");
            return;
        }
        SysUser admin = new SysUser();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode(initPassword));
        admin.setNickname("管理员");
        sysUserMapper.insert(admin);
        log.info("已创建默认管理员账号 admin，请尽快修改初始密码");
    }
}
