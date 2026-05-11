package com.knowledge.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.auth.entity.SysAccount;
import com.knowledge.auth.mapper.SysAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysAccountService {
    private final SysAccountMapper accountMapper;
    private final PasswordEncoder passwordEncoder;

    public Page<SysAccount> page(int page, int size) {
        return accountMapper.selectPage(new Page<>(page, size), null);
    }

    public List<SysAccount> listByUserId(Long userId) {
        return accountMapper.selectList(
                new LambdaQueryWrapper<SysAccount>().eq(SysAccount::getUserId, userId));
    }

    public SysAccount getById(Long id) {
        return accountMapper.selectById(id);
    }

    public void save(SysAccount account) {
        account.setPassword(passwordEncoder.encode(account.getPassword()));
        accountMapper.insert(account);
    }

    public void update(SysAccount account) {
        if (account.getPassword() != null && !account.getPassword().isEmpty()) {
            account.setPassword(passwordEncoder.encode(account.getPassword()));
        } else {
            account.setPassword(null);
        }
        accountMapper.updateById(account);
    }

    public void delete(Long id) {
        accountMapper.deleteById(id);
    }
}
