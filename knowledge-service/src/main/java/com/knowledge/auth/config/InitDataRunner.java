package com.knowledge.auth.config;

import com.knowledge.auth.entity.SysAccount;
import com.knowledge.auth.entity.SysUser;
import com.knowledge.auth.mapper.SysAccountMapper;
import com.knowledge.auth.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitDataRunner implements CommandLineRunner {

    private final SysAccountMapper accountMapper;
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (accountMapper.selectCount(null) > 0) {
            return;
        }

        log.info("======== 初始化系统数据 ========");

        SysUser adminUser = new SysUser();
        adminUser.setName("管理员");
        adminUser.setStatus(1);
        userMapper.insert(adminUser);

        SysAccount adminAccount = new SysAccount();
        adminAccount.setUserId(adminUser.getId());
        adminAccount.setUsername("admin");
        adminAccount.setPassword(passwordEncoder.encode("admin123"));
        adminAccount.setIsSuperAdmin(1);
        adminAccount.setStatus(1);
        accountMapper.insert(adminAccount);

        log.info("超级管理员账号创建成功: admin / admin123");
        log.info("======== 系统数据初始化完成 ========");
    }
}
