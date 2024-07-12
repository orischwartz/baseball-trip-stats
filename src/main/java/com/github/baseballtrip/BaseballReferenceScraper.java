package com.github.baseballtrip;

import static java.lang.Integer.parseInt;
import static java.lang.Math.round;
import static java.util.regex.Pattern.compile;
import static java.util.stream.Collectors.toList;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableTable;
import com.google.common.io.Files;
import com.google.common.primitives.Floats;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Comment;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

class BaseballReferenceScraper {
  private final HttpFetcher fetcher;

  public BaseballReferenceScraper(HttpFetcher fetcher) {
    this.fetcher = fetcher;
  }

  public Boxscore parseBoxscore(URI uri) throws IOException {
    Document document = Jsoup.parse(fetcher.fetch(uri), uri.toString());
    return new Boxscore(parseMatchup(document), parsePlayerStats(document));
  }

  private ImmutableList<PlayerStat> parsePlayerStats(Document document) {
    ArrayList<PlayerStat> result = new ArrayList<>();

    // content is commented out in the HTML
    for (Comment comment : document.select(".commented").comments()) {
      Document parsedComment = Jsoup.parseBodyFragment(comment.getData());

      for (Element statTable : parsedComment.select("table.sortable.stats_table")) {
        result.addAll(parseStatsFromTable(statTable));
      }

      for (Element battingDetail : parsedComment.select(".footer.no_hide_long")) {
        parseStatsFromBattingDetail(battingDetail, result);
      }
    }

    return ImmutableList.copyOf(result);
  }

  private static final Pattern BATTING_DETAIL_VALUE =
      Pattern.compile("([\\p{L} \\.]+)(?: (\\d+))? (\\([^\\)]*\\);?)");
  private static final Pattern BATTING_DETAIL_LINE = Pattern.compile("([0-9A-ZA-z]+): (.*)\\.");

  private static final ImmutableSet<String> IGNORED_DETAIL_STATS = ImmutableSet.of("RBI");

  /** Ignore derived stats. */
  private static final ImmutableSet<String> IGNORED_BOXSCORE_STATS =
      ImmutableSet.of("OBP", "SLG", "OPS");

  private void parseStatsFromBattingDetail(Element battingDetail, ArrayList<PlayerStat> result) {
    for (Element label : battingDetail.select("strong")) {
      String text = label.parent().text();
      Matcher lineMatcher = BATTING_DETAIL_LINE.matcher(text);

      if (!lineMatcher.matches()) {
        continue;
      }

      String statName = lineMatcher.group(1);

      if (IGNORED_DETAIL_STATS.contains(statName)) {
        continue;
      }

      Matcher valueMatcher = BATTING_DETAIL_VALUE.matcher(lineMatcher.group(2));

      while (valueMatcher.find()) {
        String playerName = valueMatcher.group(1);
        String countStr = valueMatcher.group(2);
        int count = countStr != null ? parseInt(countStr) : 1;

        for (ListIterator<PlayerStat> it = result.listIterator(); it.hasNext(); ) {
          PlayerStat stat = it.next();

          if (stat.playerName.equals(playerName)) {
            it.set(stat.add(ImmutableTable.of("BATTING", statName, (float) count)));
            break;
          }
        }
      }
    }
  }

  private List<PlayerStat> parseStatsFromTable(Element statTable) {
    ArrayList<PlayerStat> result = new ArrayList<>();
    List<String> headers =
        statTable.select("thead th").stream().map(Element::text).collect(toList());
    String statGroup = statTable.selectFirst("thead th").attr("aria-label").toUpperCase();

    for (Element row : statTable.select("tbody tr")) {
      Element playerNameCell = row.selectFirst("th a");

      if (playerNameCell == null) {
        continue;
      }

      String playerId = parsePlayerIdFromUri(playerNameCell.attr("href"));
      String playerName = playerNameCell.text();

      result.add(
          new PlayerStat(
              playerId,
              playerName,
              toStatsTable(statGroup, parsePlayerStatsFromRow(row, headers))));
    }

    return result;
  }

  private static ImmutableTable<String, String, Float> toStatsTable(
      String statGroup, Map<String, Float> statsForGroup) {
    HashBasedTable<String, String, Float> result = HashBasedTable.create();
    result.row(statGroup).putAll(statsForGroup);
    return ImmutableTable.copyOf(result);
  }

  private ImmutableMap<String, Float> parsePlayerStatsFromRow(Element row, List<String> headers) {
    LinkedHashMap<String, Float> result = new LinkedHashMap<>();
    Elements cells = row.select("th, td");

    for (int i = 0; i < headers.size(); i++) {
      String header = headers.get(i);

      if (IGNORED_BOXSCORE_STATS.contains(header)) {
        continue;
      }

      String value = cells.get(i).text();

      Float parsedValue = Floats.tryParse(value);

      if (parsedValue != null) {
        if ("IP".equals(header)) {
          parsedValue = convertInningsPitchedToDecimal(parsedValue);
        }

        result.put(header, parsedValue);
      }
    }

    return ImmutableMap.copyOf(result);
  }

  private static float convertInningsPitchedToDecimal(float inningsPitched) {
    return convertInningsToOuts(inningsPitched) / 3f;
  }

  private static int convertInningsToOuts(float innings) {
    int intPart = (int) innings;
    float fracPart = innings - intPart;
    int fracPartAsInt = round(fracPart * 10);

    switch (fracPartAsInt) {
      case 1:
      case 2:
      case 3:
        return intPart * 3 + fracPartAsInt;

        // not a x.1, x.2, or x.3 value
      default:
        return round(innings * 3f);
    }
  }

  private static String parsePlayerIdFromUri(String playerUri) {
    return Files.getNameWithoutExtension(playerUri);
  }

  private static String proTeamFromTeamUri(String teamUri) {
    return teamUri.split("/")[2];
  }

  private Matchup parseMatchup(Document document) {
    String away =
        proTeamFromTeamUri(
            document
                .selectFirst("table.linescore tr:nth-child(1) > td:nth-child(2) > a")
                .attr("href"));
    String home =
        proTeamFromTeamUri(
            document
                .selectFirst("table.linescore tr:nth-child(2) > td:nth-child(2) > a")
                .attr("href"));

    return new Matchup(parseDateFromBoxscoreLink(document.baseUri()), away, home);
  }

  private static final Pattern BOXSCORE_URL_DATE_PATTERN =
      compile("[A-Za-z]+(\\d{4})(\\d{2})(\\d{2})\\d+\\.\\w+");

  private static LocalDate parseDateFromBoxscoreLink(String boxscoreUri) {
    try {
      String file = new File(new URL(boxscoreUri).getFile()).getName();
      Matcher matcher = BOXSCORE_URL_DATE_PATTERN.matcher(file);

      if (matcher.matches()) {
        return LocalDate.of(
            parseInt(matcher.group(1)), parseInt(matcher.group(2)), parseInt(matcher.group(3)));
      }

      throw new IllegalArgumentException(
          String.format(
              "%s does not match the expected pattern %s",
              file, BOXSCORE_URL_DATE_PATTERN.pattern()));
    } catch (IOException e) {
      throw new IllegalArgumentException(boxscoreUri + " is not a valid URL", e);
    }
  }
}
