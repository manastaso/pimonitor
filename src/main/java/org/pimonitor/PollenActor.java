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

public class PollenActor extends AbstractActorWithTimers {

    private static ActorRef ui;
    private static final Object TICK_KEY = "TickKey";
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final OkHttpClient client;
    private final Gson gson;

    public PollenActor(ActorRef ui) {
        getSelf().tell("tick", getSelf());
        getTimers().startTimerAtFixedRate(TICK_KEY, "tick", Duration.ofHours(3));
        PollenActor.ui = ui;
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
                message -> ui.tell(getPollenData(), getSelf()))
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }

    private Pollen getPollenData() throws IOException {

        HttpUrl.Builder urlBuilder
                = Objects.requireNonNull(HttpUrl.parse("https://opendata.dwd.de/climate_environment/health/alerts/s31fg.json")).newBuilder();

        String url = urlBuilder.build().toString();

        Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = client.newCall(request);
        Response response = call.execute();

        String responseString = Objects.requireNonNull(response.body()).string();
        JsonObject s = gson.fromJson(responseString, JsonObject.class);
        String last_update = s.get("last_update").getAsString();
        List<JsonElement> content = s.get("content").getAsJsonArray().asList();
        ListIterator<JsonElement> contentIterator = content.listIterator();
        JsonObject element;
        Pollen pollen;
        do {
            element = contentIterator.next().getAsJsonObject();
            pollen = gson.fromJson(element.getAsJsonObject().get("Pollen"), Pollen.class);
        } while (element != null && element.get("partregion_id").getAsInt() != 12 && contentIterator.hasNext());

        pollen.last_update = last_update;

        return pollen;
    }
}
