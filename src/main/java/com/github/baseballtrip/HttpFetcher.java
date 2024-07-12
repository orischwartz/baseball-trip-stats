package com.github.baseballtrip;

import java.io.IOException;
import java.net.URI;

interface HttpFetcher {
  String fetch(URI uri) throws IOException;
}
