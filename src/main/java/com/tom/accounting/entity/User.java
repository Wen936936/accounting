package com.tom.accounting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

// 用户实体，对应数据库表 user（表名加反引号以避开 MySQL 关键字）
@Data
@TableName("`user`")
public class User {

    // 主键，自增 BIGINT
    @TableId(type = IdType.AUTO)
    private Long id;

    // 登录用户名，唯一
    private String username;

    // BCrypt 加密后的密码，禁止返回给前端
    private String password;

    // 展示用昵称
    private String nickname;

    // 创建时间
    private LocalDateTime createdAt;

    // 更新时间
    private LocalDateTime updatedAt;
}
