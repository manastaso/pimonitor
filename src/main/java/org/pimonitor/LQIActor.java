package org.pimonitor;

import akka.actor.AbstractActorWithTimers;
import akka.actor.ActorRef;
import akka.actor.OneForOneStrategy;
import akka.actor.SupervisorStrategy;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.pf.DeciderBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

public class LQIActor extends AbstractActorWithTimers {

    private static ActorRef ui;
    private static final Object TICK_KEY = "TickKey";
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final OkHttpClient client;
    private final Gson gson;

    private Map<String, String> componentMap = new HashMap<>();

    public LQIActor(ActorRef ui) {
        getSelf().tell("tick", getSelf());
        getTimers().startTimerAtFixedRate(TICK_KEY, "tick", Duration.ofHours(3));
        LQIActor.ui = ui;
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
                message -> ui.tell(getLQIData(), getSelf()))
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }

    private LQI getLQIData() throws IOException {

        HttpUrl.Builder urlBuilder
                = Objects.requireNonNull(HttpUrl.parse("https://www.umweltbundesamt.de/api/air_data/v3/airqualityforecast/json?station=844")).newBuilder();

        String url = urlBuilder.build().toString();

        Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = client.newCall(request);
        Response response = call.execute();

        LQI lqi = new LQI();
        String responseString = Objects.requireNonNull(response.body()).string();
        JsonObject s = gson.fromJson(responseString, JsonObject.class);
        lqi.dates = "from " + s.get("dates").getAsJsonObject().get("from").getAsString() + " to " + s.get("dates").getAsJsonObject().get("to").getAsString();
        Map<String, JsonElement> valueMap = s.get("data").getAsJsonObject().get("844").getAsJsonObject().asMap();
        for (Map.Entry<String, JsonElement> e : valueMap.entrySet()) {
            JsonArray componentMeasurement = e.getValue().getAsJsonArray();
            String measureDate = componentMeasurement.get(0).getAsString();
            for (JsonElement componentMeasurementElement : componentMeasurement) {
                if (componentMeasurementElement.isJsonArray()) {
                    JsonArray componentMeasurementElementArray = componentMeasurementElement.getAsJsonArray();
                    String component = getComponentNameFromCode(componentMeasurementElementArray.get(0).getAsString());
                    int measurement = componentMeasurementElementArray.get(2).getAsInt();
                    lqi.measurements.add(new LQI.Measurement(e.getKey(), measureDate + ", " + component, measurement));
                }

            }
        }
        return lqi;
    }

    private String getComponentNameFromCode(String componentId) throws IOException {
        if (componentMap.size() == 0) {
            HttpUrl.Builder urlBuilder
                    = Objects.requireNonNull(HttpUrl.parse("https://www.umweltbundesamt.de/api/air_data/v3/components/json?lang=de&index=id")).newBuilder();

            String url = urlBuilder.build().toString();

            Request request = new Request.Builder()
                    .url(url)
                    .build();
            Call call = client.newCall(request);
            Response response = call.execute();

            String responseString = Objects.requireNonNull(response.body()).string();
            JsonObject s = gson.fromJson(responseString, JsonObject.class);
            s.asMap().forEach((key, value) -> {
                if (value.isJsonArray()) {
                    JsonArray componentElement = value.getAsJsonArray();
                    componentMap.put(key, componentElement.get(1).getAsString() + " - " + componentElement.get(componentElement.size()-1).getAsString());
                }
            });
        }

        return componentMap.get(componentId);

    }
}
