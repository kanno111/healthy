package com.healthy.identity.vo;

public record LoginVO(String token, Long userId, String name, String role) {
}
