# 공연 좌석 예약 시스템

동시 요청이 몰리는 공연 예매 상황을 가정한 좌석 예약 API 서버입니다.
여러 서버가 하나의 데이터베이스를 공유하는 환경에서도 같은 좌석이 중복 판매되지 않도록 처리하고, 중복·지연·순서 변경이 발생할 수 있는 결제 이벤트를 안전하게 반영합니다.

## 기술 스택

- Java 21
- Spring Boot
- Spring Data JPA
- PostgreSQL 16
- Flyway
- Docker Compose
- Testcontainers

## 실행 전 준비물

- Java 21
- Docker Desktop

## 실행 방법

### 1. PostgreSQL 실행

Docker Desktop을 실행한 뒤, 프로젝트 루트에서 아래 명령어를 실행합니다.

```bash
docker compose up -d
```

정상 실행 여부는 아래 명령어로 확인합니다.

```bash
docker compose ps
```

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

또는 IntelliJ에서 `src/main/java/com/kmj/concert/ConcertApplication.java`를 직접 실행해도 됩니다.

애플리케이션은 `http://localhost:8080`에서 실행됩니다.

## 초기 데이터

Flyway 마이그레이션이 애플리케이션 시작 시 자동으로 실행됩니다.

- 공연: id = 1, "2026 Demo Concert"
- 좌석: A-1부터 A-1000까지 총 1,000석
- 초기 좌석 상태: 모두 AVAILABLE

PostgreSQL 데이터는 Docker volume에 저장되므로, 애플리케이션이나 컨테이너를 재시작해도 유지됩니다.

## API

### 좌석 조회

GET /performances/1/seats


### 좌석 선점

POST /holds
Content-Type: application/json

{
"performanceId": 1,
"userId": "minsu",
"seatLabels": ["A-1", "A-2"],
"requestId": "req-001"
}


성공 시 `201 Created`를 반환합니다.

```json
{
  "holdId": "생성된 UUID",
  "paymentId": "pay-생성된 UUID",
  "seatLabels": ["A-1", "A-2"],
  "expiresAt": "2026-09-20T00:00:00Z"
}
```

같은 선점 요청을 재전송할 때는 동일한 `requestId`를 사용해야 합니다.

### 선점 취소

DELETE /holds/{holdId}?userId=minsu


성공 시 `204 No Content`를 반환합니다.

### 결제 이벤트 수신

POST /payment-events
Content-Type: application/json

{
"eventId": "evt-001",
"paymentId": "pay-생성된 UUID",
"holdId": "생성된 UUID",
"status": "approved",
"occurredAt": "2026-09-20T00:00:00Z"
}


`status`에는 `approved`, `cancelled`를 사용할 수 있습니다. 성공 시 `204 No Content`를 반환합니다.

## 주요 처리 규칙

- 같은 좌석에 대한 동시 선점 요청은 데이터베이스 잠금을 통해 하나만 성공합니다.
- 여러 좌석을 선점할 때 하나라도 사용할 수 없으면 전체 요청이 실패합니다.
- 같은 `requestId`의 선점 요청은 한 번만 처리합니다.
- 같은 `eventId`의 결제 이벤트는 한 번만 처리합니다.
- 승인과 취소 이벤트의 도착 순서가 달라도 `occurredAt` 기준으로 더 최신 이벤트만 좌석 상태에 반영합니다.
- 최신 `approved` 이벤트는 유효한 선점의 좌석을 SOLD로 변경합니다.
- 최신 `cancelled` 이벤트는 좌석을 AVAILABLE로 반환합니다.
- 선점 만료와 결제 승인이 겹치면 만료를 우선합니다. 이미 만료된 선점에 대한 승인 이벤트는 기록만 남기고 좌석을 다시 판매하지 않습니다.

설계 배경과 트레이드오프는 [DECISIONS.md](./DECISIONS.md)를 참고하세요.

## 선점 만료 시간 설정

선점 만료 시간은 `src/main/resources/application.yml`에서 변경할 수 있습니다.

```yaml
app:
  hold-duration: PT5M
```

`PT5M`은 5분을 의미합니다.

## 테스트 실행 방법

```bash
./gradlew test
```

또는 IntelliJ에서 `src/test/java` 폴더를 우클릭한 뒤 `Run 'All Tests'`를 실행해도 됩니다.

테스트는 Testcontainers를 통해 실제 PostgreSQL 컨테이너에서 실행됩니다.

### 검증 항목

- 같은 좌석에 대한 동시 선점 요청은 하나만 성공한다.
- 여러 좌석 선점 중 하나라도 실패하면 어느 좌석도 선점되지 않는다.
- 같은 `requestId`의 동시 선점 요청은 hold 하나만 생성한다.
- 같은 결제 이벤트가 동시에 재전송되어도 한 번만 반영된다.
- 취소 이벤트가 승인 이벤트보다 먼저 도착해도 문서화한 규칙을 따른다.
- 선점 만료와 결제 승인이 겹치면 만료가 우선한다.

## 구현하지 못한 항목

미완료 항목과 알려진 한계는 [NOT-DONE.md](./NOT-DONE.md)를 참고하세요.

## AI 도구 사용

AI 도구 활용 내역은 [AI-USAGE.md](./AI-USAGE.md)를 참고하세요.