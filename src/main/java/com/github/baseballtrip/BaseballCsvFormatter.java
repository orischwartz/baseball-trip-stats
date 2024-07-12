package com.github.baseballtrip;

import static com.github.baseballtrip.PlayerStat.addDerivedStats;
import static com.github.baseballtrip.PlayerStat.sumStats;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.ImmutableListMultimap.toImmutableListMultimap;
import static java.util.stream.Collectors.joining;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import java.text.DecimalFormat;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class BaseballCsvFormatter {
  private static final ImmutableSet<String> BATTING_HEADERS =
      ImmutableSet.of(
          "AB", "R", "H", "RBI", "AVG", "SLG", "TB", "2B", "3B", "HR", "BB", "SO", "PA", "SB",
          "CS");
  private static final ImmutableSet<String> PITCHING_HEADERS =
      ImmutableSet.of("IP", "ER", "R", "BB", "SO", "BF", "PIT", "HR", "ERA", "K/9", "WHIP");

  private final ImmutableMap<String, ImmutableSet<String>> headerGroups =
      ImmutableMap.of("BATTING", BATTING_HEADERS, "PITCHING", PITCHING_HEADERS);

  private final ImmutableList<String> headers =
      ImmutableList.<String>builder()
          .add("player")
          .addAll(BATTING_HEADERS)
          .addAll(PITCHING_HEADERS)
          .add("seasons")
          .build();

  private final DecimalFormat statFormatter = new DecimalFormat(",##0.#");
  private final DecimalFormat averageFormatter = new DecimalFormat(".000");

  private final DecimalFormat eraFormatter = new DecimalFormat("0.00");

  private final ImmutableMap<String, Function<Float, String>> formatters =
      ImmutableMap.<String, Function<Float, String>>builder()
          .put("IP", BaseballCsvFormatter::formatInnings)
          .put("AVG", averageFormatter::format)
          .put("SLG", averageFormatter::format)
          .put("ERA", eraFormatter::format)
          .build();

  public String[][] toCsvCells(List<Boxscore> boxscores) {
    ImmutableListMultimap<String, PlayerStat> byPlayerId =
        boxscores.stream()
            .flatMap(b -> b.stats.stream())
            .collect(toImmutableListMultimap(p -> p.playerId, p -> p));

    IdentityHashMap<PlayerStat, Matchup> matchupsByStat = new IdentityHashMap<>();

    for (Boxscore boxscore : boxscores) {
      for (PlayerStat stat : boxscore.stats) {
        matchupsByStat.put(stat, boxscore.matchup);
      }
    }

    String[][] result = new String[byPlayerId.keySet().size() + 1][];
    int row = 0;
    result[row++] = headers.toArray(new String[0]);

    for (String playerId : byPlayerId.keySet()) {
      List<PlayerStat> statsForPlayer = byPlayerId.get(playerId);
      ImmutableTable<String, String, Float> sums =
          addDerivedStats(sumStats(Lists.transform(statsForPlayer, p -> p.stats)));

      String[] formattedStatSums = new String[headers.size()];
      int column = 0;
      formattedStatSums[column++] = statsForPlayer.get(0).playerName;

      for (Map.Entry<String, ImmutableSet<String>> e : headerGroups.entrySet()) {
        for (String header : e.getValue()) {
          Float value = sums.row(e.getKey()).get(header);
          String formattedValue =
              value == null
                  ? ""
                  : formatters.getOrDefault(header, statFormatter::format).apply(value);
          formattedStatSums[column++] = formattedValue;
        }
      }

      formattedStatSums[column++] =
          statsForPlayer.stream()
              .map(p -> matchupsByStat.get(p).date.getYear())
              .distinct()
              .sorted()
              .map(Object::toString)
              .collect(joining(", "));

      result[row++] = formattedStatSums;
    }

    return result;
  }

  private static String formatInnings(Float value) {
    int intPart = (int) value.floatValue();
    float fracPart = value - intPart;
    int fracOuts = Math.round(fracPart * 3);
    checkState(fracOuts < 3, "Invalid innings value: %s", value);
    return String.format("%d.%d", intPart, fracOuts);
  }
}
