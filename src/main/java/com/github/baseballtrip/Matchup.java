package com.github.baseballtrip;

import java.time.LocalDate;

public class Matchup {
  public final LocalDate date;
  public final String away;
  public final String home;

  public Matchup(LocalDate date, String away, String home) {
    this.date = date;
    this.away = away;
    this.home = home;
  }

  @Override
  public String toString() {
    return String.format("%s @ %s %s", away, home, date);
  }
}
