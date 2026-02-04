package com.example.starter.repository

import com.example.starter.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {

    fun findByUsername(username: String): User?

    fun findByEmail(email: String): User?
}
