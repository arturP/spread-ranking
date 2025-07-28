package io.artur.interview.kanga.spread_ranking.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.artur.interview.kanga.spread_ranking.domain.model.Spread;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SpreadApiDto(String market, BigDecimal spreadPercentage) {

    public static SpreadApiDto fromDomainSpread(Spread spread) {
        return new SpreadApiDto(
                spread.marketId(),
                spread.percentage()
        );
    }

    public static SpreadApiDto unknown(String marketId) {
        return new SpreadApiDto(marketId, null);
    }
}