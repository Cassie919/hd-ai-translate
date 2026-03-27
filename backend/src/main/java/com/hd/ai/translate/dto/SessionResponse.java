package com.hd.ai.translate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {
    private boolean exists;
    private SessionMeta meta;
    private SessionData data;
}
