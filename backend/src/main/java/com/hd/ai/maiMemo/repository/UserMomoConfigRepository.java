package com.hd.ai.maiMemo.repository;

import com.hd.ai.maiMemo.entity.UserMomoConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserMomoConfigRepository extends JpaRepository<UserMomoConfig, Long> {
    Optional<UserMomoConfig> findByUserId(Long userId);
}
