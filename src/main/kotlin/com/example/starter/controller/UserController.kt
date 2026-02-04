package com.example.starter.controller

import com.example.starter.dto.UserCreateRequest
import com.example.starter.dto.UserResponse
import com.example.starter.dto.UserUpdateRequest
import com.example.starter.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
@Tag(name = "User", description = "사용자 관리 API")
class UserController(private val userService: UserService) {

    @GetMapping
    @Operation(summary = "전체 사용자 조회")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "사용자 목록 반환")
    ])
    fun findAll(): ResponseEntity<List<UserResponse>> {
        val users = userService.findAll().map(UserResponse::from)
        return ResponseEntity.ok(users)
    }

    @GetMapping("/{id}")
    @Operation(summary = "단일 사용자 조회")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "사용자 반환"),
        ApiResponse(responseCode = "404", description = "사용자 미발견")
    ])
    fun findById(@PathVariable id: Long): ResponseEntity<UserResponse> {
        val user = userService.findById(id)
        return ResponseEntity.ok(UserResponse.from(user))
    }

    @PostMapping
    @Operation(summary = "사용자 생성")
    @ApiResponses(value = [
        ApiResponse(responseCode = "201", description = "사용자 생성 완료"),
        ApiResponse(responseCode = "400", description = "유효성 검사 실패")
    ])
    fun create(@Valid @RequestBody request: UserCreateRequest): ResponseEntity<UserResponse> {
        val user = userService.create(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user))
    }

    @PutMapping("/{id}")
    @Operation(summary = "사용자 수정")
    @ApiResponses(value = [
        ApiResponse(responseCode = "200", description = "사용자 수정 완료"),
        ApiResponse(responseCode = "400", description = "유효성 검사 실패"),
        ApiResponse(responseCode = "404", description = "사용자 미발견")
    ])
    fun update(@PathVariable id: Long, @Valid @RequestBody request: UserUpdateRequest): ResponseEntity<UserResponse> {
        val user = userService.update(id, request)
        return ResponseEntity.ok(UserResponse.from(user))
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "사용자 삭제")
    @ApiResponses(value = [
        ApiResponse(responseCode = "204", description = "사용자 삭제 완료"),
        ApiResponse(responseCode = "404", description = "사용자 미발견")
    ])
    fun delete(@PathVariable id: Long): ResponseEntity<Unit> {
        userService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
