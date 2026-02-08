# beStarterKit 코드 품질 리뷰 - 학습 메모

## 프로젝트 아키텍처 패턴
- **계층형 아키텍처**: Controller → Service → Repository → Entity
- **트랜잭션 경계**: Service 계층에서 `@Transactional` 관리
- **DTO 변환**: `UserResponse.from(entity)` 팩토리 패턴 사용
- **예외 처리**: GlobalExceptionHandler로 전역 예외 핸들링

## 코드 스타일 컨벤션
- **주석**: 한국어, 복잡한 로직에만 추가
- **변수명**: 영어 camelCase
- **테스트 메서드명**: 한국어 백틱 함수명 (`fun postUsers_유효한요청시_201반환()`)
- **Kotlin 특이사항**: `@field:NotBlank` use-site target 필수

## 보안 패턴
- **SQL Injection**: Spring Data JPA 메서드 네이밍 사용 → 안전
- **Validation**: Jakarta Validation + `@Valid` 적용 완료
- **환경변수**: prod는 환경변수 사용, dev는 평문 (개선 필요)

## 발견된 주요 이슈

### 로직 문제
1. **중복 사용자명/이메일 체크 누락** (치명적)
   - 위치: `/src/main/kotlin/com/example/starter/service/UserService.kt:22-28, 31-37`
   - Repository에 `findByUsername`, `findByEmail` 메서드 있으나 사용 안 함
   - DB UNIQUE 제약조건만 의존 → 500 에러 발생

2. **페이징 없는 findAll()** (성능)
   - 위치: `/src/main/kotlin/com/example/starter/service/UserService.kt:15`
   - DoS 위험 (데이터 많을 때 메모리 부족)

3. **예외 메시지 노출** (보안)
   - 위치: `/src/main/kotlin/com/example/starter/exception/GlobalExceptionHandler.kt:26`
   - RuntimeException 메시지를 클라이언트에 그대로 노출
   - 프로덕션에서는 일반화 필요

### 테스트 부족
- Service 계층 테스트 없음 (Controller 테스트만 존재)
- Repository 통합 테스트 없음
- 중복 체크 로직 테스트 없음

## 성능 최적화 포인트
- **N+1 문제**: 현재 없음 (단일 엔티티)
- **인덱스**: username, email에 이미 인덱스 적용 완료
- **페이징**: findAll()에 페이징 추가 필요

## 코드 품질 지표
- 컴파일 상태: ✅ 성공
- 테스트 통과율: ✅ 100% (2개 파일, 10개 테스트)
- 테스트 커버리지: ⚠️ Controller만 커버 (Service/Repository 미커버)

## 다음 검토 시 체크사항
- [ ] 중복 체크 로직 추가 여부
- [ ] Service 계층 테스트 추가 여부
- [ ] 페이징 구현 여부
- [ ] Spring Security 인증/인가 추가 여부
