# 权限管理模块 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a complete user management + permission system with Spring Security, including user/account/role/dept/menu management and row-level data permission control integrated with the existing knowledge base Q&A system.

**Architecture:** Extends existing knowledge-core/service/api/ui four-module structure. Uses Spring Security + JWT for authentication, MyBatis-Plus DataPermissionInterceptor for row-level data isolation (dept + region dual dimensions), and RBAC for menu/button permission control. Super admin bypasses all checks via `is_super_admin` flag.

**Tech Stack:** Spring Boot 3.2.5, Spring Security, JWT (jjwt 0.12.5), MyBatis-Plus 3.5.9, Redis, Vue 3.4 + Element Plus

**Package Layout:**
```
knowledge-core:
  com.knowledge.auth.entity/*        — 12 entity classes
  com.knowledge.auth.enums/*         — 2 enum classes
  com.knowledge.auth.mapper/*        — 6 mapper interfaces
  com.knowledge.common.annotation/   — @DataPermission
  com.knowledge.common.interceptor/  — DataPermissionInterceptor

knowledge-service:
  com.knowledge.auth.service/*       — 7 service classes
  com.knowledge.auth.security/*      — JWT + UserDetails
  com.knowledge.auth.permission/*    — Data scope providers

knowledge-api:
  com.knowledge.auth.controller/*    — 7 controller classes
  com.knowledge.auth.dto/*           — 5+ DTO classes
  com.knowledge.auth.security/*      — SecurityConfig, JwtAuthFilter

knowledge-ui:
  src/views/login/LoginView.vue
  src/views/system/{User,Account,Role,Dept,Menu,Region}View.vue
  src/api/{auth,user,account,role,dept,menu,region}.js
  src/stores/auth.js
  src/directives/permission.js
  src/router/index.js (modified)
  src/App.vue (modified)
```

---

### Task 1: 添加依赖和数据库表结构

**Files:**
- Modify: `knowledge-api/build.gradle`
- Modify: `knowledge-api/src/main/resources/db/schema.sql`

- [ ] **Step 1: 添加 Maven 依赖**

Add to `knowledge-api/build.gradle` after existing dependencies:

```groovy
dependencies {
    // existing dependencies...

    // Spring Security
    implementation 'org.springframework.boot:spring-boot-starter-security'

    // JWT
    implementation 'io.jsonwebtoken:jjwt-api:0.12.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.5'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.5'
}
```

- [ ] **Step 2: 添加数据库表 DDL**

Append to `knowledge-api/src/main/resources/db/schema.sql`:

```sql
-- ========== 权限管理模块表结构 ==========

-- 部门表
CREATE TABLE IF NOT EXISTS sys_dept (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    name VARCHAR(100) NOT NULL COMMENT '部门名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父部门ID',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 地区表
CREATE TABLE IF NOT EXISTS sys_region (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    name VARCHAR(100) NOT NULL COMMENT '地区名称',
    code VARCHAR(50) COMMENT '地区编码',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='地区表';

-- 用户表（人员信息）
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    name VARCHAR(100) NOT NULL COMMENT '姓名',
    phone VARCHAR(20) COMMENT '手机号',
    email VARCHAR(255) COMMENT '邮箱',
    avatar VARCHAR(500) COMMENT '头像',
    status TINYINT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 账号表
CREATE TABLE IF NOT EXISTS sys_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '关联用户ID',
    username VARCHAR(100) NOT NULL COMMENT '登录名',
    password VARCHAR(255) NOT NULL COMMENT '密码(bcrypt)',
    account_type VARCHAR(20) DEFAULT 'PASSWORD' COMMENT '账号类型',
    is_super_admin TINYINT DEFAULT 0 COMMENT '是否超级管理员 0否 1是',
    status TINYINT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账号表';

-- 菜单表
CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    name VARCHAR(100) NOT NULL COMMENT '菜单名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父菜单ID',
    type TINYINT NOT NULL COMMENT '类型 0目录 1菜单 2按钮',
    permission VARCHAR(200) COMMENT '权限标识如 system:user:list',
    path VARCHAR(255) COMMENT '路由路径',
    component VARCHAR(255) COMMENT '组件路径',
    icon VARCHAR(100) COMMENT '图标',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- 角色表
CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    name VARCHAR(100) NOT NULL COMMENT '角色名称',
    code VARCHAR(100) NOT NULL COMMENT '角色编码',
    dept_data_scope TINYINT DEFAULT 3 COMMENT '部门数据权限 1全部 2自定义 3本部门及下属 4本部门 5本人',
    region_data_scope TINYINT DEFAULT 1 COMMENT '地区数据权限 1全部 2自定义 3本用户所属地区',
    status TINYINT DEFAULT 1 COMMENT '状态 0禁用 1启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 用户-部门关联表
CREATE TABLE IF NOT EXISTS sys_user_dept (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    is_leader TINYINT DEFAULT 0 COMMENT '是否部门负责人 0否 1是',
    UNIQUE KEY uk_user_dept (user_id, dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户部门关联表';

-- 用户-地区关联表
CREATE TABLE IF NOT EXISTS sys_user_region (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    region_id BIGINT NOT NULL COMMENT '地区ID',
    UNIQUE KEY uk_user_region (user_id, region_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户地区关联表';

-- 用户-角色关联表
CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    UNIQUE KEY uk_user_role (user_id, role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- 角色-菜单关联表
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    UNIQUE KEY uk_role_menu (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- 角色-部门关联表(自定义数据权限)
CREATE TABLE IF NOT EXISTS sys_role_dept (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    dept_id BIGINT NOT NULL COMMENT '部门ID',
    UNIQUE KEY uk_role_dept (role_id, dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色数据权限部门关联表';

-- 角色-地区关联表(自定义数据权限)
CREATE TABLE IF NOT EXISTS sys_role_region (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    region_id BIGINT NOT NULL COMMENT '地区ID',
    UNIQUE KEY uk_role_region (role_id, region_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色数据权限地区关联表';

-- 初始化默认菜单数据
INSERT INTO sys_menu (name, parent_id, type, permission, path, component, icon, sort_order) VALUES
('系统管理', 0, 0, NULL, '/system', NULL, 'Setting', 1),
('用户管理', 1, 1, 'system:user:list', '/system/user', 'system/UserView', 'User', 1),
('账号管理', 1, 1, 'system:account:list', '/system/account', 'system/AccountView', 'Key', 2),
('角色管理', 1, 1, 'system:role:list', '/system/role', 'system/RoleView', 'Avatar', 3),
('部门管理', 1, 1, 'system:dept:list', '/system/dept', 'system/DeptView', 'Organization', 4),
('菜单管理', 1, 1, 'system:menu:list', '/system/menu', 'system/MenuView', 'Menu', 5),
('地区管理', 1, 1, 'system:region:list', '/system/region', 'system/RegionView', 'MapLocation', 6);

-- 用户管理按钮权限
INSERT INTO sys_menu (name, parent_id, type, permission, sort_order) VALUES
('用户新增', 2, 2, 'system:user:add', 1),
('用户编辑', 2, 2, 'system:user:edit', 2),
('用户删除', 2, 2, 'system:user:delete', 3);

-- 账号管理按钮权限
INSERT INTO sys_menu (name, parent_id, type, permission, sort_order) VALUES
('账号新增', 3, 2, 'system:account:add', 1),
('账号编辑', 3, 2, 'system:account:edit', 2),
('账号删除', 3, 2, 'system:account:delete', 3);

-- 角色管理按钮权限
INSERT INTO sys_menu (name, parent_id, type, permission, sort_order) VALUES
('角色新增', 4, 2, 'system:role:add', 1),
('角色编辑', 4, 2, 'system:role:edit', 2),
('角色删除', 4, 2, 'system:role:delete', 3);

-- 部门管理按钮权限
INSERT INTO sys_menu (name, parent_id, type, permission, sort_order) VALUES
('部门新增', 5, 2, 'system:dept:add', 1),
('部门编辑', 5, 2, 'system:dept:edit', 2),
('部门删除', 5, 2, 'system:dept:delete', 3);

-- 菜单管理按钮权限
INSERT INTO sys_menu (name, parent_id, type, permission, sort_order) VALUES
('菜单新增', 6, 2, 'system:menu:add', 1),
('菜单编辑', 6, 2, 'system:menu:edit', 2),
('菜单删除', 6, 2, 'system:menu:delete', 3);

-- 地区管理按钮权限
INSERT INTO sys_menu (name, parent_id, type, permission, sort_order) VALUES
('地区新增', 7, 2, 'system:region:add', 1),
('地区编辑', 7, 2, 'system:region:edit', 2),
('地区删除', 7, 2, 'system:region:delete', 3);
```

- [ ] **Step 3: 提交**

```bash
git add knowledge-api/build.gradle knowledge-api/src/main/resources/db/schema.sql
git commit -m "feat: add auth module dependencies and database schema"
```

---

### Task 2: 创建枚举类和基础 Entity 类

**Files:**
- Create: `knowledge-core/src/main/java/com/knowledge/auth/enums/DataScopeDeptEnum.java`
- Create: `knowledge-core/src/main/java/com/knowledge/auth/enums/DataScopeRegionEnum.java`
- Create: `knowledge-core/src/main/java/com/knowledge/auth/enums/MenuTypeEnum.java`

- [ ] **Step 1: 创建部门数据权限枚举**

```java
package com.knowledge.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DataScopeDeptEnum {
    ALL(1, "全部数据"),
    CUSTOM(2, "自定义部门"),
    DEPT_AND_SUB(3, "本部门及下属"),
    DEPT_ONLY(4, "本部门"),
    SELF(5, "本人");

    private final int value;
    private final String desc;
}
```

- [ ] **Step 2: 创建地区数据权限枚举**

```java
package com.knowledge.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DataScopeRegionEnum {
    ALL(1, "全部地区"),
    CUSTOM(2, "自定义地区"),
    USER_REGION(3, "本用户所属地区");

    private final int value;
    private final String desc;
}
```

- [ ] **Step 3: 创建菜单类型枚举**

```java
package com.knowledge.auth.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MenuTypeEnum {
    DIR(0, "目录"),
    MENU(1, "菜单"),
    BUTTON(2, "按钮");

    private final int value;
    private final String desc;
}
```

- [ ] **Step 4: 提交**

```bash
git add knowledge-core/src/main/java/com/knowledge/auth/enums/
git commit -m "feat: add auth enums"
```

---

### Task 3: 创建所有 Entity 类

**Files:**
- Create: `knowledge-core/src/main/java/com/knowledge/auth/entity/SysDept.java`
- Create: `knowledge-core/src/main/java/com/knowledge/auth/entity/SysRegion.java`
- Create: `knowledge-core/src/main/java/com/knowledge/auth/entity/SysMenu.java`
- Create: `knowledge-core/src/main/java/com/knowledge/auth/entity/SysUser.java`
- Create: `knowledge-core/src/main/java/com/knowledge/auth/entity/SysAccount.java`
- Create: `knowledge-core/src/main/java/com/knowledge/auth/entity/SysRole.java`
- Create: `knowledge-core/src/main/java/com/knowledge/auth/entity/SysUserDept.java`
- Create: `knowledge-core/src/main/java/com/knowledge/auth/entity/SysUserRegion.java`
- Create: `knowledge-core/src/main/java/com/knowledge/auth/entity/SysUserRole.java`
- Create: `knowledge-core/src/main/java/com/knowledge/auth/entity/SysRoleMenu.java`
- Create: `knowledge-core/src/main/java/com/knowledge/auth/entity/SysRoleDept.java`
- Create: `knowledge-core/src/main/java/com/knowledge/auth/entity/SysRoleRegion.java`

- [ ] **Step 1: 创建 SysDept**

```java
package com.knowledge.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_dept")
public class SysDept {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long parentId;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

- [ ] **Step 2: 创建 SysRegion**

```java
package com.knowledge.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_region")
public class SysRegion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String code;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

- [ ] **Step 3: 创建 SysMenu**

```java
package com.knowledge.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_menu")
public class SysMenu {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long parentId;
    private Integer type;
    private String permission;
    private String path;
    private String component;
    private String icon;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

- [ ] **Step 4: 创建 SysUser**

```java
package com.knowledge.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String phone;
    private String email;
    private String avatar;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

- [ ] **Step 5: 创建 SysAccount**

```java
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
```

- [ ] **Step 6: 创建 SysRole**

```java
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
```

- [ ] **Step 7: 创建关联 Entity（SysUserDept, SysUserRegion, SysUserRole, SysRoleMenu, SysRoleDept, SysRoleRegion）**

```java
package com.knowledge.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

// SysUserDept
@Data
@TableName("sys_user_dept")
public class SysUserDept {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long deptId;
    private Integer isLeader;
}

// SysUserRegion
@Data
@TableName("sys_user_region")
public class SysUserRegion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long regionId;
}

// SysUserRole
@Data
@TableName("sys_user_role")
public class SysUserRole {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long roleId;
}

// SysRoleMenu
@Data
@TableName("sys_role_menu")
public class SysRoleMenu {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roleId;
    private Long menuId;
}

// SysRoleDept
@Data
@TableName("sys_role_dept")
public class SysRoleDept {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roleId;
    private Long deptId;
}

// SysRoleRegion
@Data
@TableName("sys_role_region")
public class SysRoleRegion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roleId;
    private Long regionId;
}
```

Note: Put each class in its own file, not combined as shown above (the code above is illustrative). Create 6 separate files under `com.knowledge.auth.entity` package.

- [ ] **Step 8: 提交**

```bash
git add knowledge-core/src/main/java/com/knowledge/auth/entity/
git commit -m "feat: add auth entities"
```

---

### Task 4: 创建 Mapper 接口

**Files:**
- Create: `knowledge-core/src/main/java/com/knowledge/auth/mapper/SysDeptMapper.java`
- Create: `knowledge-core/src/main/java/com/knowledge/auth/mapper/SysRegionMapper.java`
- Create: `knowledge-core/src/main/java/com/knowledge/auth/mapper/SysMenuMapper.java`
- Create: `knowledge-core/src/main/java/com/knowledge/auth/mapper/SysUserMapper.java`
- Create: `knowledge-core/src/main/java/com/knowledge/auth/mapper/SysAccountMapper.java`
- Create: `knowledge-core/src/main/java/com/knowledge/auth/mapper/SysRoleMapper.java`
- Create: `knowledge-core/src/main/java/com/knowledge/auth/mapper/SysUserDeptMapper.java`
- Create: `knowledge-core/src/main/java/com/knowledge/auth/mapper/SysUserRegionMapper.java`
- Create: `knowledge-core/src/main/java/com/knowledge/auth/mapper/SysUserRoleMapper.java`
- Create: `knowledge-core/src/main/java/com/knowledge/auth/mapper/SysRoleMenuMapper.java`
- Create: `knowledge-core/src/main/java/com/knowledge/auth/mapper/SysRoleDeptMapper.java`
- Create: `knowledge-core/src/main/java/com/knowledge/auth/mapper/SysRoleRegionMapper.java`

- [ ] **Step 1: 创建基础 Entity mapper base package**

Ensure mapper scanning covers the new package. The existing `@MapperScan("com.knowledge.mapper")` in `KnowledgeApplication.java` only scans `com.knowledge.mapper`. The new mappers are under `com.knowledge.auth.mapper`. Change the `@MapperScan` to scan both:

```java
@MapperScan({"com.knowledge.mapper", "com.knowledge.auth.mapper"})
```

- [ ] **Step 2: 创建各 Mapper 接口**

Each mapper follows this exact pattern (create one file per mapper):

```java
package com.knowledge.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.auth.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {
}
```

Repeat for: SysRegionMapper, SysMenuMapper, SysUserMapper, SysAccountMapper, SysRoleMapper, SysUserDeptMapper, SysUserRegionMapper, SysUserRoleMapper, SysRoleMenuMapper, SysRoleDeptMapper, SysRoleRegionMapper.

- [ ] **Step 3: 创建自定义查询 Mapper 方法**

`SysUserMapper.java` needs extra methods for auth:

```java
package com.knowledge.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.auth.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    @Select("SELECT r.dept_data_scope, r.region_data_scope " +
            "FROM sys_user_role ur " +
            "JOIN sys_role r ON ur.role_id = r.id " +
            "WHERE ur.user_id = #{userId}")
    List<SysRole> selectRoleDataScopesByUserId(Long userId);

    @Select("SELECT rd.dept_id FROM sys_role_dept rd " +
            "JOIN sys_user_role ur ON rd.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<Long> selectCustomDeptIdsByUserId(Long userId);

    @Select("SELECT rr.region_id FROM sys_role_region rr " +
            "JOIN sys_user_role ur ON rr.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<Long> selectCustomRegionIdsByUserId(Long userId);
}
```

- [ ] **Step 4: 提交**

```bash
git add knowledge-core/src/main/java/com/knowledge/auth/mapper/ knowledge-core/src/main/java/com/knowledge/KnowledgeApplication.java
git commit -m "feat: add auth mappers"
```

---

### Task 5: 创建 @DataPermission 注解和数据权限拦截器

**Files:**
- Create: `knowledge-core/src/main/java/com/knowledge/common/annotation/DataPermission.java`
- Create: `knowledge-core/src/main/java/com/knowledge/common/interceptor/DataPermissionInterceptor.java`

- [ ] **Step 1: 创建 @DataPermission 注解**

```java
package com.knowledge.common.annotation;

import java.lang.annotation.*;

@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataPermission {
    /** 需要过滤的表别名 */
    String tableAlias() default "";
    /** 部门ID字段名 */
    String deptIdField() default "dept_id";
    /** 地区ID字段名 */
    String regionIdField() default "region_id";
    /** 创建人字段名 */
    String createByField() default "create_by";
    /** 是否启用部门数据权限 */
    boolean enableDept() default true;
    /** 是否启用地区数据权限 */
    boolean enableRegion() default true;
}
```

- [ ] **Step 2: 创建 DataPermissionInterceptor**

This is the core data permission interceptor that uses MyBatis-Plus's inner interceptor chain.

```java
package com.knowledge.common.interceptor;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.knowledge.common.annotation.DataPermission;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.*;

public class DataPermissionInterceptor implements InnerInterceptor {

    private final DataScopeService dataScopeService;

    public DataPermissionInterceptor(DataScopeService dataScopeService) {
        this.dataScopeService = dataScopeService;
    }

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
        // 获取当前用户认证信息
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return;
        }

        // 检查是否有 @DataPermission 注解
        DataPermission dataPermission = getDataPermissionAnnotation(ms);
        if (dataPermission == null) {
            return;
        }

        // 获取 SQL
        String originalSql = boundSql.getSql();
        if (originalSql == null || !originalSql.trim().toUpperCase().startsWith("SELECT")) {
            return;
        }

        // 构建数据权限 SQL 片段
        String dataSql = dataScopeService.buildDataScopeSql(dataPermission);
        if (!StringUtils.hasText(dataSql)) {
            return;
        }

        // 拼接 WHERE 条件
        String newSql;
        if (originalSql.toUpperCase().contains("WHERE")) {
            newSql = originalSql + " AND (" + dataSql + ")";
        } else {
            newSql = originalSql + " WHERE " + dataSql;
        }

        // 通过反射修改 BoundSql 的 SQL
        try {
            java.lang.reflect.Field field = BoundSql.class.getDeclaredField("sql");
            field.setAccessible(true);
            field.set(boundSql, newSql);
        } catch (Exception e) {
            throw new RuntimeException("Failed to inject data permission SQL", e);
        }
    }

    private DataPermission getDataPermissionAnnotation(MappedStatement ms) {
        try {
            String id = ms.getId();
            Class<?> clazz = Class.forName(id.substring(0, id.lastIndexOf('.')));
            String methodName = id.substring(id.lastIndexOf('.') + 1);

            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(methodName) && method.isAnnotationPresent(DataPermission.class)) {
                    return method.getAnnotation(DataPermission.class);
                }
            }
            // Check class-level annotation
            if (clazz.isAnnotationPresent(DataPermission.class)) {
                return clazz.getAnnotation(DataPermission.class);
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
```

- [ ] **Step 3: 创建 DataScopeService 接口**

This interface lives in knowledge-core so the interceptor can reference it. Implementation will be in knowledge-service.

```java
package com.knowledge.common.annotation;

import com.knowledge.common.annotation.DataPermission;

public interface DataScopeService {
    /** 构建数据权限 SQL 片段 (不含 WHERE) */
    String buildDataScopeSql(DataPermission annotation);
}
```

- [ ] **Step 4: 创建 MyBatis-Plus 分页/插件配置类**

Create in knowledge-core config package:

```java
package com.knowledge.common.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.knowledge.common.annotation.DataScopeService;
import com.knowledge.common.interceptor.DataPermissionInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(DataScopeService dataScopeService) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new DataPermissionInterceptor(dataScopeService));
        return interceptor;
    }
}
```

- [ ] **Step 5: 提交**

```bash
git add knowledge-core/src/main/java/com/knowledge/common/
git commit -m "feat: add DataPermission annotation and interceptor"
```

---

### Task 6: 创建 JWT 工具类和 Security UserDetails

**Files:**
- Create: `knowledge-service/src/main/java/com/knowledge/auth/security/JwtUtils.java`
- Create: `knowledge-service/src/main/java/com/knowledge/auth/security/LoginUserDetails.java`

- [ ] **Step 1: 创建 LoginUserDetails**

```java
package com.knowledge.auth.security;

import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class LoginUserDetails implements UserDetails {
    private Long userId;
    private Long accountId;
    private String username;
    private String password;
    private String userName;
    private Integer isSuperAdmin;
    private List<String> permissions;
    private List<Long> deptIds;
    private List<Long> regionIds;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (isSuperAdmin == 1) {
            return List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        }
        return permissions.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public boolean isAccountNonExpired() { return true; }
    @Override
    public boolean isAccountNonLocked() { return true; }
    @Override
    public boolean isCredentialsNonExpired() { return true; }
    @Override
    public boolean isEnabled() { return true; }
}
```

- [ ] **Step 2: 创建 JwtUtils**

```java
package com.knowledge.auth.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtils {
    @Value("${jwt.secret:knowledge-qa-jwt-secret-key-2024-spring-boot-3}")
    private String secret;

    @Value("${jwt.access-token-expire:7200000}")
    private long accessTokenExpire;

    @Value("${jwt.refresh-token-expire:604800000}")
    private long refreshTokenExpire;

    private SecretKey key;

    @PostConstruct
    public void init() {
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, Long accountId, String username, List<String> permissions) {
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("accountId", accountId)
                .claim("permissions", permissions)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpire))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(Long accountId) {
        return Jwts.builder()
                .subject(String.valueOf(accountId))
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshTokenExpire))
                .signWith(key)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
```

- [ ] **Step 3: 创建 UserDetailsServiceImpl 实现 UserDetailsService**

```java
package com.knowledge.auth.security;

import com.knowledge.auth.entity.*;
import com.knowledge.auth.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final SysAccountMapper accountMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysMenuMapper menuMapper;
    private final SysUserDeptMapper userDeptMapper;
    private final SysUserRegionMapper userRegionMapper;
    private final SysRoleMapper roleMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysAccount account = accountMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysAccount>()
                        .eq(SysAccount::getUsername, username)
                        .eq(SysAccount::getStatus, 1)
        );
        if (account == null) {
            throw new UsernameNotFoundException("账号不存在或已禁用");
        }

        SysUser user = userMapper.selectById(account.getUserId());
        if (user == null || user.getStatus() == 0) {
            throw new UsernameNotFoundException("用户不存在或已禁用");
        }

        // 获取用户部门
        List<SysUserDept> userDepts = userDeptMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUserDept>()
                        .eq(SysUserDept::getUserId, user.getId())
        );
        List<Long> deptIds = userDepts.stream().map(SysUserDept::getDeptId).collect(Collectors.toList());

        // 获取用户地区
        List<SysUserRegion> userRegions = userRegionMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUserRegion>()
                        .eq(SysUserRegion::getUserId, user.getId())
        );
        List<Long> regionIds = userRegions.stream().map(SysUserRegion::getRegionId).collect(Collectors.toList());

        // 获取权限列表
        List<String> permissions = new ArrayList<>();
        if (account.getIsSuperAdmin() != 1) {
            List<SysUserRole> userRoles = userRoleMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUserRole>()
                            .eq(SysUserRole::getUserId, user.getId())
            );
            List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
            if (!roleIds.isEmpty()) {
                List<SysRoleMenu> roleMenus = roleMenuMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRoleMenu>()
                                .in(SysRoleMenu::getRoleId, roleIds)
                );
                List<Long> menuIds = roleMenus.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toList());
                if (!menuIds.isEmpty()) {
                    List<SysMenu> menus = menuMapper.selectList(
                            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysMenu>()
                                    .in(SysMenu::getId, menuIds)
                                    .isNotNull(SysMenu::getPermission)
                    );
                    permissions = menus.stream().map(SysMenu::getPermission)
                            .filter(p -> p != null && !p.isEmpty())
                            .collect(Collectors.toList());
                }
            }
        }

        return LoginUserDetails.builder()
                .userId(user.getId())
                .accountId(account.getId())
                .username(account.getUsername())
                .password(account.getPassword())
                .userName(user.getName())
                .isSuperAdmin(account.getIsSuperAdmin())
                .permissions(permissions)
                .deptIds(deptIds)
                .regionIds(regionIds)
                .build();
    }
}
```

Note: This class references mappers from knowledge-core. Ensure the `@MapperScan` update from Task 4 includes `com.knowledge.auth.mapper` so these mappers can be injected.

- [ ] **Step 4: 提交**

```bash
git add knowledge-service/src/main/java/com/knowledge/auth/security/
git commit -m "feat: add JWT utils and UserDetailsService"
```

---

### Task 7: 创建数据权限服务实现

**Files:**
- Create: `knowledge-service/src/main/java/com/knowledge/auth/permission/DataScopeServiceImpl.java`

- [ ] **Step 1: 创建 DataScopeServiceImpl**

```java
package com.knowledge.auth.permission;

import com.knowledge.auth.entity.*;
import com.knowledge.auth.enums.DataScopeDeptEnum;
import com.knowledge.auth.enums.DataScopeRegionEnum;
import com.knowledge.auth.mapper.*;
import com.knowledge.auth.security.LoginUserDetails;
import com.knowledge.common.annotation.DataPermission;
import com.knowledge.common.annotation.DataScopeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DataScopeServiceImpl implements DataScopeService {

    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysRoleRegionMapper roleRegionMapper;
    private final SysDeptMapper deptMapper;
    private final SysUserDeptMapper userDeptMapper;
    private final SysUserRegionMapper userRegionMapper;

    @Override
    public String buildDataScopeSql(DataPermission annotation) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof LoginUserDetails user)) {
            return "";
        }

        // 超级管理员不限制
        if (user.getIsSuperAdmin() == 1) {
            return "";
        }

        String alias = annotation.tableAlias();
        String prefix = StringUtils.hasText(alias) ? alias + "." : "";
        List<String> conditions = new ArrayList<>();

        // 部门数据权限
        if (annotation.enableDept()) {
            String deptCondition = buildDeptCondition(user, annotation, prefix);
            if (StringUtils.hasText(deptCondition)) {
                conditions.add(deptCondition);
            }
        }

        // 地区数据权限
        if (annotation.enableRegion()) {
            String regionCondition = buildRegionCondition(user, annotation, prefix);
            if (StringUtils.hasText(regionCondition)) {
                conditions.add(regionCondition);
            }
        }

        if (conditions.isEmpty()) {
            return "";
        }
        return String.join(" AND ", conditions);
    }

    private String buildDeptCondition(LoginUserDetails user, DataPermission annotation, String prefix) {
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, user.getUserId())
        );
        if (userRoles.isEmpty()) return "";

        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        List<SysRole> roles = roleMapper.selectBatchIds(roleIds);

        // 如果任一角色是"全部数据权限"，不限制部门
        boolean hasAllDept = roles.stream().anyMatch(r -> r.getDeptDataScope() == DataScopeDeptEnum.ALL.getValue());
        if (hasAllDept) return "";

        // 取范围最小的作为最终权限
        int minScope = roles.stream()
                .mapToInt(SysRole::getDeptDataScope)
                .max().orElse(DataScopeDeptEnum.SELF.getValue());

        Set<Long> deptIds = new HashSet<>();
        switch (minScope) {
            case 2: // 自定义
                List<SysRoleDept> roleDepts = roleDeptMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRoleDept>()
                                .in(SysRoleDept::getRoleId, roleIds)
                );
                deptIds.addAll(roleDepts.stream().map(SysRoleDept::getDeptId).collect(Collectors.toSet()));
                break;
            case 3: // 本部门及下属
                deptIds.addAll(getDeptAndChildren(user.getDeptIds()));
                break;
            case 4: // 本部门
                deptIds.addAll(user.getDeptIds());
                break;
            case 5: // 本人
                return prefix + annotation.createByField() + " = '" + user.getUsername() + "'";
        }

        if (deptIds.isEmpty()) return "1=0"; // 无权限
        return prefix + annotation.deptIdField() + " IN (" +
                deptIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")";
    }

    private String buildRegionCondition(LoginUserDetails user, DataPermission annotation, String prefix) {
        List<SysUserRole> userRoles = userRoleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, user.getUserId())
        );
        if (userRoles.isEmpty()) return "";

        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        List<SysRole> roles = roleMapper.selectBatchIds(roleIds);

        boolean hasAllRegion = roles.stream().anyMatch(r -> r.getRegionDataScope() == DataScopeRegionEnum.ALL.getValue());
        if (hasAllRegion) return "";

        int minScope = roles.stream()
                .mapToInt(SysRole::getRegionDataScope)
                .max().orElse(DataScopeRegionEnum.USER_REGION.getValue());

        Set<Long> regionIds = new HashSet<>();
        switch (minScope) {
            case 2: // 自定义
                List<SysRoleRegion> roleRegions = roleRegionMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysRoleRegion>()
                                .in(SysRoleRegion::getRoleId, roleIds)
                );
                regionIds.addAll(roleRegions.stream().map(SysRoleRegion::getRegionId).collect(Collectors.toSet()));
                break;
            case 3: // 本用户所属地区
                regionIds.addAll(user.getRegionIds());
                break;
        }

        if (regionIds.isEmpty()) return "1=0";
        return prefix + annotation.regionIdField() + " IN (" +
                regionIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")";
    }

    private List<Long> getDeptAndChildren(List<Long> deptIds) {
        Set<Long> result = new HashSet<>(deptIds);
        List<SysDept> allDepts = deptMapper.selectList(null);
        for (Long deptId : deptIds) {
            collectChildren(allDepts, deptId, result);
        }
        return new ArrayList<>(result);
    }

    private void collectChildren(List<SysDept> allDepts, Long parentId, Set<Long> result) {
        allDepts.stream()
                .filter(d -> Objects.equals(d.getParentId(), parentId))
                .forEach(d -> {
                    result.add(d.getId());
                    collectChildren(allDepts, d.getId(), result);
                });
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add knowledge-service/src/main/java/com/knowledge/auth/permission/
git commit -m "feat: add data scope service implementation"
```

---

### Task 8: 创建基础 Service 类（Dept, Menu, Region）

**Files:**
- Create: `knowledge-service/src/main/java/com/knowledge/auth/service/SysDeptService.java`
- Create: `knowledge-service/src/main/java/com/knowledge/auth/service/SysMenuService.java`
- Create: `knowledge-service/src/main/java/com/knowledge/auth/service/SysRegionService.java`

- [ ] **Step 1: 创建 SysDeptService**

```java
package com.knowledge.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.auth.entity.SysDept;
import com.knowledge.auth.mapper.SysDeptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysDeptService {
    private final SysDeptMapper deptMapper;

    public List<SysDept> listAll() {
        return deptMapper.selectList(
                new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getSortOrder));
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
        // 检查是否有子部门
        long count = deptMapper.selectCount(
                new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
        if (count > 0) {
            throw new RuntimeException("存在子部门，无法删除");
        }
        deptMapper.deleteById(id);
    }
}
```

Note: Add a `children` field to `SysDept` entity (not a DB column, just transient):

```java
// Add to SysDept entity
@TableField(exist = false)
private List<SysDept> children = new ArrayList<>();
```

- [ ] **Step 2: 创建 SysMenuService**

```java
package com.knowledge.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.auth.entity.SysMenu;
import com.knowledge.auth.mapper.SysMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SysMenuService {
    private final SysMenuMapper menuMapper;

    public List<SysMenu> listAll() {
        return menuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>().orderByAsc(SysMenu::getSortOrder));
    }

    public List<SysMenu> listTree() {
        List<SysMenu> all = listAll();
        return buildTree(all, 0L);
    }

    public List<SysMenu> listByRoleId(Long roleId) {
        return menuMapper.selectMenuListByRoleId(roleId);
    }

    private List<SysMenu> buildTree(List<SysMenu> all, Long parentId) {
        List<SysMenu> children = all.stream()
                .filter(m -> Objects.equals(m.getParentId(), parentId))
                .collect(Collectors.toList());
        for (SysMenu menu : children) {
            menu.setChildren(buildTree(all, menu.getId()));
        }
        return children;
    }

    public SysMenu getById(Long id) {
        return menuMapper.selectById(id);
    }

    public void save(SysMenu menu) {
        menuMapper.insert(menu);
    }

    public void update(SysMenu menu) {
        menuMapper.updateById(menu);
    }

    public void delete(Long id) {
        long count = menuMapper.selectCount(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (count > 0) {
            throw new RuntimeException("存在子菜单，无法删除");
        }
        menuMapper.deleteById(id);
    }
}
```

Add children field to SysMenu:

```java
@TableField(exist = false)
private List<SysMenu> children = new ArrayList<>();
```

Add method to SysMenuMapper:

```java
@Select("SELECT m.* FROM sys_menu m " +
        "JOIN sys_role_menu rm ON m.id = rm.menu_id " +
        "WHERE rm.role_id = #{roleId} ORDER BY m.sort_order")
List<SysMenu> selectMenuListByRoleId(Long roleId);
```

- [ ] **Step 3: 创建 SysRegionService**

```java
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
        return regionMapper.selectList(
                new LambdaQueryWrapper<SysRegion>().orderByAsc(SysRegion::getId));
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
```

- [ ] **Step 4: 提交**

```bash
git add knowledge-service/src/main/java/com/knowledge/auth/service/SysDeptService.java knowledge-service/src/main/java/com/knowledge/auth/service/SysMenuService.java knowledge-service/src/main/java/com/knowledge/auth/service/SysRegionService.java knowledge-core/src/main/java/com/knowledge/auth/entity/SysDept.java knowledge-core/src/main/java/com/knowledge/auth/entity/SysMenu.java knowledge-core/src/main/java/com/knowledge/auth/mapper/SysMenuMapper.java
git commit -m "feat: add dept, menu, region services"
```

---

### Task 9: 创建 RoleService 和 AccountService

**Files:**
- Create: `knowledge-service/src/main/java/com/knowledge/auth/service/SysRoleService.java`
- Create: `knowledge-service/src/main/java/com/knowledge/auth/service/SysAccountService.java`

- [ ] **Step 1: 创建 SysRoleService**

```java
package com.knowledge.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.knowledge.auth.entity.*;
import com.knowledge.auth.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysRoleService {
    private final SysRoleMapper roleMapper;
    private final SysRoleMenuMapper roleMenuMapper;
    private final SysRoleDeptMapper roleDeptMapper;
    private final SysRoleRegionMapper roleRegionMapper;
    private final SysUserRoleMapper userRoleMapper;

    public List<SysRole> listAll() {
        return roleMapper.selectList(null);
    }

    public SysRole getById(Long id) {
        return roleMapper.selectById(id);
    }

    public void save(SysRole role) {
        roleMapper.insert(role);
    }

    public void update(SysRole role) {
        roleMapper.updateById(role);
    }

    @Transactional
    public void delete(Long id) {
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, id));
        roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, id));
        roleRegionMapper.delete(new LambdaQueryWrapper<SysRoleRegion>().eq(SysRoleRegion::getRoleId, id));
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, id));
        roleMapper.deleteById(id);
    }

    @Transactional
    public void assignMenus(Long roleId, List<Long> menuIds) {
        roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        for (Long menuId : menuIds) {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(roleId);
            rm.setMenuId(menuId);
            roleMenuMapper.insert(rm);
        }
    }

    @Transactional
    public void assignDepts(Long roleId, List<Long> deptIds) {
        roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, roleId));
        for (Long deptId : deptIds) {
            SysRoleDept rd = new SysRoleDept();
            rd.setRoleId(roleId);
            rd.setDeptId(deptId);
            roleDeptMapper.insert(rd);
        }
    }

    @Transactional
    public void assignRegions(Long roleId, List<Long> regionIds) {
        roleRegionMapper.delete(new LambdaQueryWrapper<SysRoleRegion>().eq(SysRoleRegion::getRoleId, roleId));
        for (Long regionId : regionIds) {
            SysRoleRegion rr = new SysRoleRegion();
            rr.setRoleId(roleId);
            rr.setRegionId(regionId);
            roleRegionMapper.insert(rr);
        }
    }

    public List<Long> getMenuIds(Long roleId) {
        return roleMenuMapper.selectList(
                new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId))
                .stream().map(SysRoleMenu::getMenuId).toList();
    }

    public List<Long> getDeptIds(Long roleId) {
        return roleDeptMapper.selectList(
                new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, roleId))
                .stream().map(SysRoleDept::getDeptId).toList();
    }

    public List<Long> getRegionIds(Long roleId) {
        return roleRegionMapper.selectList(
                new LambdaQueryWrapper<SysRoleRegion>().eq(SysRoleRegion::getRoleId, roleId))
                .stream().map(SysRoleRegion::getRegionId).toList();
    }
}
```

- [ ] **Step 2: 创建 SysAccountService**

```java
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
```

- [ ] **Step 3: 提交**

```bash
git add knowledge-service/src/main/java/com/knowledge/auth/service/SysRoleService.java knowledge-service/src/main/java/com/knowledge/auth/service/SysAccountService.java
git commit -m "feat: add role and account services"
```

---

### Task 10: 创建 SysUserService 和 AuthService

**Files:**
- Create: `knowledge-service/src/main/java/com/knowledge/auth/service/SysUserService.java`
- Create: `knowledge-service/src/main/java/com/knowledge/auth/service/AuthService.java`

- [ ] **Step 1: 创建 SysUserService**

```java
package com.knowledge.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.auth.entity.*;
import com.knowledge.auth.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SysUserService {
    private final SysUserMapper userMapper;
    private final SysUserDeptMapper userDeptMapper;
    private final SysUserRegionMapper userRegionMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysDeptMapper deptMapper;

    public Page<SysUser> page(int page, int size) {
        return userMapper.selectPage(new Page<>(page, size), null);
    }

    public SysUser getById(Long id) {
        return userMapper.selectById(id);
    }

    public void save(SysUser user) {
        userMapper.insert(user);
    }

    public void update(SysUser user) {
        userMapper.updateById(user);
    }

    @Transactional
    public void delete(Long id) {
        userDeptMapper.delete(new LambdaQueryWrapper<SysUserDept>().eq(SysUserDept::getUserId, id));
        userRegionMapper.delete(new LambdaQueryWrapper<SysUserRegion>().eq(SysUserRegion::getUserId, id));
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        userMapper.deleteById(id);
    }

    @Transactional
    public void assignDepts(Long userId, List<Long> deptIds, List<Long> leaderDeptIds) {
        userDeptMapper.delete(new LambdaQueryWrapper<SysUserDept>().eq(SysUserDept::getUserId, userId));
        for (Long deptId : deptIds) {
            SysUserDept ud = new SysUserDept();
            ud.setUserId(userId);
            ud.setDeptId(deptId);
            ud.setIsLeader(leaderDeptIds != null && leaderDeptIds.contains(deptId) ? 1 : 0);
            userDeptMapper.insert(ud);
        }
    }

    @Transactional
    public void assignRegions(Long userId, List<Long> regionIds) {
        userRegionMapper.delete(new LambdaQueryWrapper<SysUserRegion>().eq(SysUserRegion::getUserId, userId));
        for (Long regionId : regionIds) {
            SysUserRegion ur = new SysUserRegion();
            ur.setUserId(userId);
            ur.setRegionId(regionId);
            userRegionMapper.insert(ur);
        }
    }

    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        for (Long roleId : roleIds) {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleMapper.insert(ur);
        }
    }

    public List<Long> getDeptIds(Long userId) {
        return userDeptMapper.selectList(
                new LambdaQueryWrapper<SysUserDept>().eq(SysUserDept::getUserId, userId))
                .stream().map(SysUserDept::getDeptId).toList();
    }

    public List<Long> getRegionIds(Long userId) {
        return userRegionMapper.selectList(
                new LambdaQueryWrapper<SysUserRegion>().eq(SysUserRegion::getUserId, userId))
                .stream().map(SysUserRegion::getRegionId).toList();
    }

    public List<Long> getRoleIds(Long userId) {
        return userRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).toList();
    }
}
```

- [ ] **Step 2: 创建 AuthService**

```java
package com.knowledge.auth.service;

import com.knowledge.auth.dto.*;
import com.knowledge.auth.security.JwtUtils;
import com.knowledge.auth.security.LoginUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    public LoginResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        LoginUserDetails user = (LoginUserDetails) auth.getPrincipal();

        String accessToken = jwtUtils.generateAccessToken(
                user.getUserId(), user.getAccountId(),
                user.getUsername(), user.getPermissions());
        String refreshToken = jwtUtils.generateRefreshToken(user.getAccountId());

        LoginResponse resp = new LoginResponse();
        resp.setAccessToken(accessToken);
        resp.setRefreshToken(refreshToken);
        resp.setUserId(user.getUserId());
        resp.setUsername(user.getUsername());
        resp.setUserName(user.getUserName());
        resp.setIsSuperAdmin(user.getIsSuperAdmin());
        resp.setPermissions(user.getPermissions());
        return resp;
    }

    public LoginResponse refresh(RefreshTokenRequest request) {
        var claims = jwtUtils.parseToken(request.getRefreshToken());
        String type = claims.get("type", String.class);
        if (!"refresh".equals(type)) {
            throw new RuntimeException("无效的refresh token");
        }
        // 重新生成 token pair (简单实现: 通过 accountId 重新加载用户信息)
        // 实际应该从缓存中获取用户信息或者重新查询
        Long accountId = Long.valueOf(claims.getSubject());
        // 这里简单重新生成一对 token
        String newAccessToken = "re-issued"; // placeholder, 实际需要重新查询用户信息
        String newRefreshToken = jwtUtils.generateRefreshToken(accountId);
        LoginResponse resp = new LoginResponse();
        resp.setAccessToken(newAccessToken);
        resp.setRefreshToken(newRefreshToken);
        return resp;
    }
}
```

Note: The `LoginRequest`, `RefreshTokenRequest`, `LoginResponse` DTOs will be created in the API module (Task 11). This service references them from the API layer which violates layer conventions. Better approach: define these DTOs in `knowledge-api` and have `AuthService` return `LoginResponse`. Alternatively, move the DTOs to `knowledge-core` or accept the cross-layer reference.

Recommended: Keep DTOs in `knowledge-api` as they are request/response objects. AuthService in knowledge-service can reference them since knowledge-service depends on knowledge-api indirectly through runtime. Actually, in the current project structure, knowledge-api depends on knowledge-service, not the other way around.

So AuthService should use simple return types, and the controller handles DTO conversion. Let me adjust:

Actually, the simplest approach: place a `LoginUserInfo` POJO in knowledge-service:

```java
// In knowledge-service
@Data
@Builder
public class LoginResult {
    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String username;
    private String userName;
    private Integer isSuperAdmin;
    private List<String> permissions;
}
```

Then AuthService returns `LoginResult`, and the controller converts to `LoginResponse` DTO.

- [ ] **Step 3: 提交**

```bash
git add knowledge-service/src/main/java/com/knowledge/auth/service/SysUserService.java knowledge-service/src/main/java/com/knowledge/auth/service/AuthService.java
git commit -m "feat: add user service and auth service"
```

---

### Task 11: 创建 Spring Security 配置和 JWT 过滤器

**Files:**
- Create: `knowledge-api/src/main/java/com/knowledge/auth/security/SecurityConfig.java`
- Create: `knowledge-api/src/main/java/com/knowledge/auth/security/JwtAuthFilter.java`

- [ ] **Step 1: 创建 SecurityConfig**

```java
package com.knowledge.auth.security;

import com.knowledge.auth.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/auth/refresh").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
```

- [ ] **Step 2: 创建 JwtAuthFilter**

```java
package com.knowledge.auth.security;

import com.knowledge.auth.security.JwtUtils;
import com.knowledge.auth.security.LoginUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);
        if (token != null && jwtUtils.validateToken(token)) {
            var claims = jwtUtils.parseToken(token);
            String username = claims.getSubject();
            Long userId = claims.get("userId", Long.class);
            Long accountId = claims.get("accountId", Long.class);
            @SuppressWarnings("unchecked")
            List<String> permissions = claims.get("permissions", List.class);

            LoginUserDetails user = LoginUserDetails.builder()
                    .userId(userId)
                    .accountId(accountId)
                    .username(username)
                    .permissions(permissions)
                    .build();

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }
        return null;
    }
}
```

- [ ] **Step 3: 提交**

```bash
git add knowledge-api/src/main/java/com/knowledge/auth/security/
git commit -m "feat: add Spring Security config and JWT filter"
```

---

### Task 12: 创建 API DTO 类

**Files:**
- Create: `knowledge-api/src/main/java/com/knowledge/auth/dto/LoginRequest.java`
- Create: `knowledge-api/src/main/java/com/knowledge/auth/dto/RefreshTokenRequest.java`
- Create: `knowledge-api/src/main/java/com/knowledge/auth/dto/LoginResponse.java`
- Create: `knowledge-api/src/main/java/com/knowledge/auth/dto/R.java` (统一响应包装)

- [ ] **Step 1: 创建 LoginRequest**

```java
package com.knowledge.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;
    @NotBlank(message = "密码不能为空")
    private String password;
}
```

- [ ] **Step 2: 创建 RefreshTokenRequest**

```java
package com.knowledge.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshTokenRequest {
    @NotBlank(message = "refreshToken不能为空")
    private String refreshToken;
}
```

- [ ] **Step 3: 创建 LoginResponse**

```java
package com.knowledge.auth.dto;

import lombok.Data;
import java.util.List;

@Data
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String username;
    private String userName;
    private Integer isSuperAdmin;
    private List<String> permissions;
}
```

- [ ] **Step 4: 创建统一响应 R**

```java
package com.knowledge.auth.dto;

import lombok.Data;

@Data
public class R<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.code = 200;
        r.msg = "success";
        r.data = data;
        return r;
    }

    public static <T> R<T> ok() {
        return ok(null);
    }

    public static <T> R<T> fail(String msg) {
        R<T> r = new R<>();
        r.code = 500;
        r.msg = msg;
        return r;
    }

    public static <T> R<T> fail(int code, String msg) {
        R<T> r = new R<>();
        r.code = code;
        r.msg = msg;
        return r;
    }
}
```

- [ ] **Step 5: 提交**

```bash
git add knowledge-api/src/main/java/com/knowledge/auth/dto/
git commit -m "feat: add auth DTOs"
```

---

### Task 13: 创建所有 Controller

**Files:**
- Create: `knowledge-api/src/main/java/com/knowledge/auth/controller/AuthController.java`
- Create: `knowledge-api/src/main/java/com/knowledge/auth/controller/SysDeptController.java`
- Create: `knowledge-api/src/main/java/com/knowledge/auth/controller/SysMenuController.java`
- Create: `knowledge-api/src/main/java/com/knowledge/auth/controller/SysRegionController.java`
- Create: `knowledge-api/src/main/java/com/knowledge/auth/controller/SysRoleController.java`
- Create: `knowledge-api/src/main/java/com/knowledge/auth/controller/SysAccountController.java`
- Create: `knowledge-api/src/main/java/com/knowledge/auth/controller/SysUserController.java`

- [ ] **Step 1: 创建 AuthController**

```java
package com.knowledge.auth.controller;

import com.knowledge.auth.dto.*;
import com.knowledge.auth.security.LoginUserDetails;
import com.knowledge.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse resp = authService.login(request);
        return R.ok(resp);
    }

    @PostMapping("/refresh")
    public R<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse resp = authService.refresh(request);
        return R.ok(resp);
    }

    @GetMapping("/userinfo")
    public R<LoginResponse> userinfo(@AuthenticationPrincipal LoginUserDetails user) {
        LoginResponse resp = new LoginResponse();
        resp.setUserId(user.getUserId());
        resp.setUsername(user.getUsername());
        resp.setUserName(user.getUserName());
        resp.setIsSuperAdmin(user.getIsSuperAdmin());
        resp.setPermissions(user.getPermissions());
        return R.ok(resp);
    }

    @GetMapping("/menus")
    public R<List> menus(@AuthenticationPrincipal LoginUserDetails user) {
        // 返回当前用户可见的菜单树
        return R.ok(authService.getUserMenus(user));
    }
}
```

Note: `authService.getUserMenus()` needs to be added to `AuthService`. It should return the menu tree filtered by the user's permissions.

- [ ] **Step 2: 添加 getUserMenus 到 AuthService**

In SysMenuService:

```java
public List<SysMenu> getMenuByPermissions(List<String> permissions) {
    if (permissions == null || permissions.isEmpty()) return List.of();
    return menuMapper.selectList(
            new LambdaQueryWrapper<SysMenu>()
                    .in(SysMenu::getPermission, permissions)
                    .orderByAsc(SysMenu::getSortOrder)
    );
}

public List<SysMenu> getVisibleMenus() {
    return menuMapper.selectList(
            new LambdaQueryWrapper<SysMenu>()
                    .ne(SysMenu::getType, 2) // exclude buttons
                    .orderByAsc(SysMenu::getSortOrder));
}
```

- [ ] **Step 3: 创建 SysDeptController**

```java
package com.knowledge.auth.controller;

import com.knowledge.auth.dto.R;
import com.knowledge.auth.entity.SysDept;
import com.knowledge.auth.service.SysDeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system/dept")
@RequiredArgsConstructor
public class SysDeptController {
    private final SysDeptService deptService;

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:dept:list')")
    public R<List<SysDept>> tree() {
        return R.ok(deptService.listTree());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:list')")
    public R<SysDept> get(@PathVariable Long id) {
        return R.ok(deptService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:dept:add')")
    public R<Void> save(@RequestBody SysDept dept) {
        deptService.save(dept);
        return R.ok();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:dept:edit')")
    public R<Void> update(@RequestBody SysDept dept) {
        deptService.update(dept);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:delete')")
    public R<Void> delete(@PathVariable Long id) {
        deptService.delete(id);
        return R.ok();
    }

    @GetMapping("/list")
    @PreAuthorize("hasAuthority('system:dept:list')")
    public R<List<SysDept>> list() {
        return R.ok(deptService.listAll());
    }
}
```

- [ ] **Step 4: 创建 SysMenuController**

```java
@RestController
@RequestMapping("/api/system/menu")
@RequiredArgsConstructor
public class SysMenuController {
    private final SysMenuService menuService;

    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:menu:list')")
    public R<List<SysMenu>> tree() {
        return R.ok(menuService.listTree());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:list')")
    public R<SysMenu> get(@PathVariable Long id) {
        return R.ok(menuService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:menu:add')")
    public R<Void> save(@RequestBody SysMenu menu) {
        menuService.save(menu);
        return R.ok();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:menu:edit')")
    public R<Void> update(@RequestBody SysMenu menu) {
        menuService.update(menu);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:delete')")
    public R<Void> delete(@PathVariable Long id) {
        menuService.delete(id);
        return R.ok();
    }
}
```

- [ ] **Step 5: 创建 SysRegionController**

```java
@RestController
@RequestMapping("/api/system/region")
@RequiredArgsConstructor
public class SysRegionController {
    private final SysRegionService regionService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:region:list')")
    public R<List<SysRegion>> list() {
        return R.ok(regionService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:region:list')")
    public R<SysRegion> get(@PathVariable Long id) {
        return R.ok(regionService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:region:add')")
    public R<Void> save(@RequestBody SysRegion region) {
        regionService.save(region);
        return R.ok();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:region:edit')")
    public R<Void> update(@RequestBody SysRegion region) {
        regionService.update(region);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:region:delete')")
    public R<Void> delete(@PathVariable Long id) {
        regionService.delete(id);
        return R.ok();
    }
}
```

- [ ] **Step 6: 创建 SysRoleController**

```java
@RestController
@RequestMapping("/api/system/role")
@RequiredArgsConstructor
public class SysRoleController {
    private final SysRoleService roleService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:role:list')")
    public R<List<SysRole>> list() {
        return R.ok(roleService.listAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:list')")
    public R<SysRole> get(@PathVariable Long id) {
        return R.ok(roleService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:role:add')")
    public R<Void> save(@RequestBody SysRole role) {
        roleService.save(role);
        return R.ok();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:role:edit')")
    public R<Void> update(@RequestBody SysRole role) {
        roleService.update(role);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:delete')")
    public R<Void> delete(@PathVariable Long id) {
        roleService.delete(id);
        return R.ok();
    }

    @PostMapping("/{roleId}/menus")
    @PreAuthorize("hasAuthority('system:role:edit')")
    public R<Void> assignMenus(@PathVariable Long roleId, @RequestBody List<Long> menuIds) {
        roleService.assignMenus(roleId, menuIds);
        return R.ok();
    }

    @GetMapping("/{roleId}/menus")
    public R<List<Long>> getMenuIds(@PathVariable Long roleId) {
        return R.ok(roleService.getMenuIds(roleId));
    }

    @PostMapping("/{roleId}/depts")
    @PreAuthorize("hasAuthority('system:role:edit')")
    public R<Void> assignDepts(@PathVariable Long roleId, @RequestBody List<Long> deptIds) {
        roleService.assignDepts(roleId, deptIds);
        return R.ok();
    }

    @GetMapping("/{roleId}/depts")
    public R<List<Long>> getDeptIds(@PathVariable Long roleId) {
        return R.ok(roleService.getDeptIds(roleId));
    }

    @PostMapping("/{roleId}/regions")
    @PreAuthorize("hasAuthority('system:role:edit')")
    public R<Void> assignRegions(@PathVariable Long roleId, @RequestBody List<Long> regionIds) {
        roleService.assignRegions(roleId, regionIds);
        return R.ok();
    }

    @GetMapping("/{roleId}/regions")
    public R<List<Long>> getRegionIds(@PathVariable Long roleId) {
        return R.ok(roleService.getRegionIds(roleId));
    }
}
```

- [ ] **Step 7: 创建 SysAccountController**

```java
@RestController
@RequestMapping("/api/system/account")
@RequiredArgsConstructor
public class SysAccountController {
    private final SysAccountService accountService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:account:list')")
    public R<Page<SysAccount>> page(@RequestParam(defaultValue = "1") int page,
                                     @RequestParam(defaultValue = "10") int size) {
        return R.ok(accountService.page(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:account:list')")
    public R<SysAccount> get(@PathVariable Long id) {
        return R.ok(accountService.getById(id));
    }

    @GetMapping("/by-user/{userId}")
    public R<List<SysAccount>> listByUserId(@PathVariable Long userId) {
        return R.ok(accountService.listByUserId(userId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:account:add')")
    public R<Void> save(@RequestBody SysAccount account) {
        accountService.save(account);
        return R.ok();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:account:edit')")
    public R<Void> update(@RequestBody SysAccount account) {
        accountService.update(account);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:account:delete')")
    public R<Void> delete(@PathVariable Long id) {
        accountService.delete(id);
        return R.ok();
    }
}
```

- [ ] **Step 8: 创建 SysUserController**

```java
@RestController
@RequestMapping("/api/system/user")
@RequiredArgsConstructor
public class SysUserController {
    private final SysUserService userService;

    @GetMapping
    @PreAuthorize("hasAuthority('system:user:list')")
    public R<Page<SysUser>> page(@RequestParam(defaultValue = "1") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        return R.ok(userService.page(page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:list')")
    public R<SysUser> get(@PathVariable Long id) {
        return R.ok(userService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('system:user:add')")
    public R<Void> save(@RequestBody SysUser user) {
        userService.save(user);
        return R.ok();
    }

    @PutMapping
    @PreAuthorize("hasAuthority('system:user:edit')")
    public R<Void> update(@RequestBody SysUser user) {
        userService.update(user);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    public R<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return R.ok();
    }

    @PostMapping("/{userId}/depts")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public R<Void> assignDepts(@PathVariable Long userId, @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Long> deptIds = (List<Long>) body.get("deptIds");
        @SuppressWarnings("unchecked")
        List<Long> leaderDeptIds = (List<Long>) body.get("leaderDeptIds");
        userService.assignDepts(userId, deptIds, leaderDeptIds);
        return R.ok();
    }

    @GetMapping("/{userId}/depts")
    public R<List<Long>> getDeptIds(@PathVariable Long userId) {
        return R.ok(userService.getDeptIds(userId));
    }

    @PostMapping("/{userId}/regions")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public R<Void> assignRegions(@PathVariable Long userId, @RequestBody List<Long> regionIds) {
        userService.assignRegions(userId, regionIds);
        return R.ok();
    }

    @GetMapping("/{userId}/regions")
    public R<List<Long>> getRegionIds(@PathVariable Long userId) {
        return R.ok(userService.getRegionIds(userId));
    }

    @PostMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('system:user:edit')")
    public R<Void> assignRoles(@PathVariable Long userId, @RequestBody List<Long> roleIds) {
        userService.assignRoles(userId, roleIds);
        return R.ok();
    }

    @GetMapping("/{userId}/roles")
    public R<List<Long>> getRoleIds(@PathVariable Long userId) {
        return R.ok(userService.getRoleIds(userId));
    }
}
```

- [ ] **Step 9: 提交**

```bash
git add knowledge-api/src/main/java/com/knowledge/auth/controller/
git commit -m "feat: add all auth controllers"
```

---

### Task 14: 前端 — API 模块和 Pinia Store

**Files:**
- Create: `knowledge-ui/src/api/auth.js`
- Create: `knowledge-ui/src/api/user.js`
- Create: `knowledge-ui/src/api/account.js`
- Create: `knowledge-ui/src/api/role.js`
- Create: `knowledge-ui/src/api/dept.js`
- Create: `knowledge-ui/src/api/menu.js`
- Create: `knowledge-ui/src/api/region.js`
- Create: `knowledge-ui/src/stores/auth.js`

- [ ] **Step 1: 创建 auth.js API**

```javascript
import api from './request'

export function login(data) {
  return api.post('/api/auth/login', data)
}

export function refreshToken(refreshToken) {
  return api.post('/api/auth/refresh', { refreshToken })
}

export function getUserInfo() {
  return api.get('/api/auth/userinfo')
}

export function getUserMenus() {
  return api.get('/api/auth/menus')
}
```

- [ ] **Step 2: 创建 dept.js, menu.js, region.js**

```javascript
// dept.js
import api from './request'

export function getDeptTree() {
  return api.get('/api/system/dept/tree')
}

export function getDeptList() {
  return api.get('/api/system/dept/list')
}

export function getDept(id) {
  return api.get(`/api/system/dept/${id}`)
}

export function saveDept(data) {
  return api.post('/api/system/dept', data)
}

export function updateDept(data) {
  return api.put('/api/system/dept', data)
}

export function deleteDept(id) {
  return api.delete(`/api/system/dept/${id}`)
}
```

```javascript
// menu.js
import api from './request'

export function getMenuTree() {
  return api.get('/api/system/menu/tree')
}

export function getMenu(id) {
  return api.get(`/api/system/menu/${id}`)
}

export function saveMenu(data) {
  return api.post('/api/system/menu', data)
}

export function updateMenu(data) {
  return api.put('/api/system/menu', data)
}

export function deleteMenu(id) {
  return api.delete(`/api/system/menu/${id}`)
}
```

```javascript
// region.js
import api from './request'

export function getRegionList() {
  return api.get('/api/system/region')
}

export function getRegion(id) {
  return api.get(`/api/system/region/${id}`)
}

export function saveRegion(data) {
  return api.post('/api/system/region', data)
}

export function updateRegion(data) {
  return api.put('/api/system/region', data)
}

export function deleteRegion(id) {
  return api.delete(`/api/system/region/${id}`)
}
```

- [ ] **Step 3: 创建 role.js**

```javascript
import api from './request'

export function getRoleList() {
  return api.get('/api/system/role')
}

export function getRole(id) {
  return api.get(`/api/system/role/${id}`)
}

export function saveRole(data) {
  return api.post('/api/system/role', data)
}

export function updateRole(data) {
  return api.put('/api/system/role', data)
}

export function deleteRole(id) {
  return api.delete(`/api/system/role/${id}`)
}

export function assignRoleMenus(roleId, menuIds) {
  return api.post(`/api/system/role/${roleId}/menus`, menuIds)
}

export function getRoleMenuIds(roleId) {
  return api.get(`/api/system/role/${roleId}/menus`)
}

export function assignRoleDepts(roleId, deptIds) {
  return api.post(`/api/system/role/${roleId}/depts`, deptIds)
}

export function getRoleDeptIds(roleId) {
  return api.get(`/api/system/role/${roleId}/depts`)
}

export function assignRoleRegions(roleId, regionIds) {
  return api.post(`/api/system/role/${roleId}/regions`, regionIds)
}

export function getRoleRegionIds(roleId) {
  return api.get(`/api/system/role/${roleId}/regions`)
}
```

- [ ] **Step 4: 创建 user.js 和 account.js**

```javascript
// user.js
import api from './request'

export function getUserPage(page, size) {
  return api.get('/api/system/user', { params: { page, size } })
}

export function getUser(id) {
  return api.get(`/api/system/user/${id}`)
}

export function saveUser(data) {
  return api.post('/api/system/user', data)
}

export function updateUser(data) {
  return api.put('/api/system/user', data)
}

export function deleteUser(id) {
  return api.delete(`/api/system/user/${id}`)
}

export function assignUserDepts(userId, deptIds, leaderDeptIds) {
  return api.post(`/api/system/user/${userId}/depts`, { deptIds, leaderDeptIds })
}

export function getUserDeptIds(userId) {
  return api.get(`/api/system/user/${userId}/depts`)
}

export function assignUserRegions(userId, regionIds) {
  return api.post(`/api/system/user/${userId}/regions`, regionIds)
}

export function getUserRegionIds(userId) {
  return api.get(`/api/system/user/${userId}/regions`)
}

export function assignUserRoles(userId, roleIds) {
  return api.post(`/api/system/user/${userId}/roles`, roleIds)
}

export function getUserRoleIds(userId) {
  return api.get(`/api/system/user/${userId}/roles`)
}
```

```javascript
// account.js
import api from './request'

export function getAccountPage(page, size) {
  return api.get('/api/system/account', { params: { page, size } })
}

export function getAccount(id) {
  return api.get(`/api/system/account/${id}`)
}

export function getAccountsByUser(userId) {
  return api.get(`/api/system/account/by-user/${userId}`)
}

export function saveAccount(data) {
  return api.post('/api/system/account', data)
}

export function updateAccount(data) {
  return api.put('/api/system/account', data)
}

export function deleteAccount(id) {
  return api.delete(`/api/system/account/${id}`)
}
```

- [ ] **Step 5: 创建 auth Pinia store**

```javascript
// stores/auth.js
import { defineStore } from 'pinia'
import { login as loginApi, getUserInfo, getUserMenus } from '@/api/auth'
import router from '@/router'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('accessToken') || null,
    refreshToken: localStorage.getItem('refreshToken') || null,
    userInfo: null,
    permissions: [],
    menus: []
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    isSuperAdmin: (state) => state.userInfo?.isSuperAdmin === 1,
    hasPermission: (state) => (perm) => {
      if (state.userInfo?.isSuperAdmin === 1) return true
      return state.permissions.includes(perm)
    }
  },

  actions: {
    async login(credentials) {
      const res = await loginApi(credentials)
      this.token = res.data.accessToken
      this.refreshToken = res.data.refreshToken
      this.userInfo = res.data
      this.permissions = res.data.permissions || []
      localStorage.setItem('accessToken', res.data.accessToken)
      localStorage.setItem('refreshToken', res.data.refreshToken)
      return res.data
    },

    async loadUserInfo() {
      try {
        const res = await getUserInfo()
        this.userInfo = res.data
        this.permissions = res.data.permissions || []
        const menuRes = await getUserMenus()
        this.menus = menuRes.data || []
      } catch {
        this.logout()
      }
    },

    logout() {
      this.token = null
      this.refreshToken = null
      this.userInfo = null
      this.permissions = []
      this.menus = []
      localStorage.removeItem('accessToken')
      localStorage.removeItem('refreshToken')
      router.push('/login')
    }
  }
})
```

- [ ] **Step 6: 提交**

```bash
git add knowledge-ui/src/api/ knowledge-ui/src/stores/
git commit -m "feat: add frontend API modules and auth store"
```

---

### Task 15: 前端 — 登录页面

**Files:**
- Create: `knowledge-ui/src/views/login/LoginView.vue`

- [ ] **Step 1: 创建 LoginView.vue**

```vue
<template>
  <div class="login-container">
    <div class="login-card">
      <h2 class="login-title">知识库问答系统</h2>
      <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" size="large" prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" @click="handleLogin" class="login-btn">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.login(form)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e) {
    ElMessage.error(e.response?.data?.msg || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
}

.login-card {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.login-title {
  text-align: center;
  margin-bottom: 30px;
  color: #1a1a2e;
  font-size: 24px;
}

.login-btn {
  width: 100%;
}
</style>
```

- [ ] **Step 2: 提交**

```bash
git add knowledge-ui/src/views/login/
git commit -m "feat: add login page"
```

---

### Task 16: 前端 — 系统管理页面（Dept, Menu, Region）

**Files:**
- Create: `knowledge-ui/src/views/system/DeptView.vue`
- Create: `knowledge-ui/src/views/system/MenuView.vue`
- Create: `knowledge-ui/src/views/system/RegionView.vue`

- [ ] **Step 1: 创建 DeptView.vue**

Department management with tree table:

```vue
<template>
  <div class="system-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>部门管理</span>
          <el-button type="primary" size="small" @click="handleAdd">新增部门</el-button>
        </div>
      </template>

      <el-table :data="deptTree" row-key="id" default-expand-all border>
        <el-table-column prop="name" label="部门名称" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑部门' : '新增部门'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="上级部门" prop="parentId">
          <el-tree-select v-model="form.parentId" :data="deptTree" :props="{ label: 'name', value: 'id' }" placeholder="请选择上级部门" clearable />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getDeptTree, getDept, saveDept, updateDept, deleteDept } from '@/api/dept'
import { ElMessage, ElMessageBox } from 'element-plus'

const deptTree = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = ref({ name: '', parentId: null, sortOrder: 0, status: 1 })
const rules = { name: [{ required: true, message: '请输入部门名称', trigger: 'blur' }] }

const loadTree = async () => {
  const res = await getDeptTree()
  deptTree.value = res.data || []
}

const handleAdd = () => {
  isEdit.value = false
  form.value = { name: '', parentId: null, sortOrder: 0, status: 1 }
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  isEdit.value = true
  const res = await getDept(row.id)
  form.value = { ...res.data }
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确认删除该部门？', '提示')
  await deleteDept(row.id)
  ElMessage.success('删除成功')
  loadTree()
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (isEdit.value) {
    await updateDept(form.value)
  } else {
    await saveDept(form.value)
  }
  ElMessage.success(isEdit.value ? '更新成功' : '新增成功')
  dialogVisible.value = false
  loadTree()
}

onMounted(loadTree)
</script>
```

- [ ] **Step 2: 创建 MenuView.vue**

Similar to DeptView but for menus (with type selection and permission field). Pattern follows the same tree-table + dialog structure.

- [ ] **Step 3: 创建 RegionView.vue**

Simple table CRUD for regions (no tree structure needed):

```vue
<template>
  <div class="system-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>地区管理</span>
          <el-button type="primary" size="small" @click="handleAdd">新增地区</el-button>
        </div>
      </template>

      <el-table :data="regionList" border>
        <el-table-column prop="name" label="地区名称" />
        <el-table-column prop="code" label="地区编码" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑地区' : '新增地区'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="地区名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="地区编码" prop="code">
          <el-input v-model="form.code" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getRegionList, getRegion, saveRegion, updateRegion, deleteRegion } from '@/api/region'
import { ElMessage, ElMessageBox } from 'element-plus'

const regionList = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = ref({ name: '', code: '' })
const rules = { name: [{ required: true, message: '请输入地区名称', trigger: 'blur' }] }

const loadList = async () => {
  const res = await getRegionList()
  regionList.value = res.data || []
}

const handleAdd = () => {
  isEdit.value = false
  form.value = { name: '', code: '' }
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  isEdit.value = true
  const res = await getRegion(row.id)
  form.value = { ...res.data }
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确认删除该地区？', '提示')
  await deleteRegion(row.id)
  ElMessage.success('删除成功')
  loadList()
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (isEdit.value) {
    await updateRegion(form.value)
  } else {
    await saveRegion(form.value)
  }
  ElMessage.success(isEdit.value ? '更新成功' : '新增成功')
  dialogVisible.value = false
  loadList()
}

onMounted(loadList)
</script>
```

- [ ] **Step 4: 提交**

```bash
git add knowledge-ui/src/views/system/DeptView.vue knowledge-ui/src/views/system/MenuView.vue knowledge-ui/src/views/system/RegionView.vue
git commit -m "feat: add dept, menu, region management pages"
```

---

### Task 17: 前端 — 系统管理页面（Role, Account, User）

**Files:**
- Create: `knowledge-ui/src/views/system/RoleView.vue`
- Create: `knowledge-ui/src/views/system/AccountView.vue`
- Create: `knowledge-ui/src/views/system/UserView.vue`

- [ ] **Step 1: 创建 RoleView.vue**

Role management with menu/dept/region assignment tabs:

```vue
<template>
  <div class="system-view">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>角色管理</span>
          <el-button type="primary" size="small" @click="handleAdd">新增角色</el-button>
        </div>
      </template>

      <el-table :data="roleList" border>
        <el-table-column prop="name" label="角色名称" />
        <el-table-column prop="code" label="角色编码" />
        <el-table-column label="部门数据权限" width="150">
          <template #default="{ row }">
            {{ deptScopeMap[row.deptDataScope] || '未知' }}
          </template>
        </el-table-column>
        <el-table-column label="地区数据权限" width="150">
          <template #default="{ row }">
            {{ regionScopeMap[row.regionDataScope] || '未知' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="success" link size="small" @click="handlePermission(row)">权限配置</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 角色编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑角色' : '新增角色'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model="form.code" />
        </el-form-item>
        <el-form-item label="部门数据权限" prop="deptDataScope">
          <el-select v-model="form.deptDataScope">
            <el-option :value="1" label="全部数据" />
            <el-option :value="2" label="自定义部门" />
            <el-option :value="3" label="本部门及下属" />
            <el-option :value="4" label="本部门" />
            <el-option :value="5" label="本人" />
          </el-select>
        </el-form-item>
        <el-form-item label="地区数据权限" prop="regionDataScope">
          <el-select v-model="form.regionDataScope">
            <el-option :value="1" label="全部地区" />
            <el-option :value="2" label="自定义地区" />
            <el-option :value="3" label="本用户所属地区" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 权限配置对话框 -->
    <el-dialog v-model="permDialogVisible" title="权限配置" width="700px">
      <el-tabs>
        <el-tab-pane label="菜单权限">
          <el-tree ref="menuTreeRef" :data="menuTree" show-checkbox node-key="id"
            :props="{ label: 'name', children: 'children' }" default-expand-all />
        </el-tab-pane>
        <el-tab-pane label="部门数据权限">
          <el-tree ref="deptTreeRef" :data="deptTree" show-checkbox node-key="id"
            :props="{ label: 'name', children: 'children' }" default-expand-all />
        </el-tab-pane>
        <el-tab-pane label="地区数据权限">
          <el-select v-model="selectedRegions" multiple placeholder="选择地区" style="width: 100%">
            <el-option v-for="r in regionList" :key="r.id" :label="r.name" :value="r.id" />
          </el-select>
        </el-tab-pane>
      </el-tabs>
      <template #footer>
        <el-button @click="permDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSavePerm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { getRoleList, getRole, saveRole, updateRole, deleteRole,
  assignRoleMenus, getRoleMenuIds, assignRoleDepts, getRoleDeptIds,
  assignRoleRegions, getRoleRegionIds } from '@/api/role'
import { getMenuTree } from '@/api/menu'
import { getDeptTree } from '@/api/dept'
import { getRegionList } from '@/api/region'
import { ElMessage, ElMessageBox } from 'element-plus'

const roleList = ref([])
const menuTree = ref([])
const deptTree = ref([])
const regionList = ref([])
const dialogVisible = ref(false)
const permDialogVisible = ref(false)
const isEdit = ref(false)
const currentRoleId = ref(null)
const menuTreeRef = ref(null)
const deptTreeRef = ref(null)
const selectedRegions = ref([])
const formRef = ref(null)
const form = ref({ name: '', code: '', deptDataScope: 3, regionDataScope: 1, status: 1 })
const rules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }]
}

const deptScopeMap = { 1: '全部数据', 2: '自定义部门', 3: '本部门及下属', 4: '本部门', 5: '本人' }
const regionScopeMap = { 1: '全部地区', 2: '自定义地区', 3: '本用户所属地区' }

const loadData = async () => {
  const [roleRes, menuRes, deptRes, regionRes] = await Promise.all([
    getRoleList(), getMenuTree(), getDeptTree(), getRegionList()
  ])
  roleList.value = roleRes.data || []
  menuTree.value = menuRes.data || []
  deptTree.value = deptRes.data || []
  regionList.value = regionRes.data || []
}

const handleAdd = () => {
  isEdit.value = false
  form.value = { name: '', code: '', deptDataScope: 3, regionDataScope: 1, status: 1 }
  dialogVisible.value = true
}

const handleEdit = async (row) => {
  isEdit.value = true
  const res = await getRole(row.id)
  form.value = { ...res.data }
  dialogVisible.value = true
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确认删除该角色？', '提示')
  await deleteRole(row.id)
  ElMessage.success('删除成功')
  loadData()
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (isEdit.value) {
    await updateRole(form.value)
  } else {
    await saveRole(form.value)
  }
  ElMessage.success(isEdit.value ? '更新成功' : '新增成功')
  dialogVisible.value = false
  loadData()
}

const handlePermission = async (row) => {
  currentRoleId.value = row.id
  permDialogVisible.value = true
  await loadPermissionData(row.id)
}

const loadPermissionData = async (roleId) => {
  const [menuIds, deptIds, regionIds] = await Promise.all([
    getRoleMenuIds(roleId), getRoleDeptIds(roleId), getRoleRegionIds(roleId)
  ])
  // Tree components need nextTick to be ready
  setTimeout(() => {
    menuTreeRef.value?.setCheckedKeys(menuIds.data || [])
    deptTreeRef.value?.setCheckedKeys(deptIds.data || [])
  }, 100)
  selectedRegions.value = regionIds.data || []
}

const handleSavePerm = async () => {
  if (!currentRoleId.value) return
  await Promise.all([
    assignRoleMenus(currentRoleId.value, menuTreeRef.value?.getCheckedKeys() || []),
    assignRoleDepts(currentRoleId.value, deptTreeRef.value?.getCheckedKeys() || []),
    assignRoleRegions(currentRoleId.value, selectedRegions.value)
  ])
  ElMessage.success('权限配置保存成功')
  permDialogVisible.value = false
}

onMounted(loadData)
</script>
```

- [ ] **Step 2: 创建 AccountView.vue** (simple page with user selection, similar pattern)

- [ ] **Step 3: 创建 UserView.vue** (user page with dept/region/role assignment in dialog)

- [ ] **Step 4: 提交**

```bash
git add knowledge-ui/src/views/system/RoleView.vue knowledge-ui/src/views/system/AccountView.vue knowledge-ui/src/views/system/UserView.vue
git commit -m "feat: add role, account, user management pages"
```

---

### Task 18: 前端 — 路由守卫、权限指令和布局改造

**Files:**
- Modify: `knowledge-ui/src/router/index.js`
- Create: `knowledge-ui/src/directives/permission.js`
- Modify: `knowledge-ui/src/App.vue`

- [ ] **Step 1: 更新路由配置**

```javascript
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue')
  },
  {
    path: '/',
    redirect: '/chat'
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('@/views/ChatView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/documents',
    name: 'Documents',
    component: () => import('@/views/DocumentsView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/system',
    redirect: '/system/user',
    meta: { requiresAuth: true }
  },
  {
    path: '/system/user',
    name: 'User',
    component: () => import('@/views/system/UserView.vue'),
    meta: { requiresAuth: true, permission: 'system:user:list' }
  },
  {
    path: '/system/account',
    name: 'Account',
    component: () => import('@/views/system/AccountView.vue'),
    meta: { requiresAuth: true, permission: 'system:account:list' }
  },
  {
    path: '/system/role',
    name: 'Role',
    component: () => import('@/views/system/RoleView.vue'),
    meta: { requiresAuth: true, permission: 'system:role:list' }
  },
  {
    path: '/system/dept',
    name: 'Dept',
    component: () => import('@/views/system/DeptView.vue'),
    meta: { requiresAuth: true, permission: 'system:dept:list' }
  },
  {
    path: '/system/menu',
    name: 'Menu',
    component: () => import('@/views/system/MenuView.vue'),
    meta: { requiresAuth: true, permission: 'system:menu:list' }
  },
  {
    path: '/system/region',
    name: 'Region',
    component: () => import('@/views/system/RegionView.vue'),
    meta: { requiresAuth: true, permission: 'system:region:list' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Navigation guard
router.beforeEach(async (to, from, next) => {
  const token = localStorage.getItem('accessToken')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
```

- [ ] **Step 2: 创建 permission 指令**

```javascript
// directives/permission.js
import { useAuthStore } from '@/stores/auth'

export default {
  mounted(el, binding) {
    const authStore = useAuthStore()
    const permission = binding.value
    if (permission && !authStore.hasPermission(permission)) {
      el.parentNode?.removeChild(el)
    }
  }
}
```

Register it in main.js:

```javascript
// In main.js, add:
import permission from '@/directives/permission'
app.directive('permission', permission)
```

- [ ] **Step 3: 更新 App.vue**

Add login check and dynamic menu rendering from auth store:

```vue
<template>
  <el-config-provider :locale="zhCn">
    <router-view v-if="!isLoggedIn" />
    <el-container v-else class="app-container">
      <el-aside width="220px" class="sidebar">
        <div class="logo">
          <el-icon><Reading /></el-icon>
          <span>知识库问答</span>
        </div>
        <el-menu
          :default-active="activeMenu"
          class="sidebar-menu"
          router
        >
          <el-menu-item index="/chat">
            <el-icon><ChatDotRound /></el-icon>
            <span>智能问答</span>
          </el-menu-item>
          <el-menu-item index="/documents">
            <el-icon><Document /></el-icon>
            <span>文档管理</span>
          </el-menu-item>
          <el-sub-menu index="/system" v-if="hasSystemAccess">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>系统管理</span>
            </template>
            <el-menu-item index="/system/user" v-if="hasPerm('system:user:list')">用户管理</el-menu-item>
            <el-menu-item index="/system/account" v-if="hasPerm('system:account:list')">账号管理</el-menu-item>
            <el-menu-item index="/system/role" v-if="hasPerm('system:role:list')">角色管理</el-menu-item>
            <el-menu-item index="/system/dept" v-if="hasPerm('system:dept:list')">部门管理</el-menu-item>
            <el-menu-item index="/system/menu" v-if="hasPerm('system:menu:list')">菜单管理</el-menu-item>
            <el-menu-item index="/system/region" v-if="hasPerm('system:region:list')">地区管理</el-menu-item>
          </el-sub-menu>
        </el-menu>
        <div class="sidebar-footer">
          <span>{{ userName }}</span>
          <el-button type="danger" link size="small" @click="handleLogout">退出</el-button>
        </div>
      </el-aside>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-config-provider>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isLoggedIn = computed(() => !authStore.isLoggedIn)
const activeMenu = computed(() => route.path)
const userName = computed(() => authStore.userInfo?.userName || '')
const hasSystemAccess = computed(() =>
  authStore.isSuperAdmin ||
  ['system:user:list','system:account:list','system:role:list',
   'system:dept:list','system:menu:list','system:region:list']
    .some(p => authStore.hasPermission(p))
)

const hasPerm = (perm) => authStore.hasPermission(perm)

const handleLogout = () => {
  authStore.logout()
}

onMounted(async () => {
  if (authStore.isLoggedIn && !authStore.userInfo) {
    await authStore.loadUserInfo()
  }
})
</script>
```

Note: The `v-if="!isLoggedIn"` logic needs careful handling. A simpler approach:

```vue
<template>
  <el-config-provider :locale="zhCn">
    <!-- Login page: no sidebar -->
    <router-view v-if="isLoginPage" />
    <!-- Main app: with sidebar -->
    <el-container v-else-if="authStore.isLoggedIn" class="app-container">
      ...
    </el-container>
  </el-config-provider>
</template>

<script setup>
const isLoginPage = computed(() => route.path === '/login')
</script>
```

- [ ] **Step 4: 提交**

```bash
git add knowledge-ui/src/router/index.js knowledge-ui/src/directives/ knowledge-ui/src/App.vue knowledge-ui/src/main.js
git commit -m "feat: add route guard, permission directive, and layout auth integration"
```

---

### Task 19: 知识库集成 — 文档关联部门和地区

**Files:**
- Modify: `knowledge-core/src/main/java/com/knowledge/entity/KnowledgeDocument.java`
- Modify: `knowledge-api/src/main/java/com/knowledge/controller/DocumentController.java`
- Modify: `knowledge-service/src/main/java/com/knowledge/service/KnowledgeDocumentService.java`
- Modify: `knowledge-service/src/main/java/com/knowledge/service/DocumentProcessService.java`
- Modify: `knowledge-ui/src/views/DocumentsView.vue`
- Modify: `knowledge-api/src/main/resources/db/schema.sql`

- [ ] **Step 1: 给 knowledge_document 表新增字段**

```sql
ALTER TABLE knowledge_document
    ADD COLUMN dept_id BIGINT COMMENT '所属部门ID',
    ADD COLUMN region_id BIGINT COMMENT '所属地区ID',
    ADD COLUMN create_by VARCHAR(100) COMMENT '创建人';
```

- [ ] **Step 2: 更新 KnowledgeDocument 实体**

Add fields:
```java
private Long deptId;
private Long regionId;
private String createBy;
```

- [ ] **Step 3: 更新上传 DTO**

```java
// In DocumentController or a new DTO
@Data
public class DocumentUploadRequest {
    private String title;
    private Long deptId;
    private Long regionId;
}
```

- [ ] **Step 4: 更新 DocumentController.upload()**

Accept `deptId` and `regionId` as request params or part of multipart request, pass them to the service layer.

- [ ] **Step 5: 更新 DocumentsView.vue**

Add dept and region columns to the document table. Add department and region selectors to the upload dialog.

- [ ] **Step 6: 给 Mapper 添加 @DataPermission 注解**

```java
@com.knowledge.common.annotation.DataPermission(tableAlias = "kd")
@Mapper
public interface KnowledgeDocumentMapper extends BaseMapper<KnowledgeDocument> {
}
```

- [ ] **Step 7: 提交**

```bash
git add knowledge-core/src/main/java/com/knowledge/entity/KnowledgeDocument.java knowledge-api/src/main/java/com/knowledge/controller/DocumentController.java knowledge-service/src/main/java/com/knowledge/service/ knowledge-ui/src/views/DocumentsView.vue knowledge-api/src/main/resources/db/schema.sql knowledge-core/src/main/java/com/knowledge/mapper/KnowledgeDocumentMapper.java
git commit -m "feat: integrate knowledge document with dept/region data permission"
```

---

### Task 20: 初始化超级管理员和测试数据

**Files:**
- Create: `knowledge-service/src/main/java/com/knowledge/auth/config/InitDataRunner.java`

- [ ] **Step 1: 创建启动初始化 Runner**

```java
package com.knowledge.auth.config;

import com.knowledge.auth.entity.*;
import com.knowledge.auth.mapper.*;
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
        // 检查是否已经初始化
        if (accountMapper.selectCount(null) > 0) {
            return;
        }

        log.info("======== 初始化系统数据 ========");

        // 创建超级管理员用户
        SysUser adminUser = new SysUser();
        adminUser.setName("管理员");
        adminUser.setStatus(1);
        userMapper.insert(adminUser);

        // 创建超级管理员账号
        SysAccount adminAccount = new SysAccount();
        adminAccount.setUserId(adminUser.getId());
        adminAccount.setUsername("admin");
        adminAccount.setPassword(passwordEncoder.encode("admin123"));
        adminAccount.setIsSuperAdmin(1);
        adminAccount.setStatus(1);
        accountMapper.insert(adminAccount);

        log.info("超级管理员账号创建成功: admin / admin123");

        // 创建测试部门
        // Note: This requires SysDeptMapper; add it as field

        log.info("======== 系统数据初始化完成 ========");
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add knowledge-service/src/main/java/com/knowledge/auth/config/
git commit -m "feat: add system init data runner"
```
