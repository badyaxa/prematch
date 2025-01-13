package com.leonbets.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.leonbets.model.EventData;
import com.leonbets.model.LeonEvent;
import com.leonbets.model.LeonLeague;
import com.leonbets.model.LeonMarket;
import com.leonbets.model.LeonRegion;
import com.leonbets.model.LeonRunner;
import com.leonbets.model.LeonSportEvent;
import com.leonbets.service.LeonService;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
public class LeonApiJsonService implements LeonService {

    public final String SPORTS_URL = "https://leonbets.com/api-2/betline/sports?ctag=en-US";

    private static final Integer ROW_LIMIT = 3;

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void printToConsole() {
        for (LeonSportEvent event : getSportEvents()) {
            log.info("Parsing event: {}", event.getName());
            parse(event);
        }
    }

    private List<LeonSportEvent> getSportEvents() {
        HttpRequest sportsRequest = HttpRequest.newBuilder()
                .uri(URI.create(SPORTS_URL))
                .headers("Content-Type", "application/json")
                .build();
        CompletableFuture<HttpResponse<String>> response = client.sendAsync(
                sportsRequest,
                HttpResponse.BodyHandlers.ofString());
        CollectionType toSportsEventType = objectMapper.getTypeFactory()
                .constructCollectionType(List.class, LeonSportEvent.class);
        try {
            return objectMapper.readValue(response.get().body(), toSportsEventType);
        } catch (JsonProcessingException | InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void parse(LeonSportEvent event) {
        for (LeonRegion region : event.getRegions()) {
            parse(event, region);
        }
    }

    private void parse(LeonSportEvent event, LeonRegion region) {
        for (LeonLeague league : region.getLeagues()) {
            parse(event, region, league);
        }
    }

    private void parse(LeonSportEvent event, LeonRegion region, LeonLeague league) {
        if (league.getTop()) {
            System.out.println(event.getName() + ", " + region.getName() + " " + league.getName());

            getEvents(league).stream()
                    .limit(ROW_LIMIT)
                    .forEach(ev -> {
                        printToConsole(ev);
                        parse(ev);
                    });
        }
    }

    private List<LeonEvent> getEvents(LeonLeague league) {
        final String EVENTS_URL = "https://leonbets.com/api-2/betline/events/all?ctag=en-US&league_id="
                + league.getId()
                + "&hideClosed=true&flags=reg,urlv2,mm2,rrc,nodup";

        HttpRequest eventsRequest = HttpRequest.newBuilder()
                .uri(URI.create(EVENTS_URL))
                .headers("Content-Type", "application/json")
                .build();
        CompletableFuture<HttpResponse<String>> eventsResponse = client.sendAsync(
                eventsRequest,
                HttpResponse.BodyHandlers.ofString());
        EventData eventsData;
        try {
            eventsData = objectMapper.readValue(
                    eventsResponse.get().body(),
                    objectMapper.getTypeFactory().constructType(EventData.class));
        } catch (JsonProcessingException | InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        return eventsData.getEvents();
    }

    private void parse(LeonEvent ev) {
        if (ev.getMarkets() == null) {
            return;
        }
        ev.getMarkets().stream()
                .limit(ROW_LIMIT)
                .forEach(market -> {
                    printToConsole(market);
                    market.getRunners().stream()
                            .limit(ROW_LIMIT)
                            .forEach(this::printToConsole);
                });

    }

    private void printToConsole(LeonEvent event) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
                .withZone(ZoneId.of("UTC"));
        System.out.println("    " + event.getName() + ", "
                + formatter.format(Instant.ofEpochMilli(event.getKickoff())) + ", "
                + event.getId());
    }

    private void printToConsole(LeonMarket market) {
        System.out.println("        " + market.getName());
    }

    private void printToConsole(LeonRunner runner) {
        System.out.println("            " + runner.getName() + ", "
                + runner.getPriceStr() + ", "
                + runner.getId());
    }
}
