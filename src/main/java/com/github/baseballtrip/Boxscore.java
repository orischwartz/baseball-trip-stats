package com.github.baseballtrip;

import com.google.common.collect.ImmutableList;

public class Boxscore {
  public final Matchup matchup;
  public final ImmutableList<PlayerStat> stats;

  public Boxscore(Matchup matchup, ImmutableList<PlayerStat> stats) {
    this.matchup = matchup;
    this.stats = stats;
  }

  @Override
  public String toString() {
    return String.format("%s %s", matchup, stats);
  }
}
