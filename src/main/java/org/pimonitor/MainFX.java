package org.pimonitor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.tools.FlowGridPane;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.Locale;

public class MainFX extends Application{

    private static final double TILE_WIDTH = 150;
    private static final double TILE_HEIGHT = 150;

    public static void main(String[] args) {
        launch();
    }

    public void start(Stage stage) {

        Tile clockTile = TileBuilder.create()
                .skinType(Tile.SkinType.CLOCK)
                .title("Clock")
                .text("Clock")
                .prefSize(150, 150)
                .dateVisible(true)
                .locale(Locale.US)
                .running(true)
                .build();

        Tile dateTile = TileBuilder.create()
                .skinType(Tile.SkinType.DATE)
                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                .build();

        Tile temperatureTile = TileBuilder.create().skinType(Tile.SkinType.CHARACTER)
                .prefSize(TILE_WIDTH + 150, TILE_HEIGHT)
                .title("Temperature")
                .titleAlignment(TextAlignment.CENTER)
                .description("...")
                .build();

        Tile windSpeedTile = TileBuilder.create().skinType(Tile.SkinType.CHARACTER)
                .prefSize(TILE_WIDTH + 150, TILE_HEIGHT)
                .title("Wind Speed")
                .titleAlignment(TextAlignment.CENTER)
                .description("...")
                .build();

        Tile windDirectionTile = TileBuilder.create().skinType(Tile.SkinType.CHARACTER)
                .prefSize(TILE_WIDTH + 150, TILE_HEIGHT)
                .title("Wind Direction")
                .titleAlignment(TextAlignment.CENTER)
                .description("...")
                .build();

        Tile areaChartTileTemperature = TileBuilder.create()
                .skinType(Tile.SkinType.SMOOTHED_CHART)
                .prefSize(TILE_WIDTH + 150, TILE_HEIGHT)
                .title("Hourly Temperature")
                .chartType(Tile.ChartType.AREA)
                .animated(false)
                .smoothing(true)
                .tooltipTimeout(1000)
                .build();

        Tile areaChartTileRain = TileBuilder.create()
                .skinType(Tile.SkinType.SMOOTHED_CHART)
                .prefSize(TILE_WIDTH + 150, TILE_HEIGHT)
                .title("Hourly Rain")
                .chartType(Tile.ChartType.AREA)
                .animated(false)
                .smoothing(true)
                .tooltipTimeout(1000)
                .build();

        Tile areaChartTileHumidity = TileBuilder.create()
                .skinType(Tile.SkinType.SMOOTHED_CHART)
                .prefSize(TILE_WIDTH + 150, TILE_HEIGHT)
                .title("Hourly Humidity")
                .chartType(Tile.ChartType.AREA)
                .animated(false)
                .smoothing(true)
                .tooltipTimeout(1000)
                .build();

        Tile areaChartTileUV = TileBuilder.create()
                .skinType(Tile.SkinType.SMOOTHED_CHART)
                .prefSize(TILE_WIDTH + 150, TILE_HEIGHT)
                .title("Daily UV Index")
                .chartType(Tile.ChartType.AREA)
                .animated(false)
                .smoothing(true)
                .tooltipTimeout(1000)
                .build();

        Tile areaChartTileWindSpeed = TileBuilder.create()
                .skinType(Tile.SkinType.SMOOTHED_CHART)
                .prefSize(TILE_WIDTH + 150, TILE_HEIGHT)
                .title("Wind Speed")
                .chartType(Tile.ChartType.AREA)
                .animated(false)
                .smoothing(true)
                .tooltipTimeout(1000)
                .build();

        Tile areaChartTileWindGusts = TileBuilder.create()
                .skinType(Tile.SkinType.SMOOTHED_CHART)
                .prefSize(TILE_WIDTH + 150, TILE_HEIGHT)
                .title("Wind Gusts")
                .chartType(Tile.ChartType.AREA)
                .animated(false)
                .smoothing(true)
                .tooltipTimeout(1000)
                .build();

        Tile areaChartTilePrecipitation = TileBuilder.create()
                .skinType(Tile.SkinType.SMOOTHED_CHART)
                .prefSize(TILE_WIDTH + 150, TILE_HEIGHT)
                .title("Precipitation probability")
                .chartType(Tile.ChartType.AREA)
                .animated(false)
                .smoothing(true)
                .tooltipTimeout(1000)
                .build();

        FlowGridPane pane = new FlowGridPane(3, 4,
                clockTile, dateTile, temperatureTile, areaChartTileTemperature,
                windSpeedTile, windDirectionTile, areaChartTileWindSpeed,
                areaChartTileWindGusts, areaChartTileHumidity,
                areaChartTileRain, areaChartTilePrecipitation, areaChartTileUV);

        pane.setHgap(5);
        pane.setVgap(5);
        pane.setAlignment(Pos.CENTER);
        pane.setCenterShape(true);
        pane.setPadding(new Insets(5));
        pane.setBackground(new Background(new BackgroundFill(Color.web("#101214"), CornerRadii.EMPTY, Insets.EMPTY)));

        PerspectiveCamera camera = new PerspectiveCamera();
        camera.setFieldOfView(10);

        Scene scene = new Scene(pane);
        scene.setCamera(camera);
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.setTitle("PiMonitor");
        pane.autosize();

        stage.show();

        stage.setOnCloseRequest(t -> {
            Platform.exit();
            System.exit(0);
        });

        ActorSystem system = ActorSystem.create("PiMonitor");
        ActorRef ui = system.actorOf(Props.create(UIActor.class, temperatureTile, areaChartTileTemperature,
                windDirectionTile, windSpeedTile, areaChartTileHumidity, areaChartTileUV,
                areaChartTileRain, areaChartTilePrecipitation,
                areaChartTileWindSpeed, areaChartTileWindGusts, clockTile), "ui");
        system.actorOf(Props.create(WeatherActor.class, ui), "weather");
        system.actorOf(Props.create(NamedayActor.class, ui), "namedays");
    }
}