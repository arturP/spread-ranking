package io.artur.interview.kanga.spread_ranking.infrastructure.repository;

import io.artur.interview.kanga.spread_ranking.domain.model.Market;
import io.artur.interview.kanga.spread_ranking.domain.repository.MarketDataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe in-memory implementation of MarketDataRepository.
 * Uses ConcurrentHashMap for storage and ReadWriteLock for batch operations.
 */
@Repository
@Slf4j
class InMemoryMarketDataRepository implements MarketDataRepository {

    private final ConcurrentHashMap<String, Market> markets = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public void saveAll(List<Market> marketList) {
        if (marketList == null || marketList.isEmpty()) {
            log.debug("No markets to save - list is null or empty");
            return;
        }

        lock.writeLock().lock();
        try {
            log.debug("Saving {} markets to repository", marketList.size());
            
            // Clear existing markets first to ensure fresh data
            markets.clear();
            
            // Save all new markets
            for (Market market : marketList) {
                if (market != null && market.tickerId() != null) {
                    markets.put(market.tickerId(), market);
                    log.trace("Saved market: {} with bid={}, ask={}", 
                            market.tickerId(), market.bidPrice(), market.askPrice());
                } else {
                    log.warn("Skipping invalid market: {}", market);
                }
            }
            
            log.info("Successfully saved {} markets to repository", markets.size());
            
        } catch (Exception e) {
            log.error("Error while saving markets to repository", e);
            throw new RuntimeException("Failed to save markets", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Thread-safe method to get all markets (for potential future use).
     * @return defensive copy of all markets
     */
    public List<Market> findAll() {
        lock.readLock().lock();
        try {
            log.debug("Retrieving all {} markets from repository", markets.size());
            return List.copyOf(markets.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Thread-safe method to get a specific market by ticker ID.
     * @param tickerId the ticker ID to search for
     * @return the market if found, null otherwise
     */
    public Market findByTickerId(String tickerId) {
        if (tickerId == null) {
            return null;
        }
        
        lock.readLock().lock();
        try {
            Market market = markets.get(tickerId);
            log.trace("Retrieved market for ticker {}: {}", tickerId, market);
            return market;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Thread-safe method to get the count of stored markets.
     * @return number of markets in the repository
     */
    public int getMarketCount() {
        lock.readLock().lock();
        try {
            int count = markets.size();
            log.trace("Repository contains {} markets", count);
            return count;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Thread-safe method to check if repository contains markets.
     * @return true if repository has markets, false otherwise
     */
    public boolean hasMarkets() {
        lock.readLock().lock();
        try {
            boolean hasMarkets = !markets.isEmpty();
            log.trace("Repository has markets: {}", hasMarkets);
            return hasMarkets;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Thread-safe method to clear all markets from repository.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            int previousSize = markets.size();
            markets.clear();
            log.info("Cleared {} markets from repository", previousSize);
        } finally {
            lock.writeLock().unlock();
        }
    }
}