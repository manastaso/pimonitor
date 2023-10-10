package org.pimonitor;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.chart.TilesFXSeries;
import javafx.application.Platform;
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class UIActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final Tile temperatureTile;
    private final Tile windDirectionTile;
    private final Tile windSpeedTile;
    private final Tile areaTileTemperature;
    private final Tile areaTileHumidity;
    private final Tile areaTileUV;
    private final Tile areaTileRain;


    public UIActor(Tile temperatureTile, Tile windDirectionTile, Tile windSpeedTile, Tile areaTileTemperature,
                   Tile areaTileHumidity, Tile areaTileUV, Tile areaTileRain) {
        this.temperatureTile = temperatureTile;
        this.windDirectionTile = windDirectionTile;
        this.windSpeedTile = windSpeedTile;
        this.areaTileTemperature = areaTileTemperature;
        this.areaTileHumidity = areaTileHumidity;
        this.areaTileUV = areaTileUV;
        this.areaTileRain = areaTileRain;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(
                        JsonObject.class,
                        s -> {
                            log.info("Received String message: {}", s);
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    temperatureTile.setDescription(s.get("current_weather").getAsJsonObject().get("temperature").toString());
                                    String time = s.get("generationtime_ms").getAsString();
                                    temperatureTile.setText(time);
                                    windDirectionTile.setDescription(s.get("current_weather").getAsJsonObject().get("winddirection").toString());
                                    windDirectionTile.setText(time);
                                    windSpeedTile.setDescription(s.get("current_weather").getAsJsonObject().get("windspeed").toString());
                                    windSpeedTile.setText(time);

                                    List<JsonElement> temperatures = s.get("hourly").getAsJsonObject().get("temperature_2m").getAsJsonArray().asList();
                                    List<JsonElement> humidities = s.get("hourly").getAsJsonObject().get("relativehumidity_2m").getAsJsonArray().asList();

                                    List<JsonElement> uv = s.get("daily").getAsJsonObject().get("uv_index_max").getAsJsonArray().asList();

                                    List<JsonElement> hourly_times = s.get("hourly").getAsJsonObject().get("time").getAsJsonArray().asList();
                                    List<JsonElement> daily_times = s.get("daily").getAsJsonObject().get("time").getAsJsonArray().asList();
                                    List<JsonElement> rain = s.get("hourly").getAsJsonObject().get("rain").getAsJsonArray().asList();

                                    plotChartHourly(hourly_times, temperatures, areaTileTemperature,"Temperature", time);
                                    plotChartHourly(hourly_times, humidities, areaTileHumidity, "Humidity", time);
                                    plotChartDaily(daily_times, uv, areaTileUV, "UV", time);
                                    plotChartHourly(hourly_times, rain, areaTileRain, "Rain", time);

                                }
                            });
                        })
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }

    private void plotChartHourly(List<JsonElement> keys, List<JsonElement> values, Tile tile, String title, String text) {

        XYChart.Series<String, Number> series = new XYChart.Series();
        series.setName(title);

        for (int i = 0; i < values.size(); i++) {

            String keyToDisplay = Integer.valueOf(i).toString();
            DateTime time;

            time = new DateTime(keys.get(i).getAsLong()*1000);

            series.getData().add(new XYChart.Data(getKeyFromHourlyDate(time) != "" ? getKeyFromHourlyDate(time) : keyToDisplay, values.get(i).getAsDouble()));
        }

        tile.setText(text);

        tile.setTilesFXSeries(new TilesFXSeries<>(series,
                Tile.BLUE,
                new LinearGradient(0, 0, 0, 1,
                        true, CycleMethod.NO_CYCLE,
                        new Stop(0, Tile.BLUE),
                        new Stop(1, Color.TRANSPARENT))));
    }

    private void plotChartDaily(List<JsonElement> keys, List<JsonElement> values, Tile tile, String title, String text) {

        XYChart.Series<String, Number> series = new XYChart.Series();
        series.setName(title);

        for (int i = 0; i < values.size(); i++) {

            String keyToDisplay = Integer.valueOf(i).toString();
            DateTime time;

            time = new DateTime(keys.get(i).getAsLong()*1000);

            series.getData().add(new XYChart.Data(getKeyFromDailyDate(time) != "" ? getKeyFromDailyDate(time) : keyToDisplay, values.get(i).getAsDouble()));
        }

        tile.setText(text);

        tile.setTilesFXSeries(new TilesFXSeries<>(series,
                Tile.BLUE,
                new LinearGradient(0, 0, 0, 1,
                        true, CycleMethod.NO_CYCLE,
                        new Stop(0, Tile.BLUE),
                        new Stop(1, Color.TRANSPARENT))));
    }

    private static String getKeyFromHourlyDate(DateTime time) {
        Hours hours = Hours.hoursBetween(DateTime.now(), time);
        return Integer.toString(hours.getHours());
    }

    private static String getKeyFromDailyDate(DateTime time) {
        String keyToDisplay = "";
        int month = time.getMonthOfYear();
        int day = time.getDayOfMonth();

        DateTime now = DateTime.now();
        int monthNow = now.getMonthOfYear();
        int dayNow = now.getDayOfMonth();

        if (month == monthNow && day == dayNow) {
            keyToDisplay = "TODAY";
        }
        return keyToDisplay;
    }
}
