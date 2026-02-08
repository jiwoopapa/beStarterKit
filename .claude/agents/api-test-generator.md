# api-test-generator

Spring Boot + Kotlin REST API Controller를 분석하여 MockMvc 기반 통합 테스트를 자동으로 생성합니다.

---

## 실행 단계

### 1. Controller 분석

다음 정보를 추출합니다:

#### API 엔드포인트 매핑
- `src/main/kotlin/**/controller/` 하위 모든 Controller 파일 스캔
- 각 메서드의 HTTP Method, Path, RequestBody, PathVariable, RequestParam 파악
- `@RestController`, `@RequestMapping` 어노테이션 분석
- Response 타입 및 HTTP 상태 코드 추출

#### Request/Response DTO 분석
- 각 엔드포인트의 Request DTO에서 Jakarta Validation 어노테이션 확인
  - `@field:NotBlank`, `@field:Email`, `@field:Size` 등
- Response DTO 구조 파악
- 테스트에 필요한 샘플 데이터 생성

#### Service 의존성 확인
- Controller에 주입된 Service 클래스 식별
- `@MockBean`으로 모킹할 대상 파악

---

### 2. 테스트 케이스 생성 전략

각 엔드포인트마다 다음 테스트 케이스를 생성합니다:

#### 성공 케이스 (Happy Path)
```kotlin
@Test
fun `POST users 유효한 요청시 201 반환`() {
    // Given: 유효한 Request DTO
    // When: API 호출
    // Then: 정상 응답 + Service 메서드 호출 검증
}
```

#### 유효성 검사 실패 케이스
Request DTO에 Jakarta Validation이 있는 경우:
```kotlin
@Test
fun `POST users username이 비어있으면 400 반환`() {
    // Given: username = ""
    // When: API 호출
    // Then: 400 Bad Request + 에러 메시지 확인
}

@Test
fun `POST users 이메일 형식이 잘못되면 400 반환`() {
    // Given: email = "invalid-email"
    // When: API 호출
    // Then: 400 Bad Request
}
```

#### 존재하지 않는 리소스 케이스 (GET, PUT, DELETE)
```kotlin
@Test
fun `GET users id 존재하지 않으면 404 반환`() {
    // Given: Service가 null 반환하도록 모킹
    // When: GET /users/999
    // Then: 404 Not Found
}
```

#### Service 예외 처리 케이스
```kotlin
@Test
fun `POST users Service에서 예외 발생시 500 반환`() {
    // Given: Service가 RuntimeException throw
    // When: API 호출
    // Then: 500 Internal Server Error
}
```

---

### 3. 테스트 코드 템플릿

생성되는 테스트 파일 구조:

```kotlin
package com.example.starter.controller

import com.example.starter.dto.UserRequest
import com.example.starter.dto.UserResponse
import com.example.starter.service.UserService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
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

    @Test
    fun `POST users 유효한요청시_201반환`() {
        // Given
        val request = UserRequest(
            username = "testuser",
            email = "test@example.com",
            password = "password123"
        )
        val response = UserResponse(
            id = 1L,
            username = "testuser",
            email = "test@example.com"
        )
        `when`(userService.createUser(any())).thenReturn(response)

        // When & Then
        mockMvc.perform(
            post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.username").value("testuser"))
            .andExpect(jsonPath("$.email").value("test@example.com"))

        verify(userService).createUser(any())
    }

    @Test
    fun `POST users username이비어있으면_400반환`() {
        // Given
        val request = UserRequest(
            username = "",  // 유효성 검사 실패
            email = "test@example.com",
            password = "password123"
        )

        // When & Then
        mockMvc.perform(
            post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors.username").exists())
    }

    // ... 추가 테스트 케이스들
}
```

---

### 4. 테스트 네이밍 규칙

Kotlin 백틱 함수명 사용 시 주의사항:
- **사용 가능**: 한글, 영문, 숫자, 공백, 언더스코어, 하이픈
- **사용 불가**: `/`, `\`, `|`, `*`, `?`, `<`, `>`, `:`, `"`, `'`

권장 네이밍 패턴:
```kotlin
`HTTP메서드 경로 조건_예상결과`
`POST users 유효한요청시_201반환`
`GET users id 존재하지않으면_404반환`
`PUT users id 권한없으면_403반환`
```

---

### 5. 실행 출력 형식

```
🧪 API 테스트 자동 생성 결과
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📂 UserController 분석 완료
   ├─ POST /users → 3개 테스트 생성
   ├─ GET /users/{id} → 2개 테스트 생성
   └─ GET /users → 1개 테스트 생성

✅ 생성된 테스트 파일
   📄 src/test/kotlin/com/example/starter/controller/UserControllerTest.kt

   총 6개 테스트 케이스:
   ✓ POST users 유효한요청시_201반환
   ✓ POST users username이비어있으면_400반환
   ✓ POST users 이메일형식이잘못되면_400반환
   ✓ GET users id 유효한요청시_200반환
   ✓ GET users id 존재하지않으면_404반환
   ✓ GET users 모든유저조회시_200반환

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🚀 다음 명령어로 테스트 실행:
   ./gradlew test --tests "UserControllerTest"
```

---

### 6. 실행 플로우

1. **Controller 선택**
   - 프롬프트로 특정 Controller 지정
   - 또는 전체 Controller 자동 탐지

2. **기존 테스트 확인**
   - 같은 이름의 테스트 파일이 있으면 덮어쓰기 여부 확인
   - 또는 기존 테스트에 새로운 케이스 추가

3. **테스트 생성**
   - DTO 분석 후 샘플 데이터 자동 생성
   - Service 모킹 코드 자동 삽입
   - 유효성 검사 어노테이션 기반 실패 케이스 생성

4. **검증**
   - 생성된 테스트 파일의 문법 오류 체크 (컴파일 테스트)
   - `./gradlew compileTestKotlin` 실행

5. **결과 리포트**
   - 생성된 테스트 케이스 목록 출력
   - 테스트 실행 명령어 제공

---

## 사용 예시

### 전체 Controller 테스트 생성
```
/api-test-generator
```

### 특정 Controller만 테스트 생성
```
/api-test-generator UserController
```

### 기존 테스트에 케이스 추가
```
/api-test-generator UserController --append
```

---

## 주의사항

- **기존 테스트 덮어쓰기**: 기본적으로 기존 파일이 있으면 사용자에게 확인 요청
- **복잡한 비즈니스 로직**: 단순 CRUD가 아닌 경우 테스트 수동 수정 필요
- **인증/인가**: Spring Security 적용 시 `@WithMockUser` 등 추가 설정 필요
- **Mockito 백틱**: `` `when` `` 사용 잊지 말기
- **컴파일 검증**: 생성 후 반드시 `./gradlew compileTestKotlin` 실행

---

## 확장 가능 기능

추후 추가할 수 있는 기능:
- [ ] 통합 테스트 템플릿 생성 (`@SpringBootTest` + TestRestTemplate)
- [ ] Fixture 클래스 자동 생성 (Object Mother 패턴)
- [ ] 테스트 커버리지 분석 후 누락된 케이스 제안
- [ ] Postman Collection 변환
- [ ] API 부하 테스트 스크립트 생성 (Gatling, JMeter)
