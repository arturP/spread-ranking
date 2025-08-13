# Contract Tests for KangaApiClientOptimized

This directory contains contract tests for the KangaApiClientOptimized class using Spring Cloud Contract and WireMock.

## Overview

Contract tests ensure that the integration between our client and the Kanga Exchange API remains stable by defining explicit contracts that describe the expected behavior of the external API.

## Test Structure

### 1. Contract Definitions (`src/test/resources/contracts/kanga/`)
- `market_pairs_success.groovy` - Successful market pairs response
- `market_pairs_empty.groovy` - Empty market pairs response  
- `market_pairs_server_error.groovy` - Server error response
- `orderbook_success.groovy` - Successful order book response
- `orderbook_empty.groovy` - Empty order book response
- `orderbook_not_found.groovy` - Not found error response

### 2. Test Classes
- `KangaContractTestBase.java` - Base configuration for contract tests
- `KangaApiClientOptimizedContractTest.java` - Main contract test suite
- `KangaApiClientPerformanceTest.java` - Performance and resilience tests

### 3. Test Configuration
- `application-test.yml` - Test-specific configuration

## Running the Tests

### Run Contract Tests
```bash
# Run all contract tests
mvn test -Dtest="*ContractTest"

# Run with contract generation
mvn clean test spring-cloud-contract:convert spring-cloud-contract:generateTests
```

### Run Performance Tests
```bash
mvn test -Dtest="*PerformanceTest"
```

### Run All Integration Tests
```bash
mvn test -Dtest="*Contract*,*Performance*"
```

## Test Scenarios Covered

### Market Pairs Endpoint
- ✅ Successful response with multiple market pairs
- ✅ Empty response handling
- ✅ Server error handling
- ✅ Timeout and retry behavior

### Order Book Endpoint  
- ✅ Successful response with bid/ask data
- ✅ Empty order book handling
- ✅ Not found error handling
- ✅ Input parameter validation
- ✅ Bulk order book fetching

### Resilience & Performance
- ✅ Circuit breaker behavior (open/half-open/closed states)
- ✅ Concurrent request handling
- ✅ Timeout respect and graceful degradation
- ✅ Mixed success/failure response handling

## Contract Benefits

1. **API Stability** - Detects breaking changes in external API
2. **Documentation** - Contracts serve as living documentation
3. **Isolation** - Tests run without external dependencies
4. **Reliability** - Validates error handling and edge cases
5. **Performance** - Verifies resilience patterns work correctly

## Contract Evolution

When the Kanga API changes:

1. Update contract definitions in `.groovy` files
2. Run tests to identify breaking changes
3. Update client implementation if needed
4. Re-run tests to ensure compatibility

## Debugging

Enable debug logging by adding to test configuration:
```yaml
logging:
  level:
    com.github.tomakehurst.wiremock: DEBUG
    org.springframework.cloud.contract: DEBUG
```

View WireMock interactions at: `http://localhost:{port}/__admin`