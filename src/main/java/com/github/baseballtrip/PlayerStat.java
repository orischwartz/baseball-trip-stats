package com.github.baseballtrip;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import java.util.List;
import java.util.Map;

public class PlayerStat {
  public final String playerId;

  public final String playerName;

  public final ImmutableTable<String, String, Float> stats;

  public PlayerStat(
      String playerId, String playerName, ImmutableTable<String, String, Float> stats) {
    this.playerId = playerId;
    this.playerName = playerName;
    this.stats = stats;
  }

  public static ImmutableTable<String, String, Float> sumAndAddDerived(List<PlayerStat> stats) {
    return addDerivedStats(sumStats(Lists.transform(stats, stat -> stat.stats)));
  }

  public static ImmutableTable<String, String, Float> sumStats(
      List<? extends Table<String, String, Float>> statsToSum) {
    HashBasedTable<String, String, Float> result = HashBasedTable.create();

    for (Table<String, String, Float> other : statsToSum) {
      for (Table.Cell<String, String, Float> cell : other.cellSet()) {
        String group = cell.getRowKey();
        String statName = cell.getColumnKey();
        Float previousValue = result.row(group).getOrDefault(statName, 0f);
        result.put(group, statName, previousValue + cell.getValue());
      }
    }

    return ImmutableTable.copyOf(result);
  }

  public static ImmutableTable<String, String, Float> addDerivedStats(
      Table<String, String, Float> stats) {
    HashBasedTable<String, String, Float> result = HashBasedTable.create();
    result.putAll(stats);

    if (stats.row("BATTING").getOrDefault("AB", 0f) > 0) {
      Map<String, Float> batting = stats.row("BATTING");
      float ab = batting.getOrDefault("AB", 0f);
      float h = batting.getOrDefault("H", 0f);
      float doubles = batting.getOrDefault("2B", 0f);
      float triples = batting.getOrDefault("3B", 0f);
      float hrs = batting.getOrDefault("HR", 0f);

      float tb = h + doubles + 2 * triples + 3 * hrs;
      float average = h / ab;
      float slugging = tb / ab;

      result.put("BATTING", "AVG", average);
      result.put("BATTING", "TB", tb);
      result.put("BATTING", "SLG", slugging);
    }

    if (stats.row("PITCHING").getOrDefault("IP", 0f) > 0) {
      Map<String, Float> pitching = stats.row("PITCHING");
      float ip = pitching.getOrDefault("IP", 0f);
      float h = pitching.getOrDefault("H", 0f);
      float er = pitching.getOrDefault("ER", 0f);
      float bb = pitching.getOrDefault("BB", 0f);
      float so = pitching.getOrDefault("SO", 0f);

      float era = 9 * er / ip;
      float whip = (bb + h) / ip;
      float k9 = 9 * so / ip;

      result.put("PITCHING", "ERA", era);
      result.put("PITCHING", "WHIP", whip);
      result.put("PITCHING", "K/9", k9);
    }

    return ImmutableTable.copyOf(result);
  }

  public PlayerStat add(Table<String, String, Float> other) {
    return new PlayerStat(playerId, playerName, sumStats(List.of(this.stats, other)));
  }

  @Override
  public String toString() {
    return String.format("%s %s %s", playerId, playerName, stats);
  }
}
