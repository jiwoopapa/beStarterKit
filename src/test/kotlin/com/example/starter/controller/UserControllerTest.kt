package com.example.starter.controller

import com.example.starter.dto.UserCreateRequest
import com.example.starter.entity.User
import com.example.starter.service.UserService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(UserController::class)
class UserControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var userService: UserService

    // 유효한 요청 → 201 Created 검증
    @Test
    fun postUsers_유효한요청시_201반환() {
        val request = UserCreateRequest(username = "testuser", email = "test@example.com")
        val mockUser = User().apply {
            id = 1L
            username = "testuser"
            email = "test@example.com"
        }

        Mockito.`when`(userService.create(request)).thenReturn(mockUser)

        mockMvc.perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
    }

    // 유효성 검사 실패 → 400 Bad Request 검증
    @Test
    fun postUsers_유효성검사실패시_400반환() {
        val invalidRequest = mapOf(
            "username" to "",       // @NotBlank 위반
            "email" to "invalid"   // @Email 위반
        )

        mockMvc.perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").isMap())
    }
}
