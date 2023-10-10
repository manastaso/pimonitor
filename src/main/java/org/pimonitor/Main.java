package org.pimonitor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import eu.hansolo.tilesfx.Tile;
import eu.hansolo.tilesfx.TileBuilder;
import eu.hansolo.tilesfx.tools.FlowGridPane;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.Locale;

public class Main extends Application{

    private static final double TILE_WIDTH = 150;
    private static final double TILE_HEIGHT = 150;

    private Tile clockTile;
    private Tile dateTile;
    private Tile temperatureTile;
    private Tile windSpeedTile;
    private Tile windDirectionTile;
    private Tile areaChartTileTemperature;
    private Tile areaChartTileHumidity;
    private Tile areaChartTileUV;
    private Tile areaChartTileRain;

    public static void main(String[] args) {
        launch();
    }

    public void start(Stage stage) {
        String javaVersion = System.getProperty("java.version");
        String javafxVersion = System.getProperty("javafx.version");
        Label l = new Label();

        clockTile = TileBuilder.create()
                .skinType(Tile.SkinType.CLOCK)
                .prefSize(150, 150)
                .title("Clock Tile")
                .text("Whatever text")
                .dateVisible(true)
                .locale(Locale.US)
                .running(true)
                .build();

        dateTile = TileBuilder.create()
                .skinType(Tile.SkinType.DATE)
                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                .build();

        temperatureTile = TileBuilder.create().skinType(Tile.SkinType.CHARACTER)
                .prefSize(TILE_WIDTH+150, TILE_HEIGHT)
                .title("Temperature")
                .titleAlignment(TextAlignment.CENTER)
                .description("...")
                .build();

        windSpeedTile = TileBuilder.create().skinType(Tile.SkinType.CHARACTER)
                .prefSize(TILE_WIDTH+150, TILE_HEIGHT)
                .title("Wind Speed")
                .titleAlignment(TextAlignment.CENTER)
                .description("...")
                .build();

        windDirectionTile = TileBuilder.create().skinType(Tile.SkinType.CHARACTER)
                .prefSize(TILE_WIDTH+150, TILE_HEIGHT)
                .title("Wind Direction")
                .titleAlignment(TextAlignment.CENTER)
                .description("...")
                .build();

        areaChartTileTemperature = TileBuilder.create()
                .skinType(Tile.SkinType.SMOOTHED_CHART)
                .prefSize(TILE_WIDTH+150, TILE_HEIGHT)
                .title("Hourly Temperature")
                .chartType(Tile.ChartType.AREA)
                .animated(false)
                .smoothing(true)
                .tooltipTimeout(1000)
                .build();

        areaChartTileRain = TileBuilder.create()
                .skinType(Tile.SkinType.SMOOTHED_CHART)
                .prefSize(TILE_WIDTH+150, TILE_HEIGHT)
                .title("Hourly Rain")
                .chartType(Tile.ChartType.AREA)
                .animated(false)
                .smoothing(true)
                .tooltipTimeout(1000)
                .build();

        areaChartTileHumidity = TileBuilder.create()
                .skinType(Tile.SkinType.SMOOTHED_CHART)
                .prefSize(TILE_WIDTH+150, TILE_HEIGHT)
                .title("Hourly Humidity")
                .chartType(Tile.ChartType.AREA)
                .animated(false)
                .smoothing(true)
                .tooltipTimeout(1000)
                .build();

        areaChartTileUV = TileBuilder.create()
                .skinType(Tile.SkinType.SMOOTHED_CHART)
                .prefSize(TILE_WIDTH+150, TILE_HEIGHT)
                .title("Daily UV Index")
                .chartType(Tile.ChartType.AREA)
                .animated(false)
                .smoothing(true)
                .tooltipTimeout(1000)
                .build();

        FlowGridPane pane = new FlowGridPane(3, 3,
                clockTile, dateTile, temperatureTile, windSpeedTile, windDirectionTile, areaChartTileTemperature,
                areaChartTileHumidity, areaChartTileUV, areaChartTileRain);

        pane.setHgap(5);
        pane.setVgap(5);
        pane.setAlignment(Pos.CENTER);
        pane.setCenterShape(true);
        pane.setPadding(new Insets(5));
        //pane.setPrefSize(800, 600);
        pane.setBackground(new Background(new BackgroundFill(Color.web("#101214"), CornerRadii.EMPTY, Insets.EMPTY)));

        PerspectiveCamera camera = new PerspectiveCamera();
        camera.setFieldOfView(10);

        Scene scene = new Scene(pane);
        scene.setCamera(camera);
        stage.setScene(scene);
        stage.show();

        ActorSystem system = ActorSystem.create("PiMonitor");
        ActorRef ui = system.actorOf(Props.create(UIActor.class, temperatureTile, windDirectionTile, windSpeedTile,
                areaChartTileTemperature, areaChartTileHumidity, areaChartTileUV, areaChartTileRain), "ui");
        ActorRef main = system.actorOf(Props.create(WeatherActor.class, ui), "main");

    }
}