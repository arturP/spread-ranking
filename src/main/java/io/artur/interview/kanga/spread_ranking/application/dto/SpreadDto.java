package io.artur.interview.kanga.spread_ranking.application.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SpreadDto {

    private final String market;
    private final String spread;
}
