package com.leonbets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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

public class WebSoccerFilter {

    public static final String SPORTS_URL = "https://leonbets.com/api-2/betline/sports?ctag=en-US";

    public static void main(String[] args) throws ExecutionException, InterruptedException, JsonProcessingException {
        int counter = 0;
        HttpClient client = HttpClient.newHttpClient();

        ObjectMapper objectMapper = new ObjectMapper();

        HttpRequest sportsRequest = HttpRequest.newBuilder()
                .uri(URI.create(SPORTS_URL))
                .build();

        CompletableFuture<HttpResponse<String>> response = client
                .sendAsync(sportsRequest, HttpResponse.BodyHandlers.ofString());

        List<SportsEvent> sportsEvents = objectMapper.readValue(response.get().body(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, SportsEvent.class));
        counter++;

        for (SportsEvent event : sportsEvents) {

            for (Region region : event.getRegions()) {

                for (League league : region.getLeagues()) {
                    if (league.getTop()) {
                        System.out.println(event.getName() + ", " + region.getName() + " " + league.getName());
                        Long leagueId = league.getId();
                        String EVENTS_URL = "https://leonbets.com/api-2/betline/events/all?ctag=en-US&league_id=" + leagueId + "&hideClosed=true&flags=reg,urlv2,mm2,rrc,nodup";
                        HttpRequest eventsRequest = HttpRequest.newBuilder()
                                .uri(URI.create(EVENTS_URL))
                                .headers("Content-Type", "application/json")
                                .build();

                        CompletableFuture<HttpResponse<String>> eventsResponse = client.sendAsync(eventsRequest, HttpResponse.BodyHandlers.ofString());
                        EventData eventsData = objectMapper.readValue(eventsResponse.get().body(), objectMapper.getTypeFactory().constructType(EventData.class));
                        counter++;
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z").withZone(ZoneId.of("UTC"));
                        eventsData.getEvents()
                                .stream().limit(2)
                                .forEach(ev -> {
                                    System.out.println("    " + ev.getName() + ", " + formatter.format(Instant.ofEpochMilli(ev.getKickoff())) + ", " + ev.getId());
                                    if (ev.getMarkets() != null) {
                                        ev.getMarkets()
                                                .stream().limit(2)
                                                .forEach(
                                                        market -> {
                                                            System.out.println("        " + market.getName());
                                                            market.getRunners()
                                                                    .stream().limit(3)
                                                                    .forEach(
                                                                            runner -> {
                                                                                System.out.println(
                                                                                        "            "
                                                                                        + runner.getName()
                                                                                        + ", "
                                                                                        + runner.getPriceStr()
                                                                                        + ", "
                                                                                        + runner.getId());
                                                                            }
                                                                    );
                                                        }
                                                );
                                    }
                                });
                    }
                }
            }
        }
        System.out.println("Total number of requests: " + counter);
    }
}
