package ru.qivan;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author sokolov
 */
@Slf4j
public class WebCrawler {

    private static final String GOOGLE_QUERY = "http://www.google.com/search?&ie=utf-8&oe=utf-8&q=";

    private final HttpClient httpClient;
    private final TopNStorage<String> storage;

    public WebCrawler(HttpClient httpClient, TopNStorage<String> storage) {
        this.httpClient = httpClient;
        this.storage = storage;
    }


    public static void main(String[] args) throws ExecutionException, InterruptedException {
        Optional<String> param = Arrays.stream(args).findFirst();
        if (param.isEmpty()) {
            Scanner sc = new Scanner(System.in);
            System.out.println("Please enter a query param");
            param = Optional.of(sc.nextLine());
        }
        String userQuery = param.get();
        log.info("You have passed = {}", userQuery);

        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        TopNStorage<String> storage = new TopNStorage<>(5);

        WebCrawler crawler = new WebCrawler(httpClient, storage);
        CompletableFuture<List<String>> futureResult =
                crawler.lookupLibs(GOOGLE_QUERY + userQuery);

        System.out.println("Parse statistic: \n" + futureResult.get());
        System.out.println();
        System.out.println();
        System.out.println();
        System.out.println("Top 5 libs:\n" + storage.getTop());

    }

    CompletableFuture<List<String>> lookupLibs(String googleQuery) {
        return downloadPage(googleQuery)
                .thenApply(WebCrawler::parseGoogleResultPage)
                .thenCompose(urls -> {
                    List<CompletableFuture<String>> googleResults =
                            urls.stream()
                                    .map(url -> downloadPageAndFindLibs(url, storage::add))
                                    .collect(Collectors.toList());

                    return joinFutures(googleResults);
                });
    }

    static List<String> parseGoogleResultPage(String body) {
        Document doc = Jsoup.parse(body);
        Elements firstPageLinks = doc.select("div.kCrYT > a[href]");
        return firstPageLinks.stream()
                .map(s -> s.attr("href"))
                .map(WebCrawler::removeUrlQ)
                .map(WebCrawler::extractBeforeAmpersand)
                .map(WebCrawler::extractBeforeQuestionMark)
                .collect(Collectors.toList());
    }

    CompletableFuture<String> downloadPageAndFindLibs(String url, Consumer<String> libConsumer) {
        return downloadPage(url)
                .thenApply(body -> url + " has " + extractLibs(body, libConsumer) + " js libs")
                .exceptionally(throwable -> {
                    log.error(throwable.getMessage());
                    return "no results for " + url + "... reason: " + throwable.getMessage();
                });

    }

    CompletableFuture<String> downloadPage(String url) {
        log.info("download " + url);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .setHeader("User-Agent", "Mozilla")
                .build();
        HttpResponse.BodyHandler<String> bodyHandler = HttpResponse.BodyHandlers.ofString();


        return httpClient.sendAsync(request, bodyHandler)
                .thenApply(WebCrawler::extractBody);
    }


    static <T> CompletableFuture<List<T>> joinFutures(List<CompletableFuture<T>> googleResults) {
        return CompletableFuture.allOf(googleResults.toArray(new CompletableFuture[0]))
                .thenApply(ignore ->
                                   googleResults.stream()
                                           .map(CompletableFuture::join)
                                           .collect(Collectors.toList()));
    }

    static int extractLibs(String body, Consumer<String> libConsumer) {
        int[] result = new int[1];
        Document document = Jsoup.parse(body);
        document.select("script").stream()
                .map(script -> script.attr("src"))
                .filter(script -> !script.isEmpty())
                .map(WebCrawler::extractBeforeAmpersand)
                .map(WebCrawler::extractBeforeJsExtension)
                .forEach(lib -> {
                    result[0]++;
                    libConsumer.accept(lib);
                });
        return result[0];
    }

    static String extractBody(HttpResponse<String> response) {
        log.info("extract body for url " + response.uri());
        if (!response.body().startsWith("<!")) {
            throw new RuntimeException("unsupported body type");
        }
        return response.body();
    }

    static String extractBeforeAmpersand(String url) {
        if (url.contains("&")) {
            return url.substring(0, url.indexOf("&"));
        }
        return url;
    }

    static String extractBeforeJsExtension(String libName) {
        String result = libName;
        if (libName.contains(".js") && !libName.endsWith(".js")) {
            result = libName.substring(0, libName.indexOf(".js") + 3);
        }
        return result;
    }

    static String extractBeforeQuestionMark(String url) {
        String tmp = url;
        if (url.contains("?")) {
            tmp = url.substring(0, url.indexOf("?"));
        }
        if (url.contains("%3F")) {
            tmp = url.substring(0, url.indexOf("%3F"));
        }
        return tmp;
    }

    static String removeUrlQ(String url) {
        return url.replace("/url?q=", "");
    }


}
