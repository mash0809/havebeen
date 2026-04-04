# havebeen 🦜

> "그때 샀더라면..." — 적립식(DCA) 투자 시뮬레이터

특정 종목을 매일 일정 금액씩 투자했다면 지금쯤 얼마가 됐을지 계산해주는 서비스입니다.
수익률에 따라 **껄무새 등급**을 부여하고, 평가금액을 일상 소비재에 빗대어 표현합니다.

---

## 기술 스택

| 분류 | 기술 |
|---|---|
| Language | Kotlin 1.9 / Java 21 |
| Framework | Spring Boot 3.5 |
| Database | MySQL 8 |
| Cache | Caffeine (TTL 60분) |
| Data Source | Yahoo Finance v8 Chart API |
| Frontend | Next.js (개발 중) |

---

## 주요 기능

- **적립식 투자 시뮬레이션**: 지정한 기간 동안 매 거래일 일정 금액을 종가로 매수
- **수익률 계산**: 평가금액 / 총 투자금 기반 수익률(%) 산출
- **껄무새 등급**: 수익률에 따라 6단계 등급 부여
- **비유 텍스트**: 평가금액을 아메리카노·아이폰·맥북으로 환산
- **차트 데이터**: 날짜별 평가금액 시계열 제공

### 껄무새 등급표

| 수익률 | 등급 |
|---|---|
| 300% 이상 | 황금 껄무새 |
| 100% 이상 | 대왕 껄무새 |
| 50% 이상 | 껄무새 |
| 10% 이상 | 아기 껄무새 |
| 0% 이상 | 알 껄무새 |
| 0% 미만 | 현명한 소비자 |

---

## 시작하기

### 사전 요구사항

- JDK 21
- MySQL 8

### 데이터베이스 설정

```sql
CREATE DATABASE havebeen;
```

`application.yaml`의 DB 접속 정보를 환경에 맞게 설정하거나 환경 변수로 주입합니다.

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/havebeen
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

### 실행

```bash
./gradlew bootRun
```

---

## API

### 시뮬레이션 실행

```
GET /api/simulation
```

**쿼리 파라미터**

| 파라미터 | 타입 | 설명 | 예시 |
|---|---|---|---|
| `symbol` | String | Yahoo Finance 종목 코드 | `AAPL`, `005930.KS` |
| `dailyAmount` | Long | 일일 투자금 (원) | `10000` |
| `startDate` | Date | 시작일 (ISO 8601) | `2023-01-01` |
| `endDate` | Date | 종료일 (ISO 8601) | `2024-01-01` |

> 한국 주식은 거래소 접미사를 포함합니다 (예: 삼성전자 → `005930.KS`)

**요청 예시**

```bash
curl "http://localhost:8080/api/simulation?symbol=005930.KS&dailyAmount=10000&startDate=2023-01-01&endDate=2024-01-01"
```

**응답 예시**

```json
{
  "symbol": "005930.KS",
  "startDate": "2023-01-01",
  "endDate": "2024-01-01",
  "dailyAmount": 10000,
  "totalInvested": 2470000,
  "currentValue": 2311420,
  "returnRate": -6.43,
  "grade": "현명한 소비자",
  "analogyText": "스타벅스 아메리카노 420.3잔",
  "chartData": [
    { "date": "2023-01-02", "value": 9870 },
    ...
  ]
}
```

---

## 테스트

```bash
./gradlew test
```

---

## 프로젝트 구조

```
src/main/kotlin/com/could/havebeen/
├── stock/
│   ├── SimulationController.kt   # REST 엔드포인트
│   ├── SimulationService.kt      # DCA 시뮬레이션 비즈니스 로직
│   ├── YahooFinanceClient.kt     # Yahoo Finance API 클라이언트
│   └── model/
│       ├── SimulationResult.kt   # 시뮬레이션 결과 모델
│       └── StockPrice.kt         # 일별 주가 모델
```
