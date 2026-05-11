package com.knowledge.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.auth.entity.SysDept;
import com.knowledge.auth.mapper.SysDeptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysDeptService {
    private final SysDeptMapper deptMapper;

    public List<SysDept> listAll() {
        return deptMapper.selectList(new LambdaQueryWrapper<SysDept>()
                .orderByAsc(SysDept::getSortOrder));
    }

    public List<SysDept> listTree() {
        List<SysDept> all = listAll();
        return buildTree(all, 0L);
    }

    private List<SysDept> buildTree(List<SysDept> all, Long parentId) {
        List<SysDept> children = all.stream()
                .filter(d -> Objects.equals(d.getParentId(), parentId))
                .collect(Collectors.toList());
        for (SysDept dept : children) {
            dept.setChildren(buildTree(all, dept.getId()));
        }
        return children;
    }

    public SysDept getById(Long id) {
        return deptMapper.selectById(id);
    }

    public void save(SysDept dept) {
        deptMapper.insert(dept);
    }

    public void update(SysDept dept) {
        deptMapper.updateById(dept);
    }

    public void delete(Long id) {
        Long count = deptMapper.selectCount(
                new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
        if (count > 0) {
            throw new RuntimeException("存在子部门，无法删除");
        }
        deptMapper.deleteById(id);
    }
}
