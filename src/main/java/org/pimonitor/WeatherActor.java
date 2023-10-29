package org.pimonitor;

import akka.actor.AbstractActorWithTimers;
import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.DeciderBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;

public class WeatherActor extends AbstractActorWithTimers {

    private static ActorRef ui;
    private static final Object TICK_KEY = "TickKey";
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final OkHttpClient client;
    private final Gson gson;

    public WeatherActor(ActorRef ui) {
        getSelf().tell("tick", getSelf());
        getTimers().startTimerAtFixedRate(TICK_KEY, "tick", Duration.ofMillis(300000));
        WeatherActor.ui = ui;
        client = new OkHttpClient();
        gson = new Gson();
    }

    private static final SupervisorStrategy strategy =
            new OneForOneStrategy(
                    10,
                    Duration.ofMinutes(1),
                    DeciderBuilder.match(Exception.class, e -> SupervisorStrategy.restart())
                            .matchAny(o -> SupervisorStrategy.escalate())
                            .build());

    @Override
    public SupervisorStrategy supervisorStrategy() {
        return strategy;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(
                String.class,
                message -> ui.tell(getWeatherData(), getSelf()))
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }

    private JsonObject getWeatherData() throws IOException {

        HttpUrl.Builder urlBuilder
                = Objects.requireNonNull(HttpUrl.parse("https://api.open-meteo.com" + "/v1/forecast")).newBuilder();
        urlBuilder.addQueryParameter("latitude", "53.551086");
        urlBuilder.addQueryParameter("longitude", "9.993682");
        urlBuilder.addQueryParameter("current", "temperature_2m,apparent_temperature,windspeed,winddirection,weathercode");
        urlBuilder.addQueryParameter("hourly", "apparent_temperature");
        urlBuilder.addQueryParameter("hourly", "temperature_2m");
        urlBuilder.addQueryParameter("hourly", "relativehumidity_2m");
        urlBuilder.addQueryParameter("hourly", "rain");
        urlBuilder.addQueryParameter("hourly", "precipitation_probability");
        urlBuilder.addQueryParameter("hourly", "windspeed_10m");
        urlBuilder.addQueryParameter("hourly", ",windgusts_10m");
        urlBuilder.addQueryParameter("hourly", ",pressure_msl,surface_pressure");
        urlBuilder.addQueryParameter("daily", "uv_index_max");
        urlBuilder.addQueryParameter("timezone", "Europe/Berlin");
        urlBuilder.addQueryParameter("timeformat", "unixtime");
        urlBuilder.addQueryParameter("windspeed_unit", "kn");

        String url = urlBuilder.build().toString();

        Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = client.newCall(request);
        Response response = call.execute();

        String responseString = Objects.requireNonNull(response.body()).string();
        return gson.fromJson(responseString, JsonObject.class);
    }
}
