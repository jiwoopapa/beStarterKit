package com.example.starter.config

import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.OpenAPI
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        val info = Info()
            .title("beStarterKit API")
            .version("1.0.0")
            .description("Spring Boot + Kotlin + PostgreSQL 백엔드 스타터 키트")
            .contact(
                Contact()
                    .name("Dev Team")
                    .email("dev@example.com")
            )

        return OpenAPI().info(info)
    }
}
