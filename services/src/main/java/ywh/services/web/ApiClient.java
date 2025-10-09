package ywh.services.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.sun.jna.platform.win32.Guid;
import ywh.commons.TextUtils;
import ywh.commons.data.ConsoleColor;
import ywh.services.exceptions.ApiClientException;
import ywh.logging.IServiceLogger;
import ywh.logging.ServiceLoggerFactory;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;


public class ApiClient {
    private final HttpClient client;
    private final Gson gson;
    private final String baseUri;
    private final Map<String, String> queryParams;

    private IServiceLogger logger = ServiceLoggerFactory.createLogger("http client", ConsoleColor.GRAY);

    public static ApiClient create(String uri) {
        return create(uri, Duration.ofSeconds(40));
    }

    public static ApiClient create(String uri, Duration timeout) {
        return new ApiClient(uri,timeout);
    }

    private ApiClient(String baseUri, Duration timeout) {
        this.baseUri = baseUri;
        this.queryParams = new HashMap<>();
        this.client = HttpClient.newBuilder()
                .version(baseUri.contains("https") ? HttpClient.Version.HTTP_2 : HttpClient.Version.HTTP_1_1)
                .connectTimeout(timeout)
                .build();

        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();
    }

    public ApiClient logger(IServiceLogger logger) {
        this.logger = logger;
        return this;
    }


    public ApiClient addUriParam(String paramName, String paramValue) {
        if (TextUtils.isNotNullOrEmpty(paramName, paramValue)) {
            queryParams.put(paramName, paramValue);
        }
        return this;
    }

    public ApiClient clearUriParams() {
        queryParams.clear();
        return this;
    }


    public ApiClient removeUriParam(String paramName) {
        queryParams.remove(paramName);
        return this;
    }
    private String buildFullUri() {
        if (queryParams.isEmpty()) {
            return baseUri;
        }

        StringBuilder uriBuilder = new StringBuilder(baseUri);
        uriBuilder.append("?");

        queryParams.entrySet().stream()
                .map(entry -> {
                    try {
                        return URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) +
                                "=" +
                                URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        logger.error("Failed to encode URI parameter: " + entry.getKey(), e);
                        return entry.getKey() + "=" + entry.getValue();
                    }
                }).forEach(param -> uriBuilder.append(param).append("&"));

        if (uriBuilder.charAt(uriBuilder.length() - 1) == '&') {
            uriBuilder.setLength(uriBuilder.length() - 1);
        }

        return uriBuilder.toString();
    }

    private HttpRequest.Builder requestBuilder() throws ApiClientException {
        String fullUri = buildFullUri();
        if (TextUtils.isNullOrEmpty(fullUri))
            throw new ApiClientException("No URI specified");

        return HttpRequest.newBuilder()
                .uri(URI.create(fullUri))
                .timeout(client.connectTimeout().orElse(Duration.ofSeconds(40)))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Accept", "*/*")
                .header("User-Agent", "mini connection driver/1.0")
                .header("Cache", "cache-control: no cache")
                .header("Cache-Control", "no-cache");
    }

    private <REQ> String getBody(REQ requestDto) throws ApiClientException {
        if (requestDto == null) {
            logger.error("Request Dto can't be null. Will throw ApiClientException.");
            throw new ApiClientException("Request Dto can't be null");
        }
        return gson.toJson(requestDto);
    }


    public <REQ, RES> Optional<RES> post(REQ requestDto, Class<RES> responseClass) {
        try {
            var guid = logStart("POST");
            var requestBody = getBody(requestDto);
            var builder = requestBuilder();
            builder.POST(HttpRequest.BodyPublishers.ofString(requestBody));
            HttpRequest request = builder.build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            logFinish(guid, "POST", requestBody, response.body());
            return Optional.ofNullable(gson.fromJson(response.body(), responseClass));
        } catch (JsonSyntaxException ex) {
            logger.error("Failed to parse JSON response", ex);
        } catch (InterruptedException ex) {
            logger.error("Request was interrupted", ex);
            Thread.currentThread().interrupt(); // Відновлюємо статус переривання
        } catch (Exception ex) {
            logger.error("Failed to send request", ex);
        }
        return Optional.empty();
    }


    public <REQ, RES> CompletableFuture<Optional<RES>> postAsync(REQ requestDto, Class<RES> responseClass) {
        try {
            var guid = logStart("POST");
            var requestBody = getBody(requestDto);
            var builder = requestBuilder();
            builder.POST(HttpRequest.BodyPublishers.ofString(requestBody));
            HttpRequest request = builder.build();
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        try {
                            logFinish(guid, "POST", requestBody, response.body());
                            RES result = gson.fromJson(response.body(), responseClass);
                            return Optional.ofNullable(result);
                        } catch (JsonSyntaxException ex) {
                            logger.error("Failed to parse JSON response", ex);
                            return Optional.<RES>empty();
                        }
                    })
                    .exceptionally(throwable -> {
                        logger.error("Failed to send request", throwable);
                        return Optional.empty();
                    });

        } catch (Exception ex) {
            logger.error("Failed to prepare request", ex);
            // Повертаємо CompletableFuture з порожнім Optional при помилці підготовки запиту
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }

    private String logStart(String method) {
        String guid = Guid.GUID.newGuid().toGuidString();
        logger.log(guid + " - " + method + " request started...");
        return guid;
    }

    private void logFinish(String guid, String method, String request, String response) {
        logger.log(guid + " - " + method + " request finished...");
        logger.log("REQUEST : \n" + request);
        logger.log("RESPONSE : \n" + response + "\n------------------------------");
    }

    public <RES> Optional<RES> get(Class<RES> responseClass) {
        try {
            var guid = logStart("GET");
            var builder = requestBuilder();
            builder.GET();
            HttpRequest request = builder.build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            logFinish(guid, "GET", request.uri().toASCIIString(), response.body());
            return Optional.ofNullable(gson.fromJson(response.body(), responseClass));
        } catch (JsonSyntaxException ex) {
            logger.error("Failed to parse JSON response", ex);
        }
        catch (InterruptedException ex) {
            logger.error("GET request was interrupted", ex);
            Thread.currentThread().interrupt(); // Відновлюємо статус переривання
        }
        catch (Exception ex) {
            logger.error("Failed to send GET request", ex);
        }
        return Optional.empty();
    }

    /**
     * Асинхронний GET запит
     */
    public <RES> CompletableFuture<Optional<RES>> getAsync(Class<RES> responseClass) {
        try {
            var guid = logStart("GET");
            var builder = requestBuilder();
            builder.GET(); // GET запит
            HttpRequest request = builder.build();
            return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        try {
                            logFinish(guid, "GET", request.uri().toASCIIString(), response.body());
                            RES result = gson.fromJson(response.body(), responseClass);
                            return Optional.ofNullable(result);
                        } catch (JsonSyntaxException ex) {
                            logger.error("Failed to parse JSON response", ex);
                            return Optional.<RES>empty();
                        }
                    })
                    .exceptionally(throwable -> {
                        logger.error("Failed to send GET request", throwable);
                        return Optional.<RES>empty();
                    });

        } catch (Exception ex) {
            logger.error("Failed to prepare GET request", ex);
            return CompletableFuture.completedFuture(Optional.empty());
        }
    }


}
