package org.pimonitor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import eu.hansolo.medusa.Gauge;
import eu.hansolo.medusa.GaugeBuilder;
import eu.hansolo.tilesfx.*;
import eu.hansolo.tilesfx.tools.FlowGridPane;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainFX extends Application{

    private static final double TILE_WIDTH = 200;
    private static final double TILE_HEIGHT = 150;

    private final List<Stage> stages = new ArrayList<>();

    public static void main(String[] args) {
        launch();
    }

    public void start(Stage stage) {

        Tile clockTile = TileBuilder.create()
                .skinType(Tile.SkinType.CLOCK)
                .title("Clock")
                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                .showInfoRegion(true)
                .tooltipText("News appearing here")
                .infoRegionTooltipText("News appearing here")
                .dateVisible(true)
                .locale(Locale.US)
                .running(true)
                .build();

        clockTile.onMouseClickedProperty().setValue(event -> {
            if (clockTile.getTooltip().isShowing()) clockTile.getTooltip().hide();
            else {
                clockTile.setTooltipText(clockTile.getInfoRegionTooltipText());
                clockTile.getTooltip().setFont(Font.font(14));
                clockTile.getTooltip().show(clockTile.getParent().getScene().getWindow());
            }
        });

        Tile nameDayTile = TileBuilder.create()
                .skinType(Tile.SkinType.TEXT)
                .textSize(Tile.TextSize.BIGGER)
                .descriptionAlignment(Pos.CENTER)
                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                .title("Nameday")
                .text("What nameday is today?")
                .description("...")
                .build();

        Tile temperatureTile = TileBuilder.create().skinType(Tile.SkinType.CHARACTER)
                .textSize(Tile.TextSize.BIGGER)
                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                .title("Temperature")
                .titleAlignment(TextAlignment.CENTER)
                .description("...")
                .build();

        Tile apparentTemperatureTile = TileBuilder.create().skinType(Tile.SkinType.CHARACTER)
                .textSize(Tile.TextSize.BIGGER)
                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                .title("Apparent Temperature")
                .titleAlignment(TextAlignment.CENTER)
                .description("...")
                .build();

        Tile radarTile = TileBuilder.create()
                .skinType(Tile.SkinType.IMAGE)
                .textSize(Tile.TextSize.BIGGER)
                .descriptionAlignment(Pos.BASELINE_LEFT)
                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                .image(new Image("https://www.dwd.de/DWD/wetter/radar/radfilm_brd_akt.gif"))
                .title("DWDS radar")
                .description("...")
                .build();

        if (radarTile.getImage().isError()) {
            System.err.println("PI-MONITOR IMAGE ERROR");
            System.err.println(radarTile.getImage().getException());
            System.err.println(radarTile.getImage().errorProperty());
        }

        Tile windSpeedTile = TileBuilder.create().skinType(Tile.SkinType.CHARACTER)
                .textSize(Tile.TextSize.BIGGER)
                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                .title("Wind Speed")
                .titleAlignment(TextAlignment.CENTER)
                .description("...")
                .build();

        Gauge windDirectionGauge = GaugeBuilder.create()
                .minValue(0)
                .maxValue(360)
                .startAngle(90)
                .angleRange(360)
                .autoScale(true)
                .tickMarkColor(Color.WHITE)
                .knobColor(Tile.BLUE)
                .valueVisible(false)
                .tickLabelsVisible(false)
                .needleColor(Color.WHITE)
                .build();

        Tile windDirectionTile = TileBuilder.create().skinType(Tile.SkinType.CUSTOM)
                .textSize(Tile.TextSize.BIGGER)
                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                .title("Wind Direction")
                .titleAlignment(TextAlignment.CENTER)
                .graphic(windDirectionGauge)
                .description("...")
                .build();

        Tile areaChartTileTemperature = TileBuilder.create()
                .textSize(Tile.TextSize.BIGGER)
                .skinType(Tile.SkinType.SMOOTHED_CHART)
                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                .title("Hourly Temperature")
                .chartType(Tile.ChartType.AREA)
                .animated(false)
                .smoothing(true)
                .tooltipTimeout(1000)
                .build();

        Tile areaChartTileLQI = TileBuilder.create()
                .skinType(Tile.SkinType.MATRIX)
                .textSize(Tile.TextSize.BIGGER)
                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                .title("LQI")
                .text("LQI Umweltbundesamt")
                .matrixSize(3,10)
                .maxValue(6)
                .minValue(-1)
                .build();

        Tile areaChartTilePollen = TileBuilder.create()
                .skinType(Tile.SkinType.MATRIX)
                .textSize(Tile.TextSize.BIGGER)
                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                .title("Pollen")
                .text("Pollen forecast DWD")
                .matrixSize(24,10)
                .maxValue(3)
                .minValue(-1)
                .build();

        Tile areaChartTileHumidity = TileBuilder.create()
                .textSize(Tile.TextSize.BIGGER)
                .skinType(Tile.SkinType.SMOOTHED_CHART)
                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                .title("Hourly Humidity")
                .chartType(Tile.ChartType.AREA)
                .animated(false)
                .smoothing(true)
                .tooltipTimeout(1000)
                .build();

        Tile areaChartTileRain = TileBuilder.create()
                .textSize(Tile.TextSize.BIGGER)
                .skinType(Tile.SkinType.SMOOTHED_CHART)
                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                .title("Hourly Rain")
                .chartType(Tile.ChartType.AREA)
                .animated(false)
                .smoothing(true)
                .tooltipTimeout(1000)
                .build();

        EventHandler<MouseEvent> radarStageHandler = new ImageStageHandler<>("https://www.dwd.de/DWD/wetter/radar/radfilm_brd_akt.gif",
                stages, 0.5);

        radarTile.onMouseClickedProperty().setValue(radarStageHandler);

        Tile areaChartTileUV = TileBuilder.create()
                .textSize(Tile.TextSize.BIGGER)
                .skinType(Tile.SkinType.SMOOTHED_CHART)
                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                .title("Daily UV Index")
                .chartType(Tile.ChartType.AREA)
                .animated(false)
                .smoothing(true)
                .tooltipTimeout(1000)
                .build();

        Tile areaChartTileWindSpeed = TileBuilder.create()
                .textSize(Tile.TextSize.BIGGER)
                .skinType(Tile.SkinType.SMOOTHED_CHART)
                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                .title("Wind Speed")
                .chartType(Tile.ChartType.AREA)
                .animated(false)
                .smoothing(true)
                .tooltipTimeout(1000)
                .build();

        Tile areaChartTileWindGusts = TileBuilder.create()
                .textSize(Tile.TextSize.BIGGER)
                .skinType(Tile.SkinType.SMOOTHED_CHART)
                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                .title("Wind Gusts")
                .chartType(Tile.ChartType.AREA)
                .animated(false)
                .smoothing(true)
                .tooltipTimeout(1000)
                .build();

        Tile areaChartTilePrecipitation = TileBuilder.create()
                .textSize(Tile.TextSize.BIGGER)
                .skinType(Tile.SkinType.SMOOTHED_CHART)
                .prefSize(TILE_WIDTH, TILE_HEIGHT)
                .title("Precipitation probability")
                .chartType(Tile.ChartType.AREA)
                .animated(false)
                .smoothing(true)
                .tooltipTimeout(1000)
                .build();

        FlowGridPane pane = new FlowGridPane(4, 4,
                clockTile, nameDayTile, temperatureTile, apparentTemperatureTile,
                windSpeedTile, windDirectionTile, areaChartTileHumidity, radarTile, areaChartTileWindSpeed,
                areaChartTileWindGusts, areaChartTileTemperature, areaChartTileLQI,
                areaChartTileRain, areaChartTilePrecipitation, areaChartTileUV, areaChartTilePollen);

        pane.setNoOfCols(3);
        pane.setNoOfCols(4);
        pane.setNoOfRows(3);
        pane.setNoOfRows(4);

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
        stage.getWidth();
        stage.getHeight();

        stage.show();

        stage.setOnCloseRequest(t -> {
            Platform.exit();
            System.exit(0);
        });

        ActorSystem system = ActorSystem.create("PiMonitor");
        ActorRef ui = system.actorOf(Props.create(UIActor.class, temperatureTile, areaChartTileTemperature,
                windDirectionTile, windDirectionGauge, windSpeedTile, areaChartTileHumidity, areaChartTileUV,
                areaChartTileRain, areaChartTilePrecipitation,
                areaChartTileWindSpeed, areaChartTileWindGusts, nameDayTile, apparentTemperatureTile, radarTile,
                areaChartTileLQI, areaChartTilePollen, clockTile), "ui");
        system.actorOf(Props.create(WeatherActor.class, ui), "weather");
        system.actorOf(Props.create(NamedayActor.class, ui), "namedays");
        system.actorOf(Props.create(PollenActor.class, ui), "pollen");
        system.actorOf(Props.create(LQIActor.class, ui), "lqi");
        //system.actorOf(Props.create(ClockActor.class, ui), "clock");
        system.actorOf(Props.create(NewsFeedActor.class, ui), "ieidiseis");
    }
}