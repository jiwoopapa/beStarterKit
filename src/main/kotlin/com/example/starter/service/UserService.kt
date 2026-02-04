package com.example.starter.service

import com.example.starter.dto.UserCreateRequest
import com.example.starter.dto.UserUpdateRequest
import com.example.starter.entity.User
import com.example.starter.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class UserService(private val userRepository: UserRepository) {

    @Transactional(readOnly = true)
    fun findAll(): List<User> = userRepository.findAll()

    @Transactional(readOnly = true)
    fun findById(id: Long): User =
        userRepository.findById(id).orElseThrow { RuntimeException("User with id $id not found") }

    @Transactional
    fun create(request: UserCreateRequest): User {
        val user = User().apply {
            username = request.username
            email = request.email
        }
        return userRepository.save(user)
    }

    @Transactional
    fun update(id: Long, request: UserUpdateRequest): User {
        val user = findById(id)
        request.username?.let { user.username = it }
        request.email?.let { user.email = it }
        user.updatedAt = LocalDateTime.now()
        return userRepository.save(user)
    }

    @Transactional
    fun delete(id: Long) {
        val user = findById(id)
        userRepository.delete(user)
    }
}
