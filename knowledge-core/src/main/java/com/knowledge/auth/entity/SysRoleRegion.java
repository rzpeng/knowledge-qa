package com.knowledge.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("sys_role_region")
public class SysRoleRegion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roleId;
    private Long regionId;
}
