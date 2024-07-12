package com.github.baseballtrip;

import static com.google.common.io.Files.readLines;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class Main {

  @Parameter(
      names = "--inFile",
      description = "List of baseball-reference URLs to scrape, separated by newlines.",
      required = true)
  private File inFile;

  @Parameter(
      names = "--outFile",
      description = "Location to output CSV. Outputs to stdout if not specified.")
  private File outFile;

  @Parameter(names = "--httpTimeout", description = "HTTP timeout in seconds")
  private int httpTimeoutSeconds = 4;

  @Parameter(names = "--help", help = true)
  private boolean help = false;

  public static void main(String[] args) throws IOException {
    Main main = new Main();
    JCommander jCommander =
        JCommander.newBuilder().programName(Main.class.getName()).addObject(main).build();
    jCommander.parse(args);

    if (main.help) {
      jCommander.usage();
    } else {
      main.run();
    }
  }

  public void run() throws IOException {
    List<Boxscore> boxscores =
        parseAll(loadBaseballReferenceUrisToScrape(inFile), Duration.ofSeconds(httpTimeoutSeconds));

    OutputStream outputStream = outFile == null ? System.out : new FileOutputStream(outFile);
    CsvPrinter printer = new CsvPrinter(new PrintWriter(outputStream, true, UTF_8));
    printer.printCsv(new BaseballCsvFormatter().toCsvCells(boxscores));
  }

  private static List<Boxscore> parseAll(Set<URI> uris, Duration httpTimeout) throws IOException {
    ArrayList<Boxscore> result = new ArrayList<>();
    BaseballReferenceScraper scraper =
        new BaseballReferenceScraper(new BaseballReferenceFetcher(httpTimeout));

    for (URI uri : uris) {
      result.add(scraper.parseBoxscore(uri));
    }

    return result;
  }

  private static Set<URI> loadBaseballReferenceUrisToScrape(File baseballReferenceUrlListFile)
      throws IOException {
    List<String> lines = readLines(baseballReferenceUrlListFile, UTF_8);

    LinkedHashSet<URI> result = new LinkedHashSet();

    for (String line : lines) {
      if (line.isEmpty() || line.startsWith("#")) {
        continue;
      }

      result.add(URI.create(line));
    }

    return result;
  }
}
