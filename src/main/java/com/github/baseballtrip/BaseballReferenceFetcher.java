package com.github.baseballtrip;

import static com.google.common.io.Resources.getResource;
import static java.util.stream.Collectors.toMap;

import com.google.common.util.concurrent.RateLimiter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;

class BaseballReferenceFetcher implements HttpFetcher {
  private final HttpClient client;

  private final RateLimiter rateLimiter;

  /** https://www.sports-reference.com/bot-traffic.html */
  private static final int MAX_REQUESTS_PER_MINUTE = 20;

  private final Duration httpTimeout;

  private final Map<String, String> httpHeaders =
      readHttpHeadersFromConfig(getResource("http-headers.properties"));

  private static Map<String, String> readHttpHeadersFromConfig(URL configUrl) {
    try (InputStream input = configUrl.openStream()) {
      Properties result = new Properties();
      result.load(input);
      return propertiesToStringMap(result);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Map<String, String> propertiesToStringMap(Properties props) {
    return props.stringPropertyNames().stream().collect(toMap(key -> key, props::getProperty));
  }

  public BaseballReferenceFetcher(Duration httpTimeout) {
    this.client =
        HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(httpTimeout)
            .build();
    this.httpTimeout = httpTimeout;
    this.rateLimiter = RateLimiter.create(MAX_REQUESTS_PER_MINUTE / 60.0d);
  }

  @Override
  public String fetch(URI uri) throws IOException {
    rateLimiter.acquire();
    try {
      return client.send(buildRequest(uri), HttpResponse.BodyHandlers.ofString()).body();
    } catch (InterruptedException e) {
      throw new IOException(e);
    }
  }

  private HttpRequest buildRequest(URI uri) {
    HttpRequest.Builder result = HttpRequest.newBuilder();

    for (Map.Entry<String, String> e : httpHeaders.entrySet()) {
      result.header(e.getKey(), e.getValue());
    }

    return result.uri(uri).timeout(httpTimeout).GET().build();
  }
}
