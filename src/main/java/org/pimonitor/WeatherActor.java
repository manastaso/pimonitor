package org.pimonitor;

import akka.actor.AbstractActorWithTimers;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.IOException;
import java.time.Duration;

public class WeatherActor extends AbstractActorWithTimers {

    private static ActorRef ui;
    private static Object TICK_KEY = "TickKey";
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final OkHttpClient client;
    private final Gson gson;
    private JsonObject jsonObject;

    public WeatherActor(ActorRef ui) {
        getJSONObject();
        
        getTimers().startTimerAtFixedRate(TICK_KEY, "tick", Duration.ofMillis(5000));
        WeatherActor.ui = ui;
        client = new OkHttpClient();
        gson = new Gson();
    }

    private void getJSONObject() {
        String json = "{\"latitude\":53.56,\"longitude\":10.0,\"generationtime_ms\":0.12290477752685547,\"utc_offset_seconds\":7200,\"timezone\":\"Europe/Berlin\",\"timezone_abbreviation\":\"CEST\",\"elevation\":11.0,\"hourly_units\":{\"time\":\"unixtime\",\"temperature_2m\":\"°C\",\"rain\":\"mm\"},\"hourly\":{\"time\":[1696888800,1696892400,1696896000,1696899600,1696903200,1696906800,1696910400,1696914000,1696917600,1696921200,1696924800,1696928400,1696932000,1696935600,1696939200,1696942800,1696946400,1696950000,1696953600,1696957200,1696960800,1696964400,1696968000,1696971600,1696975200,1696978800,1696982400,1696986000,1696989600,1696993200,1696996800,1697000400,1697004000,1697007600,1697011200,1697014800,1697018400,1697022000,1697025600,1697029200,1697032800,1697036400,1697040000,1697043600,1697047200,1697050800,1697054400,1697058000,1697061600,1697065200,1697068800,1697072400,1697076000,1697079600,1697083200,1697086800,1697090400,1697094000,1697097600,1697101200,1697104800,1697108400,1697112000,1697115600,1697119200,1697122800,1697126400,1697130000,1697133600,1697137200,1697140800,1697144400,1697148000,1697151600,1697155200,1697158800,1697162400,1697166000,1697169600,1697173200,1697176800,1697180400,1697184000,1697187600,1697191200,1697194800,1697198400,1697202000,1697205600,1697209200,1697212800,1697216400,1697220000,1697223600,1697227200,1697230800,1697234400,1697238000,1697241600,1697245200,1697248800,1697252400,1697256000,1697259600,1697263200,1697266800,1697270400,1697274000,1697277600,1697281200,1697284800,1697288400,1697292000,1697295600,1697299200,1697302800,1697306400,1697310000,1697313600,1697317200,1697320800,1697324400,1697328000,1697331600,1697335200,1697338800,1697342400,1697346000,1697349600,1697353200,1697356800,1697360400,1697364000,1697367600,1697371200,1697374800,1697378400,1697382000,1697385600,1697389200,1697392800,1697396400,1697400000,1697403600,1697407200,1697410800,1697414400,1697418000,1697421600,1697425200,1697428800,1697432400,1697436000,1697439600,1697443200,1697446800,1697450400,1697454000,1697457600,1697461200,1697464800,1697468400,1697472000,1697475600,1697479200,1697482800,1697486400,1697490000],\"temperature_2m\":[14.3,14.2,14.3,13.6,12.7,12.4,12.2,12.3,12.4,13.3,13.9,14.6,15.6,16.9,17.8,17.8,17.3,17.6,17.2,17.0,16.9,17.0,16.9,16.8,16.2,15.9,15.8,15.3,15.0,14.6,14.3,14.2,14.0,14.3,15.1,15.9,16.6,17.5,18.0,18.3,18.7,18.6,17.3,15.7,15.4,15.1,15.0,14.4,13.9,13.6,13.1,12.7,12.4,12.0,11.8,11.4,11.1,11.2,11.8,12.9,14.0,15.0,15.7,14.9,15.1,14.3,13.6,12.7,11.9,11.3,10.9,10.5,10.1,9.6,9.1,8.7,8.4,8.2,8.6,9.1,9.5,10.2,11.3,12.6,13.9,14.7,15.6,16.6,17.1,17.3,17.6,17.8,17.7,17.5,17.3,16.8,16.1,15.3,14.6,14.1,13.8,13.5,13.0,12.4,12.1,12.2,12.5,12.6,12.4,12.0,11.6,11.6,11.6,11.5,11.0,10.5,10.0,9.9,9.9,9.9,9.8,9.8,9.6,9.4,9.1,8.8,8.4,8.0,7.8,7.9,8.3,8.5,8.7,8.8,8.8,8.8,8.5,8.2,7.8,7.4,7.1,6.9,6.9,6.8,6.8,6.9,6.9,6.7,6.5,6.4,6.2,6.2,6.2,6.3,6.6,7.0,7.7,8.6,9.3,9.6,9.8,9.7,9.3,8.6,8.0,7.6,7.1,6.7],\"rain\":[0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.50,0.10,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.90,3.80,1.30,0.80,1.50,1.10,0.90,1.30,1.50,1.50,0.90,0.50,0.10,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.10,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.10,1.00,1.50,3.10,0.70,0.50,0.80,0.10,0.00,0.00,0.20,0.20,0.20,2.50,2.50,2.50,0.40,0.40,0.40,0.10,0.10,0.10,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.00,0.10,0.10,0.10,0.20,0.20,0.20,0.10,0.10,0.10,0.10,0.10,0.10,0.00,0.00,0.00,0.00,0.00,0.00]},\"daily_units\":{\"time\":\"unixtime\",\"uv_index_max\":\"\"},\"daily\":{\"time\":[1696888800,1696975200,1697061600,1697148000,1697234400,1697320800,1697407200],\"uv_index_max\":[2.55,2.65,1.80,0.50,1.50,1.80,2.35]}}";
        jsonObject = new JsonParser().parse(json).getAsJsonObject();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(
                String.class,
                message -> {
                    getWeatherData();
                    ui.tell(getWeatherData(), getSelf());
                })
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }

    private JsonObject getWeatherData() throws IOException {

        HttpUrl.Builder urlBuilder
                = HttpUrl.parse("https://api.open-meteo.com" + "/v1/forecast").newBuilder();
        urlBuilder.addQueryParameter("latitude", "53.551086");
        urlBuilder.addQueryParameter("longitude", "9.993682");
        urlBuilder.addQueryParameter("current_weather", "true");
        urlBuilder.addQueryParameter("hourly", "temperature_2m");
        urlBuilder.addQueryParameter("hourly", "relativehumidity_2m");
        urlBuilder.addQueryParameter("hourly", "rain");
        urlBuilder.addQueryParameter("daily", "uv_index_max");
        urlBuilder.addQueryParameter("timezone", "Europe/Berlin");
        urlBuilder.addQueryParameter("timeformat", "unixtime");

        String url = urlBuilder.build().toString();

        Request request = new Request.Builder()
                .url(url)
                .build();
        Call call = client.newCall(request);
        Response response = call.execute();

        String responseString = response.body().string();
        return gson.fromJson(responseString, JsonObject.class);
    }
}
