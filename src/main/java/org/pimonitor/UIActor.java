package org.pimonitor;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import eu.hansolo.medusa.Gauge;
import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.chart.ChartData;
import eu.hansolo.tilesfx.chart.TilesFXSeries;
import javafx.application.Platform;
import javafx.scene.chart.XYChart;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import org.joda.time.DateTime;
import org.joda.time.Hours;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private final Tile clockTile;
    private final Tile apparentTemperatureTile;
    private final Tile radarTile;
    private final Tile areaChartTileLQI;
    private final Tile areaChartTilePollen;

    private final Gauge windDirectionGauge;
    private final MemoryMXBean memoryBean;
    private final List<GarbageCollectorMXBean> gcBeans;

    public UIActor(Tile temperatureTile, Tile areaTileTemperature, Tile windDirectionTile, Gauge windDirectionGauge,
                   Tile windSpeedTile,
                   Tile areaTileHumidity, Tile areaTileUV, Tile areaTileRain, Tile areaChartTileprecipitation,
                   Tile areaChartTileWindSpeed, Tile areaChartTileWindGusts, Tile namedayTile,
                   Tile apparentTemperatureTile, Tile radarTile,
                   Tile areaChartTileLQI, Tile areaChartTilePollen, Tile clockTile) {
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
        this.radarTile = radarTile;
        this.areaChartTileLQI = areaChartTileLQI;
        this.areaChartTilePollen = areaChartTilePollen;
        this.clockTile = clockTile;
        memoryBean = ManagementFactory.getMemoryMXBean();
        gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(
                        JsonObject.class,
                        s -> {
                            log.info("Received String message: {}", s);

                            String time = DateTime.now().toString("HH:mm");

                            Double wind = (s.get("current").getAsJsonObject().get("windspeed").getAsDouble() + 5) / 5.0;

                            List<JsonElement> temperatures = s.get("hourly").getAsJsonObject().get("temperature_2m").getAsJsonArray().asList();
                            List<JsonElement> humidities = s.get("hourly").getAsJsonObject().get("relativehumidity_2m").getAsJsonArray().asList();

                            List<JsonElement> pressure_msl = s.get("hourly").getAsJsonObject().get("pressure_msl").getAsJsonArray().asList();
                            s.get("hourly").getAsJsonObject().get("surface_pressure").getAsJsonArray();

                            List<JsonElement> uv = s.get("daily").getAsJsonObject().get("uv_index_max").getAsJsonArray().asList();

                            List<JsonElement> hourly_times = s.get("hourly").getAsJsonObject().get("time").getAsJsonArray().asList();
                            List<JsonElement> daily_times = s.get("daily").getAsJsonObject().get("time").getAsJsonArray().asList();
                            List<JsonElement> rain = s.get("hourly").getAsJsonObject().get("rain").getAsJsonArray().asList();

                            List<JsonElement> windgusts_10m = s.get("hourly").getAsJsonObject().get("windgusts_10m").getAsJsonArray().asList();
                            List<JsonElement> new_windgusts_10m = getBeaufortScale(windgusts_10m);

                            List<JsonElement> windspeed_10m = s.get("hourly").getAsJsonObject().get("windspeed_10m").getAsJsonArray().asList();
                            List<JsonElement> new_windspeed_10m = getBeaufortScale(windspeed_10m);

                            List<JsonElement> precipitation_probability = s.get("hourly").getAsJsonObject().get("precipitation_probability").getAsJsonArray().asList();
                            String temperatureString = s.get("current").getAsJsonObject().get("apparent_temperature").toString();
                            String temperature2mString = s.get("current").getAsJsonObject().get("temperature_2m").toString();
                            Double windDirectionDouble = s.get("current").getAsJsonObject().get("winddirection").getAsDouble();
                            updateWeatherInGUI(temperature2mString,
                                    temperatureString,
                                    windDirectionDouble,
                                    time,
                                    wind,
                                    hourly_times,
                                    temperatures,
                                    humidities,
                                    daily_times,
                                    uv, rain, pressure_msl, precipitation_probability,
                                    new_windgusts_10m, new_windspeed_10m);
                        })
                .match(String.class,
                        s -> Platform.runLater(() -> namedayTile.setDescription(s)))
                .match(Pollen.class,
                        s -> Platform.runLater(() -> processPollen(s)))
                .match(LQI.class,
                        s -> Platform.runLater(() -> processLQI(s)))
                .match(Long.class,
                        s -> Platform.runLater(() -> clockTile.setTime(s))
                )
                .match(ArrayList.class,
                        s -> Platform.runLater(() -> {
                            String text = "- " + s.get(0).toString() +
                                    System.lineSeparator() +
                                    "- " + s.get(1).toString() +
                                    System.lineSeparator() +
                                    "- " + s.get(2).toString();
                            clockTile.setInfoRegionTooltipText(text);
                            radarTile.setImage(new Image("https://www.dwd.de/DWD/wetter/radar/radfilm_brd_akt.gif"));
                        })
                )
                .matchAny(o -> log.info("received unknown message"))
                .build();
    }

    private void updateWeatherInGUI(String temperature2mString,
                                    String temperatureString,
                                    Double windDirectionDouble,
                                    String time,
                                    Double wind,
                                    List<JsonElement> hourly_times,
                                    List<JsonElement> temperatures,
                                    List<JsonElement> humidities,
                                    List<JsonElement> daily_times,
                                    List<JsonElement> uv,
                                    List<JsonElement> rain,
                                    List<JsonElement> pressure_msl,
                                    List<JsonElement> precipitation_probability,
                                    List<JsonElement> new_windgusts_10m,
                                    List<JsonElement> new_windspeed_10m) {
        Platform.runLater(() -> {
            clockTile.setTitle(getCGStatistics());
            temperatureTile.setDescription(temperature2mString);
            temperatureTile.setText(time);
            apparentTemperatureTile.setDescription(temperatureString);
            apparentTemperatureTile.setText(time);
            windDirectionTile.setText(time);
            windDirectionGauge.setValue(360 - windDirectionDouble);
            windDirectionTile.setText(time);
            windSpeedTile.setDescription(Long.valueOf(wind.longValue()).toString());
            windSpeedTile.setText(time);
        });
        plotChartHourly(hourly_times, temperatures, areaTileTemperature, "Temperature", time);
        plotChartHourly(hourly_times, humidities, areaTileHumidity, "Humidity", time);
        plotChartDaily(daily_times, uv, areaTileUV, time);
        plotChartHourly(hourly_times, rain, areaTileRain, "Rain", time);
        plotChartHourly(hourly_times, pressure_msl.stream().map(x -> {
            double y = x.getAsDouble() / 33.863886666667;
            return JsonParser.parseString(Double.toString(y));
        }).collect(Collectors.toList()), areaChartTileLQI, "Sealevel Pressure", time);
        plotChartHourly(hourly_times, precipitation_probability, areaChartTileprecipitation, "Precipitation Probability", time);
        plotChartHourly(hourly_times, new_windgusts_10m, areaChartTileWindGusts, "Wind Gusts", time);
        plotChartHourly(hourly_times, new_windspeed_10m, areaChartTileWindSpeed, "Wind Speed", time);
    }

    private String getCGStatistics() {
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        long used = heap.getUsed();
        long max = heap.getMax();

        double usedMB = used / (1024.0 * 1024.0);
        double maxMB = max / (1024.0 * 1024.0);
        double usagePercent = (usedMB / maxMB) * 100.0;

        return String.format(
                "%.1f / %.1f MB (%.1f%%)",
                usedMB, maxMB, usagePercent
        );
    }

    private void processLQI(LQI s) {
        areaChartTileLQI.setMatrixSize(s.measurements.size(), 10);
        List<ChartData> chartData = new ArrayList<>(s.measurements.size());
        s.measurements.forEach(measurement -> chartData.add(new ChartData(measurement.component, measurement.measurement, getLQIColor(measurement.measurement))));
        areaChartTileLQI.setChartData(chartData);
        areaChartTileLQI.setText(s.dates);
    }

    private Color getLQIColor(int value) {
        return switch (value) {
            case 0 -> Tile.LIGHT_GREEN;
            case 1 -> Tile.GREEN;
            case 2 -> Tile.YELLOW;
            case 3 -> Tile.YELLOW_ORANGE;
            case 4 -> Tile.LIGHT_RED;
            case 5 -> Tile.RED;
            case 6 -> Tile.PINK;
            default -> Tile.GRAY;
        };
    }

    private void processPollen(Pollen s) {
        List<ChartData> chartData = new ArrayList<>(10);
        chartData.add(new ChartData("Erle(today)", getPollenValue(s.Erle.today), getPollenColor(getPollenValue(s.Erle.today))));
        chartData.add(new ChartData("Erle(tomorrow)", getPollenValue(s.Erle.tomorrow), getPollenColor(getPollenValue(s.Erle.tomorrow))));
        chartData.add(new ChartData("Erle(dayafter_to)", getPollenValue(s.Erle.dayafter_to), getPollenColor(getPollenValue(s.Erle.dayafter_to))));
        chartData.add(new ChartData("Graeser(today)", getPollenValue(s.Graeser.today), getPollenColor(getPollenValue(s.Graeser.today))));
        chartData.add(new ChartData("Graeser(tomorrow)", getPollenValue(s.Graeser.tomorrow), getPollenColor(getPollenValue(s.Graeser.tomorrow))));
        chartData.add(new ChartData("Graeser(dayafter_to)", getPollenValue(s.Graeser.dayafter_to), getPollenColor(getPollenValue(s.Graeser.dayafter_to))));
        chartData.add(new ChartData("Roggen(today)", getPollenValue(s.Roggen.today), getPollenColor(getPollenValue(s.Roggen.today))));
        chartData.add(new ChartData("Roggen(tomorrow)", getPollenValue(s.Roggen.tomorrow), getPollenColor(getPollenValue(s.Roggen.tomorrow))));
        chartData.add(new ChartData("Roggen(dayafter_to)", getPollenValue(s.Roggen.dayafter_to), getPollenColor(getPollenValue(s.Roggen.dayafter_to))));
        chartData.add(new ChartData("Beifuss(today)", getPollenValue(s.Beifuss.today), getPollenColor(getPollenValue(s.Beifuss.today))));
        chartData.add(new ChartData("Beifuss(tomorrow)", getPollenValue(s.Beifuss.tomorrow), getPollenColor(getPollenValue(s.Beifuss.tomorrow))));
        chartData.add(new ChartData("Beifuss(dayafter_to)", getPollenValue(s.Beifuss.dayafter_to), getPollenColor(getPollenValue(s.Beifuss.dayafter_to))));
        chartData.add(new ChartData("Esche(today)", getPollenValue(s.Esche.today), getPollenColor(getPollenValue(s.Esche.today))));
        chartData.add(new ChartData("Esche(tomorrow)", getPollenValue(s.Esche.tomorrow), getPollenColor(getPollenValue(s.Esche.tomorrow))));
        chartData.add(new ChartData("Esche(dayafter_to)", getPollenValue(s.Esche.dayafter_to), getPollenColor(getPollenValue(s.Esche.dayafter_to))));
        chartData.add(new ChartData("Birke(today)", getPollenValue(s.Birke.today), getPollenColor(getPollenValue(s.Birke.today))));
        chartData.add(new ChartData("Birke(tomorrow)", getPollenValue(s.Birke.tomorrow), getPollenColor(getPollenValue(s.Birke.tomorrow))));
        chartData.add(new ChartData("Birke(dayafter_to)", getPollenValue(s.Birke.dayafter_to), getPollenColor(getPollenValue(s.Birke.dayafter_to))));
        chartData.add(new ChartData("Ambrosia(today)", getPollenValue(s.Ambrosia.today), getPollenColor(getPollenValue(s.Ambrosia.today))));
        chartData.add(new ChartData("Ambrosia(tomorrow)", getPollenValue(s.Ambrosia.tomorrow), getPollenColor(getPollenValue(s.Ambrosia.tomorrow))));
        chartData.add(new ChartData("Ambrosia(dayafter_to)", getPollenValue(s.Ambrosia.dayafter_to), getPollenColor(getPollenValue(s.Ambrosia.dayafter_to))));
        chartData.add(new ChartData("Hasel(today)", getPollenValue(s.Hasel.today), getPollenColor(getPollenValue(s.Hasel.today))));
        chartData.add(new ChartData("Hasel(tomorrow)", getPollenValue(s.Hasel.tomorrow), getPollenColor(getPollenValue(s.Hasel.tomorrow))));
        chartData.add(new ChartData("Hasel(dayafter_to)", getPollenValue(s.Hasel.dayafter_to), getPollenColor(getPollenValue(s.Hasel.dayafter_to))));
        areaChartTilePollen.setChartData(chartData);
        areaChartTilePollen.setText(s.last_update);
    }

    private Color getPollenColor(double value) {
        if (value == 0.0)
            return Tile.LIGHT_GREEN;
        if (value > 0.0 && value < 1.0)
            return Tile.GREEN;
        if (value >= 1.0 && value < 2.0)
            return Tile.YELLOW;
        if (value >= 2.0 && value < 3.0)
            return Tile.LIGHT_RED;
        if (value >= 3.0)
            return Tile.RED;
        return Tile.GRAY;
    }


    private double getPollenValue(String s) {
        double d = 0.0;
        try {
            d = Double.parseDouble(s);
        } catch (NumberFormatException e) {
            StringTokenizer tokenizer = new StringTokenizer(s, "-");
            int counter = 0;
            do {
                String s1 = tokenizer.nextToken();
                d += Double.parseDouble(s1);
                counter += 1;
            } while (tokenizer.hasMoreTokens());
            d = d / counter;
        }
        return d;
    }

    private List<JsonElement> getBeaufortScale(List<JsonElement> list) {
        Stream<JsonElement> stream = list.stream().map(x -> {
            double y = (x.getAsDouble() + 5) / 5;
            long w = (long) y;
            return JsonParser.parseString(Long.toString(w));
        });
        return stream.collect(Collectors.toList());
    }

    private XYChart.Series<String, Number> getEmptySeriesFromTile(Tile tile, String title) {
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName(title);

        if (!tile.getSeries().isEmpty()) {
            series = tile.getSeries().get(0);
            series.setName(title);
            do {
                series.getData().remove(0);
            } while (!series.getData().isEmpty());
        }
        return series;
    }

    private void plotChartHourly(List<JsonElement> keys, List<JsonElement> values, Tile tile, String title, String text) {

        XYChart.Series<String, Number> series = getEmptySeriesFromTile(tile, title);

        for (int i = 0; i < values.size(); i++) {

            DateTime time;

            time = new DateTime(keys.get(i).getAsLong() * 1000);

            int keyFromHourlyDate = getKeyFromHourlyDate(time);
            if (keyFromHourlyDate > 0 && keyFromHourlyDate < 24) {
                String keyFromHourlyDateString = Integer.toString(keyFromHourlyDate);
                series.getData().add(new XYChart.Data<>(keyFromHourlyDateString,
                        values.get(i).getAsDouble()));
            }
        }

        Platform.runLater(() -> {
            tile.setText(text);

            if (tile.getTilesFXSeries().isEmpty())
                tile.setTilesFXSeries(new TilesFXSeries<>(series,
                        Tile.BLUE,
                        new LinearGradient(0, 0, 0, 1,
                                true, CycleMethod.NO_CYCLE,
                                new Stop(0, Tile.BLUE),
                                new Stop(1, Color.TRANSPARENT))));
        });
    }

    private void plotChartDaily(List<JsonElement> keys, List<JsonElement> values, Tile tile, String text) {

        XYChart.Series<String, Number> series = getEmptySeriesFromTile(tile, "UV");

        for (int i = 0; i < values.size(); i++) {

            String keyToDisplay = Integer.valueOf(i).toString();
            DateTime time;

            time = new DateTime(keys.get(i).getAsLong() * 1000);

            series.getData().add(new XYChart.Data<>(!getKeyFromDailyDate(time).isEmpty() ? getKeyFromDailyDate(time) : keyToDisplay, values.get(i).getAsDouble()));
        }
        Platform.runLater(() -> {
            tile.setText(text);

            if (tile.getTilesFXSeries().isEmpty())
                tile.setTilesFXSeries(new TilesFXSeries<>(series,
                        Tile.BLUE,
                        new LinearGradient(0, 0, 0, 1,
                                true, CycleMethod.NO_CYCLE,
                                new Stop(0, Tile.BLUE),
                                new Stop(1, Color.TRANSPARENT))));
        });
    }

    private static Integer getKeyFromHourlyDate(DateTime time) {
        return Hours.hoursBetween(DateTime.now(), time).getHours();
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
