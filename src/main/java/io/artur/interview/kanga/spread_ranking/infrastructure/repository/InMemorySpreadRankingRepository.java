package io.artur.interview.kanga.spread_ranking.infrastructure.repository;

import io.artur.interview.kanga.spread_ranking.domain.model.SpreadRanking;
import io.artur.interview.kanga.spread_ranking.domain.repository.SpreadRankingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe in-memory implementation of SpreadRankingRepository.
 * Uses ReadWriteLock to ensure thread safety for concurrent access.
 * Supports configurable ranking expiration time.
 */
@Repository
@Slf4j
public class InMemorySpreadRankingRepository implements SpreadRankingRepository {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Clock clock;
    private final Duration rankingValidityDuration;
    
    // Volatile to ensure visibility across threads
    private volatile SpreadRanking currentRanking;
    private volatile Instant lastUpdated;

    public InMemorySpreadRankingRepository(
            Clock clock,
            @Value("${app.spread-ranking.validity-duration:PT5M}") String validityDuration) {
        this.clock = clock;
        this.rankingValidityDuration = Duration.parse(validityDuration);
        log.info("Initialized SpreadRankingRepository with validity duration: {}", rankingValidityDuration);
    }

    @Override
    public void storeSpreadRanking(SpreadRanking spreadRanking) {
        if (spreadRanking == null) {
            log.warn("Attempted to store null SpreadRanking - ignoring");
            return;
        }

        lock.writeLock().lock();
        try {
            Instant now = clock.instant();
            this.currentRanking = spreadRanking;
            this.lastUpdated = now;
            
            log.info("Stored SpreadRanking with {} total markets, calculated at {}", 
                    spreadRanking.getTotalMarketsCount(), spreadRanking.getCalculatedAt());
            log.debug("SpreadRanking breakdown - Low: {}, High: {}, Unavailable: {}", 
                    spreadRanking.getLowSpreadMarkets().size(),
                    spreadRanking.getHighSpreadMarkets().size(),
                    spreadRanking.getUnavailableMarkets().size());
                    
        } catch (Exception e) {
            log.error("Error while storing SpreadRanking", e);
            throw new RuntimeException("Failed to store SpreadRanking", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<SpreadRanking> getCurrentSpreadRanking() {
        lock.readLock().lock();
        try {
            if (currentRanking == null) {
                log.debug("No SpreadRanking available in repository");
                return Optional.empty();
            }
            
            log.debug("Retrieved SpreadRanking with {} total markets from repository", 
                    currentRanking.getTotalMarketsCount());
            return Optional.of(currentRanking);
            
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean hasValidSpreadRanking() {
        lock.readLock().lock();
        try {
            boolean hasRanking = currentRanking != null;
            boolean isNotExpired = !isRankingExpired();
            boolean isValid = hasRanking && isNotExpired;
            
            log.debug("SpreadRanking validity check - hasRanking: {}, isNotExpired: {}, isValid: {}", 
                    hasRanking, isNotExpired, isValid);
            
            if (hasRanking && !isNotExpired) {
                log.info("SpreadRanking exists but has expired (age: {})", getAgeOfCurrentRanking());
            }
            
            return isValid;
            
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean isRankingExpired() {
        lock.readLock().lock();
        try {
            if (currentRanking == null || lastUpdated == null) {
                log.trace("No ranking exists, considering as expired");
                return true;
            }
            
            Instant now = clock.instant();
            Duration age = Duration.between(lastUpdated, now);
            boolean expired = age.compareTo(rankingValidityDuration) > 0;
            
            log.trace("Ranking age: {}, validity duration: {}, expired: {}", 
                    age, rankingValidityDuration, expired);
            
            return expired;
            
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            boolean hadRanking = currentRanking != null;
            currentRanking = null;
            lastUpdated = null;
            
            log.info("Cleared SpreadRanking repository (had ranking: {})", hadRanking);
            
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Thread-safe method to get the age of the current ranking.
     * @return duration since last update, or null if no ranking exists
     */
    public Duration getAgeOfCurrentRanking() {
        lock.readLock().lock();
        try {
            if (lastUpdated == null) {
                return null;
            }
            
            Instant now = clock.instant();
            Duration age = Duration.between(lastUpdated, now);
            log.trace("Current ranking age: {}", age);
            return age;
            
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Thread-safe method to get the timestamp of when ranking was last updated.
     * @return instant of last update, or null if no ranking exists
     */
    public Instant getLastUpdatedTime() {
        lock.readLock().lock();
        try {
            log.trace("Last updated time: {}", lastUpdated);
            return lastUpdated;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Thread-safe method to check if repository is empty.
     * @return true if no ranking is stored, false otherwise
     */
    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            boolean empty = currentRanking == null;
            log.trace("Repository is empty: {}", empty);
            return empty;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Thread-safe method to get repository statistics.
     * @return formatted string with repository statistics
     */
    public String getRepositoryStats() {
        lock.readLock().lock();
        try {
            if (currentRanking == null) {
                return "Repository: empty";
            }
            
            Duration age = getAgeOfCurrentRanking();
            boolean expired = isRankingExpired();
            
            return String.format("Repository: %d markets, age: %s, expired: %s", 
                    currentRanking.getTotalMarketsCount(), age, expired);
                    
        } finally {
            lock.readLock().unlock();
        }
    }
}