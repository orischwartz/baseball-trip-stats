package com.github.baseballtrip;

import java.io.PrintWriter;

public class CsvPrinter {
  private final PrintWriter output;

  public CsvPrinter(PrintWriter output) {
    this.output = output;
  }

  public void printCsv(String[][] data) {
    for (String[] row : data) {

      for (int i = 0; i < row.length; i++) {
        if (i > 0) {
          output.print(",");
        }

        output.print('"');
        output.print(escape(row[i]));
        output.print('"');
      }

      output.println();
    }

    output.flush();
  }

  private static String escape(String value) {
    return value.replace("\"", "\"\"");
  }
}
