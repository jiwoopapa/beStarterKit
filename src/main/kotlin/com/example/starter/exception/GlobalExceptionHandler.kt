package com.example.starter.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    // Jakarta Validation 실패 시 필드별 오류 메시지 반환
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, Any>> {
        val errors = ex.bindingResult.fieldErrors.associate { it.field to (it.defaultMessage ?: "오류") }
        val body = mapOf(
            "status" to HttpStatus.BAD_REQUEST.value(),
            "message" to "유효성 검사 실패",
            "errors" to errors
        )
        return ResponseEntity.badRequest().body(body)
    }

    // RuntimeException 핸들러: "not found" 포함 시 404, 그 외 500
    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(ex: RuntimeException): ResponseEntity<Map<String, Any>> {
        val message = ex.message ?: "내부 서버 오류"
        val isNotFound = message.contains("not found", ignoreCase = true)
        val status = if (isNotFound) HttpStatus.NOT_FOUND else HttpStatus.INTERNAL_SERVER_ERROR

        val body = mapOf(
            "status" to status.value(),
            "message" to message
        )
        return ResponseEntity.status(status).body(body)
    }
}
