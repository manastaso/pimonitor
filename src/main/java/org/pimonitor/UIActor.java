package org.pimonitor;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.hansolo.medusa.Gauge;
import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.chart.TilesFXSeries;
import javafx.application.Platform;
import javafx.scene.chart.XYChart;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import org.joda.time.DateTime;
import org.joda.time.Hours;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Map.entry;

public class UIActor extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private final Tile temperatureTile;
    private final Tile windDirectionTile;
    private final Tile windSpeedTile;
    private final Tile areaTileTemperature;
    private final Tile areaTileHumidity;
    private final Tile areaTileUV;
    private final Tile areaTileRain;
    private final Tile areaChartTileprecipitation;
    private final Tile areaChartTileWindGusts;
    private final Tile areaChartTileWindSpeed;
    private final Tile namedayTile;
    private final Tile apparentTemperatureTile;
    private final Tile weatherCodeTile;
    private final Tile areaChartTileSealevelPressure;
    private final Tile areaChartTileSurfacePressure;

    private static final Map<String, String> WEATHER_CODES = Map.ofEntries(
            entry("0", "Clear sky"),
            entry("1", "Mainly clear, partly cloudy, and overcast"),
            entry("2", "Mainly clear, partly cloudy, and overcast"),
            entry("3", "Mainly clear, partly cloudy, and overcast"),
            entry("45", "Fog and depositing rime fog"),
            entry("48", "Fog and depositing rime fog"),
            entry("51", "Drizzle: Light, moderate, and dense intensity"),
            entry("53", "Drizzle: Light, moderate, and dense intensity"),
            entry("55", "Drizzle: Light, moderate, and dense intensity"),
            entry("56", "Freezing Drizzle: Light and dense intensity"),
            entry("57", "Freezing Drizzle: Light and dense intensity"),
            entry("61", "Rain: Slight, moderate and heavy intensity"),
            entry("63", "Rain: Slight, moderate and heavy intensity"),
            entry("65", "Rain: Slight, moderate and heavy intensity"),
            entry("66", "Freezing Rain: Light and heavy intensity"),
            entry("67", "Freezing Rain: Light and heavy intensity"),
            entry("71", "Snow fall: Slight, moderate, and heavy intensity"),
            entry("73", "Snow fall: Slight, moderate, and heavy intensity"),
            entry("75", "Snow fall: Slight, moderate, and heavy intensity"),
            entry("77", "Snow grains"),
            entry("80", "Rain showers: Slight, moderate, and violent"),
            entry("81", "Rain showers: Slight, moderate, and violent"),
            entry("82", "Rain showers: Slight, moderate, and violent"),
            entry("85", "Snow showers slight and heavy"),
            entry("86", "Snow showers slight and heavy"),
            entry("95", "Thunderstorm: Slight or moderate"),
            entry("96", "Thunderstorm with slight and heavy hail"),
            entry("99", "Thunderstorm with slight and heavy hail")
    );
    private static final JsonParser parser = new JsonParser();
    private final Gauge windDirectionGauge;

    public UIActor(Tile temperatureTile, Tile areaTileTemperature, Tile windDirectionTile, Gauge windDirectionGauge,
                   Tile windSpeedTile,
                   Tile areaTileHumidity, Tile areaTileUV, Tile areaTileRain, Tile areaChartTileprecipitation,
                   Tile areaChartTileWindSpeed, Tile areaChartTileWindGusts, Tile namedayTile,
                   Tile apparentTemperatureTile, Tile weatherCodeTile,
                   Tile areaChartTileSealevelPressure, Tile areaChartTileSurfacePressure) {
        this.temperatureTile = temperatureTile;
        this.windDirectionTile = windDirectionTile;
        this.windDirectionGauge = windDirectionGauge;
        this.windSpeedTile = windSpeedTile;
        this.areaTileTemperature = areaTileTemperature;
        this.areaTileHumidity = areaTileHumidity;
        this.areaTileUV = areaTileUV;
        this.areaTileRain = areaTileRain;
        this.areaChartTileprecipitation = areaChartTileprecipitation;
        this.areaChartTileWindGusts = areaChartTileWindGusts;
        this.areaChartTileWindSpeed = areaChartTileWindSpeed;
        this.namedayTile = namedayTile;
        this.apparentTemperatureTile = apparentTemperatureTile;
        this.weatherCodeTile = weatherCodeTile;
        this.areaChartTileSealevelPressure = areaChartTileSealevelPressure;
        this.areaChartTileSurfacePressure = areaChartTileSurfacePressure;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(
                        JsonObject.class,
                        s -> {
                            log.info("Received String message: {}", s);
                            Platform.runLater(() -> {
                                temperatureTile.setDescription(s.get("current").getAsJsonObject().get("temperature_2m").toString());
                                String time = s.get("generationtime_ms").getAsString();
                                temperatureTile.setText(time);

                                apparentTemperatureTile.setDescription(s.get("current").getAsJsonObject().get("apparent_temperature").toString());
                                apparentTemperatureTile.setText(time);

                                windDirectionTile.setText(time);
                                windDirectionGauge.setValue(360-s.get("current").getAsJsonObject().get("winddirection").getAsDouble());

                                weatherCodeTile.setDescription(WEATHER_CODES.getOrDefault(s.get("current").getAsJsonObject().get("weathercode").toString(), "Unknown weather code"));
                                windDirectionTile.setText(time);

                                Double wind = (s.get("current").getAsJsonObject().get("windspeed").getAsDouble() + 5) / 5.0;
                                windSpeedTile.setDescription(Long.valueOf(wind.longValue()).toString());
                                windSpeedTile.setText(time);

                                List<JsonElement> temperatures = s.get("hourly").getAsJsonObject().get("temperature_2m").getAsJsonArray().asList();
                                List<JsonElement> humidities = s.get("hourly").getAsJsonObject().get("relativehumidity_2m").getAsJsonArray().asList();

                                List<JsonElement> pressure_msl = s.get("hourly").getAsJsonObject().get("pressure_msl").getAsJsonArray().asList();
                                List<JsonElement> surface_pressure = s.get("hourly").getAsJsonObject().get("surface_pressure").getAsJsonArray().asList();

                                List<JsonElement> uv = s.get("daily").getAsJsonObject().get("uv_index_max").getAsJsonArray().asList();

                                List<JsonElement> hourly_times = s.get("hourly").getAsJsonObject().get("time").getAsJsonArray().asList();
                                List<JsonElement> daily_times = s.get("daily").getAsJsonObject().get("time").getAsJsonArray().asList();
                                List<JsonElement> rain = s.get("hourly").getAsJsonObject().get("rain").getAsJsonArray().asList();

                                List<JsonElement> windgusts_10m = s.get("hourly").getAsJsonObject().get("windgusts_10m").getAsJsonArray().asList();
                                List<JsonElement> new_windgusts_10m = getBeaufortScale(windgusts_10m);

                                List<JsonElement> windspeed_10m = s.get("hourly").getAsJsonObject().get("windspeed_10m").getAsJsonArray().asList();
                                List<JsonElement> new_windspeed_10m = getBeaufortScale(windspeed_10m);

                                List<JsonElement> precipitation_probability = s.get("hourly").getAsJsonObject().get("precipitation_probability").getAsJsonArray().asList();

                                plotChartHourly(hourly_times, temperatures, areaTileTemperature, "Temperature", time);
                                plotChartHourly(hourly_times, humidities, areaTileHumidity, "Humidity", time);
                                plotChartDaily(daily_times, uv, areaTileUV, "UV", time);
                                plotChartHourly(hourly_times, rain, areaTileRain, "Rain", time);
                                plotChartHourly(hourly_times, pressure_msl.stream().map(x -> {
                                    Double y = x.getAsDouble() / 33.863886666667;
                                    return parser.parseString(y.toString());
                                }).collect(Collectors.toList()), areaChartTileSealevelPressure, "Sealevel Pressure", time);
                                plotChartHourly(hourly_times, surface_pressure.stream().map(x -> {
                                    Double y = x.getAsDouble() / 33.863886666667;
                                    return parser.parseString(y.toString());
                                }).collect(Collectors.toList()), areaChartTileSurfacePressure, "Surface Pressure", time);
                                plotChartHourly(hourly_times, precipitation_probability, areaChartTileprecipitation, "Precipitation Probability", time);
                                plotChartHourly(hourly_times, new_windgusts_10m, areaChartTileWindGusts, "Wind Gusts", time);
                                plotChartHourly(hourly_times, new_windspeed_10m, areaChartTileWindSpeed, "Wind Speed", time);

                            });
                        })
                .match(String.class,
                        s -> Platform.runLater(() -> namedayTile.setDescription(s)))
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }

    private List<JsonElement> getBeaufortScale(List<JsonElement> list) {
        Stream<JsonElement> stream = list.stream().map(x -> {
            Double y = (x.getAsDouble() + 5) / 5;
            Long w = y.longValue();
            return JsonParser.parseString(w.toString());
        });
        return stream.collect(Collectors.toList());
    }

    private XYChart.Series<String, Number> getEmptySeriesFromTile(Tile tile, String title) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(title);

        if (tile.getSeries().size() > 0) {
            series = tile.getSeries().get(0);
            series.setName(title);
            do {
                series.getData().remove(0);
            } while (series.getData().size() > 0);
        }
        return series;
    }

    private void plotChartHourly(List<JsonElement> keys, List<JsonElement> values, Tile tile, String title, String text) {

        XYChart.Series<String, Number> series = getEmptySeriesFromTile(tile, title);

        for (int i = 0; i < values.size(); i++) {

            String keyToDisplay = Integer.valueOf(i).toString();
            DateTime time;

            time = new DateTime(keys.get(i).getAsLong() * 1000);

            Integer keyFromHourlyDate = getKeyFromHourlyDate(time);
            if (keyFromHourlyDate > 0 && keyFromHourlyDate < 24) {
                String keyFromHourlyDateString = keyFromHourlyDate.toString();
                series.getData().add(new XYChart.Data<>(!keyFromHourlyDateString.equals("") ? keyFromHourlyDateString : keyToDisplay, values.get(i).getAsDouble()));
            }
        }

        tile.setText(text);

        if (tile.getTilesFXSeries().size() == 0)
            tile.setTilesFXSeries(new TilesFXSeries<>(series,
                    Tile.BLUE,
                    new LinearGradient(0, 0, 0, 1,
                            true, CycleMethod.NO_CYCLE,
                            new Stop(0, Tile.BLUE),
                            new Stop(1, Color.TRANSPARENT))));
    }

    private void plotChartDaily(List<JsonElement> keys, List<JsonElement> values, Tile tile, String title, String text) {

        XYChart.Series<String, Number> series = getEmptySeriesFromTile(tile, title);

        for (int i = 0; i < values.size(); i++) {

            String keyToDisplay = Integer.valueOf(i).toString();
            DateTime time;

            time = new DateTime(keys.get(i).getAsLong() * 1000);

            series.getData().add(new XYChart.Data<>(!getKeyFromDailyDate(time).equals("") ? getKeyFromDailyDate(time) : keyToDisplay, values.get(i).getAsDouble()));
        }

        tile.setText(text);

        if (tile.getTilesFXSeries().size() == 0)
            tile.setTilesFXSeries(new TilesFXSeries<>(series,
                    Tile.BLUE,
                    new LinearGradient(0, 0, 0, 1,
                            true, CycleMethod.NO_CYCLE,
                            new Stop(0, Tile.BLUE),
                            new Stop(1, Color.TRANSPARENT))));
    }

    private static Integer getKeyFromHourlyDate(DateTime time) {
        return Integer.valueOf(Hours.hoursBetween(DateTime.now(), time).getHours());
    }

    private static String getKeyFromDailyDate(DateTime time) {
        String keyToDisplay = "";
        int month = time.getMonthOfYear();
        int day = time.getDayOfMonth();

        DateTime now = DateTime.now();
        int monthNow = now.getMonthOfYear();
        int dayNow = now.getDayOfMonth();

        if (month == monthNow && day == dayNow) {
            keyToDisplay = "T";
        }
        return keyToDisplay;
    }
}
