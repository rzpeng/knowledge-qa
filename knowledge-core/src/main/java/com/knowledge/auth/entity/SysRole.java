package com.knowledge.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_role")
public class SysRole {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String code;
    private Integer deptDataScope;
    private Integer regionDataScope;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
