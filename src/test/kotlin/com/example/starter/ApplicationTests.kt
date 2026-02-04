package com.example.starter

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class ApplicationTests {

    @Test
    fun contextLoads() {
        // Spring ApplicationContext가 정상 로드되는지 검증
    }
}
