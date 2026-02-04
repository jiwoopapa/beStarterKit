package com.example.starter.dto

import com.example.starter.entity.User
import java.time.LocalDateTime

data class UserResponse(
    val id: Long,
    val username: String,
    val email: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        // Entity -> DTO 변환 팩토리 메서드
        fun from(user: User): UserResponse {
            return UserResponse(
                id = user.id!!,
                username = user.username,
                email = user.email,
                createdAt = user.createdAt,
                updatedAt = user.updatedAt
            )
        }
    }
}
