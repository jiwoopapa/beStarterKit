package com.example.starter.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size

// 부분 수정을 위해 모든 필드가 nullable
data class UserUpdateRequest(
    @field:Size(min = 2, max = 50, message = "username은 2~50자 사이여야 합니다")
    val username: String? = null,

    @field:Email(message = "유효한 이메일 형식이어야 합니다")
    @field:Size(max = 100, message = "email은 100자 이하여야 합니다")
    val email: String? = null
)
