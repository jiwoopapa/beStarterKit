package com.example.starter.controller

import com.example.starter.dto.UserCreateRequest
import com.example.starter.dto.UserUpdateRequest
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

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

    // GET /api/users - 전체 사용자 조회
    @Test
    fun getUsers_전체조회시_200반환() {
        val mockUsers = listOf(
            User().apply {
                id = 1L
                username = "user1"
                email = "user1@example.com"
            },
            User().apply {
                id = 2L
                username = "user2"
                email = "user2@example.com"
            }
        )

        Mockito.`when`(userService.findAll()).thenReturn(mockUsers)

        mockMvc.perform(get("/api/users"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].id").value(1L))
            .andExpect(jsonPath("$[0].username").value("user1"))
            .andExpect(jsonPath("$[1].id").value(2L))
            .andExpect(jsonPath("$[1].username").value("user2"))
    }

    // GET /api/users/{id} - 단일 사용자 조회 성공
    @Test
    fun getUserById_존재하는id로조회시_200반환() {
        val mockUser = User().apply {
            id = 1L
            username = "testuser"
            email = "test@example.com"
        }

        Mockito.`when`(userService.findById(1L)).thenReturn(mockUser)

        mockMvc.perform(get("/api/users/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"))
    }

    // GET /api/users/{id} - 존재하지 않는 사용자
    @Test
    fun getUserById_존재하지않는id로조회시_404반환() {
        Mockito.`when`(userService.findById(999L))
            .thenThrow(RuntimeException("User not found with id: 999"))

        mockMvc.perform(get("/api/users/999"))
            .andExpect(status().isNotFound())
    }

    // PUT /api/users/{id} - 사용자 수정 성공
    @Test
    fun putUser_유효한요청으로수정시_200반환() {
        val updateRequest = UserUpdateRequest(
            username = "updateduser",
            email = "updated@example.com"
        )
        val updatedUser = User().apply {
            id = 1L
            username = "updateduser"
            email = "updated@example.com"
        }

        Mockito.`when`(userService.update(1L, updateRequest)).thenReturn(updatedUser)

        mockMvc.perform(
            put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1L))
            .andExpect(jsonPath("$.username").value("updateduser"))
            .andExpect(jsonPath("$.email").value("updated@example.com"))
    }

    // PUT /api/users/{id} - 유효성 검사 실패
    @Test
    fun putUser_유효하지않은이메일로수정시_400반환() {
        val invalidRequest = mapOf(
            "email" to "invalid-email"  // @Email 위반
        )

        mockMvc.perform(
            put("/api/users/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest))
        )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors").isMap())
    }

    // PUT /api/users/{id} - 존재하지 않는 사용자
    @Test
    fun putUser_존재하지않는id로수정시_404반환() {
        val updateRequest = UserUpdateRequest(username = "newname")

        Mockito.`when`(userService.update(999L, updateRequest))
            .thenThrow(RuntimeException("User not found with id: 999"))

        mockMvc.perform(
            put("/api/users/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isNotFound())
    }

    // DELETE /api/users/{id} - 사용자 삭제 성공
    @Test
    fun deleteUser_존재하는id로삭제시_204반환() {
        Mockito.doNothing().`when`(userService).delete(1L)

        mockMvc.perform(delete("/api/users/1"))
            .andExpect(status().isNoContent())
    }

    // DELETE /api/users/{id} - 존재하지 않는 사용자
    @Test
    fun deleteUser_존재하지않는id로삭제시_404반환() {
        Mockito.doThrow(RuntimeException("User not found with id: 999"))
            .`when`(userService).delete(999L)

        mockMvc.perform(delete("/api/users/999"))
            .andExpect(status().isNotFound())
    }
}
