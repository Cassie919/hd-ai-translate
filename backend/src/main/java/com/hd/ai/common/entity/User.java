package com.hd.ai.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 无登录用户实体。
 * <p>
 * 通过 {@code user_key}（即 Cookie 中的 userId）唯一标识用户，
 * 不依赖传统用户名/密码登录体系。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_key", nullable = false, unique = true, length = 64)
    private String userKey;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        lastLoginTime = LocalDateTime.now();
    }
}
