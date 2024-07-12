package com.github.baseballtrip;

import static com.google.common.collect.Multimaps.index;
import static com.google.common.io.Resources.getResource;
import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.io.Resources;
import com.google.common.truth.Truth;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import org.junit.Before;
import org.junit.Test;

public class BaseballReferenceScraperTest {
  private final BaseballReferenceScraper scraper =
      new BaseballReferenceScraper(url -> Resources.toString(url.toURL(), UTF_8));

  private ImmutableListMultimap<String, PlayerStat> yanksSoxStatsByPlayerId;

  private Boxscore yanksSoxBoxscore;

  @Before
  public void parseNyyAtChw() throws IOException, URISyntaxException {
    yanksSoxBoxscore = scraper.parseBoxscore(uriForResourceName("CHA200908020.shtml"));
    yanksSoxStatsByPlayerId = index(yanksSoxBoxscore.stats, stat -> stat.playerId);
  }

  private static URI uriForResourceName(String resourceName) throws URISyntaxException {
    return getResource(resourceName).toURI();
  }

  @Test
  public void parseBoxscore_parsesMatchup() throws URISyntaxException, IOException {
    Matchup matchup = yanksSoxBoxscore.matchup;
    Truth.assertThat(matchup.away).isEqualTo("NYY");
    Truth.assertThat(matchup.home).isEqualTo("CHW");
    Truth.assertThat(matchup.date).isEqualTo(LocalDate.of(2009, 8, 2));
  }

  @Test
  public void parseBoxscore_parsesStats_batting() throws URISyntaxException, IOException {
    assertThat(yanksSoxBoxscore.stats).hasSize(41);

    ImmutableList<PlayerStat> cabreraStats = yanksSoxStatsByPlayerId.get("cabreme01");
    assertThat(cabreraStats).hasSize(1);
    PlayerStat cabrera = cabreraStats.get(0);
    Truth.assertThat(cabrera.playerName).isEqualTo("Melky Cabrera");

    Truth.assertThat(cabrera.stats.rowKeySet()).hasSize(1);
    ImmutableMap<String, Float> batting = cabrera.stats.row("BATTING");
    assertThat(batting).containsEntry("AB", 5f);
    assertThat(batting).containsEntry("R", 3f);
    assertThat(batting).containsEntry("H", 4f);
    assertThat(batting).containsEntry("RBI", 4f);
    assertThat(batting).containsEntry("2B", 1f);
    assertThat(batting).containsEntry("3B", 1f);
    assertThat(batting).containsEntry("HR", 1f);
  }

  @Test
  public void parseBoxscore_parsesStats_pitching() {
    ImmutableList<PlayerStat> hughesStats = yanksSoxStatsByPlayerId.get("hugheph01");
    assertThat(hughesStats).hasSize(2);
    PlayerStat hughes =
        hughesStats.stream()
            .filter(stat -> stat.stats.rowKeySet().contains("PITCHING"))
            .findFirst()
            .orElse(null);
    Truth.assertThat(hughes).isNotNull();

    ImmutableMap<String, Float> pitching = hughes.stats.row("PITCHING");
    assertThat(pitching).containsEntry("IP", 2 / 3f);
    assertThat(pitching).containsEntry("BB", 1f);
    assertThat(pitching).containsEntry("BF", 3f);
    assertThat(pitching).containsEntry("Pit", 14f);
    assertThat(pitching).containsEntry("R", 0f);
  }

  @Test
  public void parseBoxscore_parsesStats_pitching_derived() {
    ImmutableTable<String, String, Float> hughes =
        PlayerStat.sumAndAddDerived(yanksSoxStatsByPlayerId.get("sabatc.01"));
    ImmutableMap<String, Float> pitching = hughes.row("PITCHING");
    assertThat(pitching).containsEntry("ERA", 6.428571f);
    assertThat(pitching).containsEntry("WHIP", 1.4285715f);
    assertThat(pitching).containsEntry("K/9", 6.428571f);
  }

  @Test
  public void parseBoxscore_parsesStats_batting_derived() {
    ImmutableTable<String, String, Float> hughes =
        PlayerStat.sumAndAddDerived(yanksSoxStatsByPlayerId.get("cabreme01"));
    ImmutableMap<String, Float> pitching = hughes.row("BATTING");
    assertThat(pitching).containsEntry("AVG", .8f);
    assertThat(pitching).containsEntry("SLG", 2f);
    assertThat(pitching).containsEntry("TB", 10f);
  }

  @Test
  public void parseBoxscore_parsesStats_multipleDetails() throws URISyntaxException, IOException {
    Boxscore boxscore = scraper.parseBoxscore(uriForResourceName("BOS201805020.shtml"));
    ImmutableListMultimap<String, PlayerStat> byPlayerId =
        index(boxscore.stats, stat -> stat.playerId);
    ImmutableTable<String, String, Float> betts =
        PlayerStat.sumAndAddDerived(byPlayerId.get("bettsmo01"));
    assertThat(betts.get("BATTING", "HR")).isEqualTo(3f);
  }
}
