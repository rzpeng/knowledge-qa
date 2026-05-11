package com.knowledge.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.auth.entity.SysRegion;
import com.knowledge.auth.mapper.SysRegionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysRegionService {
    private final SysRegionMapper regionMapper;

    public List<SysRegion> listAll() {
        return regionMapper.selectList(new LambdaQueryWrapper<SysRegion>()
                .orderByAsc(SysRegion::getId));
    }

    public SysRegion getById(Long id) {
        return regionMapper.selectById(id);
    }

    public void save(SysRegion region) {
        regionMapper.insert(region);
    }

    public void update(SysRegion region) {
        regionMapper.updateById(region);
    }

    public void delete(Long id) {
        regionMapper.deleteById(id);
    }
}
