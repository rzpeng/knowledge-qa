package com.knowledge.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("sys_user_region")
public class SysUserRegion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long regionId;
}
