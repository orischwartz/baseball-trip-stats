# Baseball Boxscore Aggregator

This is a scraper for [baseball-reference.com](https://www.baseball-reference.com/) boxscores. Stats for all specified
games are aggregated and output in CSV format.

### Why?

We have an annual baseball trip where we go to a different city. This is a way to track cumulative stats for the players
we've seen over the years.

### Usage

Edit the boxscore-urls.txt file (or create your own) to include the URLs of the boxscores you want to aggregate. Then
run the program.

```bash
Usage: com.github.baseballtrip.Main [options]
  Options:
    --httpTimeout
      HTTP timeout in seconds
      Default: 4
  * --inFile
      List of baseball-reference URLs to scrape, separated by newlines.
    --outFile
      Location to output CSV. Outputs to stdout if not specified.
```

#### Example

```bash
mvn exec:java -Dexec.args="--inFile boxscore-urls.txt"

"player","AB","R","H","RBI","AVG","SLG","TB","2B","3B","HR","BB","SO","PA","SB","CS","IP","ER","R","BB","SO","BF","PIT","HR","ERA","K/9","WHIP","seasons"
"Brady Clark","9","1","2","0",".222",".333","3","1","","","0","0","9","","","","","","","","","","","","","","2005"
"Junior Spivey","7","0","2","0",".286",".286","2","","","","1","0","8","","","","","","","","","","","","","","2005"
"Geoff Jenkins","8","0","2","1",".250",".250","2","","","","0","1","8","","","","","","","","","","","","","","2005"
...
"Tony Armas","3","0","0","0",".000",".000","0","","","","0","1","3","","","7.0","2","2","2","3","28","","1","2.57","3.9","1","2005"
"Ryan Church","1","0","0","0",".000",".000","0","","","","0","1","1","","","","","","","","","","","","","","2005"
```

From there you can sort/filter the CSV.

| player        | AB | R | H | RBI | AVG  | SLG  | TB | 2B | 3B | HR | BB | SO | PA | SB | CS | IP  | ER | R | BB | SO | BF | PIT | HR | ERA  | K/9 | WHIP | seasons |
|---------------|----|---|---|-----|------|------|----|----|----|----|----|----|----|----|----|-----|----|---|----|----|----|-----|----|------|-----|------|---------|
| Brady Clark   | 9  | 1 | 2 | 0   | .222 | .333 | 3  | 1  |    |    | 0  | 0  | 9  |    |    |     |    |   |    |    |    |     |    |      |     |      | 2005    |
| Junior Spivey | 7  | 0 | 2 | 0   | .286 | .286 | 2  |    |    |    | 1  | 0  | 8  |    |    |     |    |   |    |    |    |     |    |      |     |      | 2005    |
| Geoff Jenkins | 8  | 0 | 2 | 1   | .250 | .250 | 2  |    |    |    | 0  | 1  | 8  |    |    |     |    |   |    |    |    |     |    |      |     |      | 2005    |
| Tony Armas    | 3  | 0 | 0 | 0   | .000 | .000 | 0  |    |    |    | 0  | 1  | 3  |    |    | 7.0 | 2  | 2 | 2  | 3  | 28 |     | 1  | 2.57 | 3.9 | 1    | 2005    |
| Ryan Church   | 1  | 0 | 0 | 0   | .000 | .000 | 0  |    |    |    | 0  | 1  | 1  |    |    |     |    |   |    |    |    |     |    |      |     |      | 2005    |

