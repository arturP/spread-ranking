package io.artur.interview.kanga.spread_ranking.infrastructure.repository;

import io.artur.interview.kanga.spread_ranking.domain.model.Market;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class InMemoryMarketDataRepositoryTest {

    private InMemoryMarketDataRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryMarketDataRepository();
    }

    @Test
    void saveAll_shouldSaveMarketsSuccessfully() {
        // Given
        List<Market> markets = List.of(
                new Market("BTC-PLN", new BigDecimal("90000"), new BigDecimal("91000")),
                new Market("ETH-PLN", new BigDecimal("3000"), new BigDecimal("3100"))
        );

        // When
        repository.saveAll(markets);

        // Then
        assertThat(repository.getMarketCount()).isEqualTo(2);
        assertThat(repository.hasMarkets()).isTrue();
        assertThat(repository.findByTickerId("BTC-PLN")).isEqualTo(markets.get(0));
        assertThat(repository.findByTickerId("ETH-PLN")).isEqualTo(markets.get(1));
    }

    @Test
    void saveAll_shouldReplaceExistingMarkets() {
        // Given
        List<Market> initialMarkets = List.of(
                new Market("BTC-PLN", new BigDecimal("90000"), new BigDecimal("91000"))
        );
        List<Market> newMarkets = List.of(
                new Market("ETH-PLN", new BigDecimal("3000"), new BigDecimal("3100"))
        );

        // When
        repository.saveAll(initialMarkets);
        repository.saveAll(newMarkets);

        // Then
        assertThat(repository.getMarketCount()).isEqualTo(1);
        assertThat(repository.findByTickerId("BTC-PLN")).isNull();
        assertThat(repository.findByTickerId("ETH-PLN")).isEqualTo(newMarkets.get(0));
    }

    @Test
    void saveAll_shouldHandleEmptyList() {
        // When
        repository.saveAll(List.of());

        // Then
        assertThat(repository.getMarketCount()).isEqualTo(0);
        assertThat(repository.hasMarkets()).isFalse();
    }

    @Test
    void saveAll_shouldHandleNullList() {
        // When/Then
        assertDoesNotThrow(() -> repository.saveAll(null));
        assertThat(repository.getMarketCount()).isEqualTo(0);
    }

    @Test
    void saveAll_shouldSkipInvalidMarkets() {
        // Given
        List<Market> markets = new ArrayList<>();
        markets.add(new Market("BTC-PLN", new BigDecimal("90000"), new BigDecimal("91000")));
        markets.add(null);
        markets.add(new Market(null, new BigDecimal("100"), new BigDecimal("200")));

        // When
        repository.saveAll(markets);

        // Then
        assertThat(repository.getMarketCount()).isEqualTo(1);
        assertThat(repository.findByTickerId("BTC-PLN")).isNotNull();
    }

    @Test
    void findAll_shouldReturnDefensiveCopy() {
        // Given
        List<Market> markets = List.of(
                new Market("BTC-PLN", new BigDecimal("90000"), new BigDecimal("91000"))
        );
        repository.saveAll(markets);

        // When
        List<Market> retrieved = repository.findAll();

        // Then
        assertThat(retrieved).hasSize(1);
        assertThat(retrieved).isNotSameAs(markets);
        assertThat(retrieved.getFirst()).isEqualTo(markets.getFirst());
    }

    @Test
    void findByTickerId_shouldReturnNullForNonExistentMarket() {
        // When
        Market result = repository.findByTickerId("NON-EXISTENT");

        // Then
        assertThat(result).isNull();
    }

    @Test
    void findByTickerId_shouldReturnNullForNullTickerId() {
        // When
        Market result = repository.findByTickerId(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void clear_shouldRemoveAllMarkets() {
        // Given
        List<Market> markets = List.of(
                new Market("BTC-PLN", new BigDecimal("90000"), new BigDecimal("91000")),
                new Market("ETH-PLN", new BigDecimal("3000"), new BigDecimal("3100"))
        );
        repository.saveAll(markets);

        // When
        repository.clear();

        // Then
        assertThat(repository.getMarketCount()).isEqualTo(0);
        assertThat(repository.hasMarkets()).isFalse();
        assertThat(repository.findByTickerId("BTC-PLN")).isNull();
    }
}