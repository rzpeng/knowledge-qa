package com.knowledge.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_account")
public class SysAccount {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String password;
    private String accountType;
    private Integer isSuperAdmin;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
