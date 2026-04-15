CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户主键',
    username VARCHAR(64) NOT NULL UNIQUE COMMENT '登录用户名',
    display_name VARCHAR(64) NOT NULL COMMENT '展示昵称',
    password_hash VARCHAR(255) NOT NULL COMMENT 'BCrypt 密码摘要',
    avatar_url VARCHAR(255) COMMENT '头像访问地址',
    created_at TIMESTAMP NOT NULL COMMENT '创建时间'
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT='用户表';

CREATE TABLE IF NOT EXISTS friendships (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '好友关系主键',
    owner_id BIGINT NOT NULL COMMENT '关系拥有者用户 ID',
    friend_id BIGINT NOT NULL COMMENT '好友用户 ID',
    friend_remark VARCHAR(200) COMMENT '拥有者对好友的备注',
    created_at TIMESTAMP NOT NULL COMMENT '建立好友关系时间',
    CONSTRAINT uk_friendships_owner_friend UNIQUE (owner_id, friend_id),
    CONSTRAINT fk_friendships_owner FOREIGN KEY (owner_id) REFERENCES users(id),
    CONSTRAINT fk_friendships_friend FOREIGN KEY (friend_id) REFERENCES users(id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT='好友关系表';

CREATE TABLE IF NOT EXISTS friend_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '好友申请主键',
    requester_id BIGINT NOT NULL COMMENT '申请发起人用户 ID',
    recipient_id BIGINT NOT NULL COMMENT '申请接收人用户 ID',
    created_at TIMESTAMP NOT NULL COMMENT '申请创建时间',
    CONSTRAINT uk_friend_requests_requester_recipient UNIQUE (requester_id, recipient_id),
    CONSTRAINT fk_friend_requests_requester FOREIGN KEY (requester_id) REFERENCES users(id),
    CONSTRAINT fk_friend_requests_recipient FOREIGN KEY (recipient_id) REFERENCES users(id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT='好友申请表';

CREATE TABLE IF NOT EXISTS chat_groups (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '群聊主键',
    name VARCHAR(64) NOT NULL COMMENT '群名称',
    owner_id BIGINT NOT NULL COMMENT '群主用户 ID',
    avatar_url VARCHAR(255) COMMENT '群头像访问地址',
    created_at TIMESTAMP NOT NULL COMMENT '创建时间',
    CONSTRAINT fk_chat_groups_owner FOREIGN KEY (owner_id) REFERENCES users(id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT='群聊表';

CREATE TABLE IF NOT EXISTS group_members (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '群成员主键',
    group_id BIGINT NOT NULL COMMENT '群聊 ID',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    role VARCHAR(16) NOT NULL COMMENT '成员角色',
    joined_at TIMESTAMP NOT NULL COMMENT '入群时间',
    CONSTRAINT uk_group_members_group_user UNIQUE (group_id, user_id),
    CONSTRAINT fk_group_members_group FOREIGN KEY (group_id) REFERENCES chat_groups(id),
    CONSTRAINT fk_group_members_user FOREIGN KEY (user_id) REFERENCES users(id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT='群成员表';

CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '消息主键',
    conversation_key VARCHAR(128) NOT NULL COMMENT '会话唯一键',
    scope VARCHAR(16) NOT NULL COMMENT '消息作用域',
    sender_id BIGINT NOT NULL COMMENT '发送者用户 ID',
    group_id BIGINT COMMENT '群聊 ID，私聊和大厅为空',
    content_type VARCHAR(16) NOT NULL COMMENT '消息类型',
    content TEXT COMMENT '文本或表情内容',
    file_name VARCHAR(255) COMMENT '附件原始文件名',
    file_url VARCHAR(255) COMMENT '附件访问地址',
    created_at TIMESTAMP NOT NULL COMMENT '发送时间',
    CONSTRAINT fk_chat_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id),
    CONSTRAINT fk_chat_messages_group FOREIGN KEY (group_id) REFERENCES chat_groups(id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT='聊天消息表';

CREATE TABLE IF NOT EXISTS conversation_states (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话状态主键',
    user_id BIGINT NOT NULL COMMENT '用户 ID',
    conversation_key VARCHAR(128) NOT NULL COMMENT '会话唯一键',
    last_read_message_id BIGINT NOT NULL COMMENT '最后已读消息 ID',
    updated_at TIMESTAMP NOT NULL COMMENT '状态更新时间',
    CONSTRAINT uk_conversation_states_user_key UNIQUE (user_id, conversation_key),
    CONSTRAINT fk_conversation_states_user FOREIGN KEY (user_id) REFERENCES users(id)
) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT='会话已读状态表';
