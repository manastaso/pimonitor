package org.pimonitor;

import akka.actor.AbstractActorWithTimers;
import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.DeciderBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

public class ClockActor extends AbstractActorWithTimers {

    private static ActorRef ui;
    private static final Object TICK_KEY = "TickKey";
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final OkHttpClient client;
    private final Gson gson;

    public ClockActor(ActorRef ui) {
        getSelf().tell("tick", getSelf());
        getTimers().startTimerAtFixedRate(TICK_KEY, "tick", Duration.ofMinutes(1));
        ClockActor.ui = ui;
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
                message -> ui.tell(getClockData(), getSelf()))
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }

    private Long getClockData() throws IOException {

        HttpUrl.Builder urlBuilder
                = Objects.requireNonNull(HttpUrl.parse("https://worldtimeapi.org/api/timezone/Europe/Berlin")).newBuilder();

        String url = urlBuilder.build().toString();

        Request request = new Request.Builder()
                .url(url)
                .build();

        Call call = client.newCall(request);
        String responseString;
        try (Response response = call.execute()) {
            responseString = Objects.requireNonNull(response.body()).string();
        }
        JsonObject s = gson.fromJson(responseString, JsonObject.class);
        return s.get("unixtime").getAsLong();
    }
}
