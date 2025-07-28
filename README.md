# Spread Ranking Microservice

A Java Spring Boot microservice that creates cryptocurrency market rankings based on current spread percentages using Kanga Exchange API.

### Spread Calculation Formula
```
Spread = (ASK - BID) / [0.5 * (ASK + BID)] * 100%
```

### Market Groups
- **Group 1**: Spread ≤ 2%
- **Group 2**: Spread > 2%
- **Group 3**: Cannot calculate spread (missing BID/ASK prices)

## Installation & Setup

### 1. Clone Repository
```bash
git clone https://github.com/arturP/spread-ranking.git
cd spread-ranking-microservice
```

### 2. Build Project
```bash
./mvnw clean compile
```

### 3. Run Tests
```bash
./mvnw test
```

### 4. Start Application
```bash
./mvnw spring-boot:run
```

The application starts on `http://localhost:8080`

## API Endpoints

### Authentication
Protected endpoints require Bearer token: `ABC123`

### Endpoints

| Method | Endpoint | Description | Authentication |
|--------|----------|-------------|----------------|
| POST | `/api/spread/calculate` | Calculate new spread ranking | Required |
| GET | `/api/spread/ranking` | Get current ranking | None |

## Usage Examples

### 1. Calculate New Ranking
```bash
curl -X POST http://localhost:8080/api/spread/calculate \
  -H "Authorization: Bearer ABC123" \
  -H "Content-Type: application/json"
```

**Response:**
```json
{
  "message": "Ranking calculated successfully",
  "timestamp": "2025-07-15T13:29:08Z",
  "status": "SUCCESS",
  "calculationDurationMs": 1250
}
```

### 2. Get Current Ranking
```bash
curl -X GET http://localhost:8080/api/spread/ranking \
  -H "Authorization: Bearer ABC123" \
  -H "Accept: application/json"
```

**Response:**
```json
{
  "timestamp": "2025-07-15T13:29:08Z",
  "ranking": {
    "group1": [
      {"market": "BTC_USDC", "spread": "1.99"},
      {"market": "ETH_USDC", "spread": "0.99"}
    ],
    "group2": [
      {"market": "ALGO_USDC", "spread": "2.99"},
      {"market": "SHIB_USDC", "spread": "7.99"}
    ],
    "group3": [
      {"market": "ETC_USDT", "spread": "N/A"},
      {"market": "XRP_USDT", "spread": "N/A"}
    ]
  }
}
```

### Docker Build & Run
```bash
./mvnw clean package
docker build -t spread-ranking-service .
docker run -p 8080:8080 spread-ranking-service
```