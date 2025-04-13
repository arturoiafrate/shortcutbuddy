package it.arturoiafrate.shortcutbuddy.service.impl; // O tuo package

import lombok.extern.slf4j.Slf4j;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture; // Per chiamate asincrone

@Slf4j
public class GithubApiClient {

    private static final String GITHUB_API_BASE = "https://api.github.com/repos/arturoiafrate/ShortcutBuddy";
    private final HttpClient httpClient;

    public GithubApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public CompletableFuture<String> fetchLatestReleaseJsonAsync() {
        String url = GITHUB_API_BASE + "/releases/latest";
        log.info("Fetching latest release info ASYNC from: {}", url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/vnd.github.v3+json")
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    if (response.statusCode() == 200) {
                        return response.body();
                    } else {
                        log.error("Error from API GitHub (Latest Release): Status={}, Body={}", response.statusCode(), response.body());
                        return null;
                    }
                })
                .exceptionally(e -> {
                    log.error("Exception during API GitHub request (Latest Release)", e);
                    return null;
                });
    }
}