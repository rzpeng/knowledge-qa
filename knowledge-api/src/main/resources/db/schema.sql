-- 创建数据库
CREATE DATABASE IF NOT EXISTS knowledge_qa DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE knowledge_qa;

-- 知识文档表
CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    title VARCHAR(255) NOT NULL COMMENT '文档标题',
    file_name VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_type VARCHAR(20) NOT NULL COMMENT '文件类型',
    file_size BIGINT NOT NULL COMMENT '文件大小(字节)',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-处理中，1-成功，2-失败',
    chunk_count INT NOT NULL DEFAULT 0 COMMENT '切片数量',
    error_msg VARCHAR(500) COMMENT '错误信息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识文档表';

-- 文档切片表
CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    document_id BIGINT NOT NULL COMMENT '文档ID',
    content TEXT NOT NULL COMMENT '切片内容',
    chunk_index INT NOT NULL COMMENT '切片序号',
    vector_id VARCHAR(64) COMMENT 'Milvus向量ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_document_id (document_id),
    INDEX idx_vector_id (vector_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文档切片表';

-- 对话会话表
CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    title VARCHAR(255) NOT NULL COMMENT '会话标题',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话会话表';

-- 对话消息表
CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    session_id BIGINT NOT NULL COMMENT '会话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色：user/assistant',
    content TEXT NOT NULL COMMENT '消息内容',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_session_id (session_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话消息表';

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

-- 给知识文档表增加部门和地区字段
ALTER TABLE knowledge_document
    ADD COLUMN dept_id BIGINT COMMENT '所属部门ID',
    ADD COLUMN region_id BIGINT COMMENT '所属地区ID',
    ADD COLUMN create_by VARCHAR(100) COMMENT '创建人';

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
INSERT INTO sys_menu (name, parent_id, type, permission, sort_order)
SELECT '用户新增', id, 2, 'system:user:add', 1 FROM sys_menu WHERE permission = 'system:user:list';
INSERT INTO sys_menu (name, parent_id, type, permission, sort_order)
SELECT '用户编辑', id, 2, 'system:user:edit', 2 FROM sys_menu WHERE permission = 'system:user:list';
INSERT INTO sys_menu (name, parent_id, type, permission, sort_order)
SELECT '用户删除', id, 2, 'system:user:delete', 3 FROM sys_menu WHERE permission = 'system:user:list';

-- 账号管理按钮权限
INSERT INTO sys_menu (name, parent_id, type, permission, sort_order)
SELECT '账号新增', id, 2, 'system:account:add', 1 FROM sys_menu WHERE permission = 'system:account:list';
INSERT INTO sys_menu (name, parent_id, type, permission, sort_order)
SELECT '账号编辑', id, 2, 'system:account:edit', 2 FROM sys_menu WHERE permission = 'system:account:list';
INSERT INTO sys_menu (name, parent_id, type, permission, sort_order)
SELECT '账号删除', id, 2, 'system:account:delete', 3 FROM sys_menu WHERE permission = 'system:account:list';

-- 角色管理按钮权限
INSERT INTO sys_menu (name, parent_id, type, permission, sort_order)
SELECT '角色新增', id, 2, 'system:role:add', 1 FROM sys_menu WHERE permission = 'system:role:list';
INSERT INTO sys_menu (name, parent_id, type, permission, sort_order)
SELECT '角色编辑', id, 2, 'system:role:edit', 2 FROM sys_menu WHERE permission = 'system:role:list';
INSERT INTO sys_menu (name, parent_id, type, permission, sort_order)
SELECT '角色删除', id, 2, 'system:role:delete', 3 FROM sys_menu WHERE permission = 'system:role:list';

-- 部门管理按钮权限
INSERT INTO sys_menu (name, parent_id, type, permission, sort_order)
SELECT '部门新增', id, 2, 'system:dept:add', 1 FROM sys_menu WHERE permission = 'system:dept:list';
INSERT INTO sys_menu (name, parent_id, type, permission, sort_order)
SELECT '部门编辑', id, 2, 'system:dept:edit', 2 FROM sys_menu WHERE permission = 'system:dept:list';
INSERT INTO sys_menu (name, parent_id, type, permission, sort_order)
SELECT '部门删除', id, 2, 'system:dept:delete', 3 FROM sys_menu WHERE permission = 'system:dept:list';

-- 菜单管理按钮权限
INSERT INTO sys_menu (name, parent_id, type, permission, sort_order)
SELECT '菜单新增', id, 2, 'system:menu:add', 1 FROM sys_menu WHERE permission = 'system:menu:list';
INSERT INTO sys_menu (name, parent_id, type, permission, sort_order)
SELECT '菜单编辑', id, 2, 'system:menu:edit', 2 FROM sys_menu WHERE permission = 'system:menu:list';
INSERT INTO sys_menu (name, parent_id, type, permission, sort_order)
SELECT '菜单删除', id, 2, 'system:menu:delete', 3 FROM sys_menu WHERE permission = 'system:menu:list';

-- 地区管理按钮权限
INSERT INTO sys_menu (name, parent_id, type, permission, sort_order)
SELECT '地区新增', id, 2, 'system:region:add', 1 FROM sys_menu WHERE permission = 'system:region:list';
INSERT INTO sys_menu (name, parent_id, type, permission, sort_order)
SELECT '地区编辑', id, 2, 'system:region:edit', 2 FROM sys_menu WHERE permission = 'system:region:list';
INSERT INTO sys_menu (name, parent_id, type, permission, sort_order)
SELECT '地区删除', id, 2, 'system:region:delete', 3 FROM sys_menu WHERE permission = 'system:region:list';

-- ========== Agent 智能助手模块表结构 ==========

-- Agent 会话表
CREATE TABLE IF NOT EXISTS agent_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    title VARCHAR(255) NOT NULL COMMENT '会话标题',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent会话表';

-- Agent 消息表
CREATE TABLE IF NOT EXISTS agent_message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    session_id BIGINT NOT NULL COMMENT '会话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色：USER/ASSISTANT/TOOL',
    content TEXT COMMENT '消息内容',
    tool_name VARCHAR(100) COMMENT '工具名称',
    tool_args TEXT COMMENT '工具参数(JSON)',
    tool_result TEXT COMMENT '工具执行结果(JSON)',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_session_id (session_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent消息表';
