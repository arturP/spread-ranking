package io.artur.interview.kanga.spread_ranking.application.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@AllArgsConstructor
public class SpreadDto {

    private final static String UNKNOWN_SPREAD = "unknown";

    private final String market;
    private final String spread;

    public static SpreadDto unknown(String marketId) {
        return new SpreadDto(marketId, UNKNOWN_SPREAD);
    }

    private static String formatSpreadValue(BigDecimal percentage) {
        if (percentage == null) {
            return UNKNOWN_SPREAD;
        }

        return percentage.setScale(2, RoundingMode.HALF_UP).toString();
    }
}
