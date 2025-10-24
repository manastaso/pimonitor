package org.pimonitor;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.util.List;

public class ImageStageHandler<T extends Event> implements EventHandler<T> {

    private final List<Stage> stages;
    private final String imageURL;
    private final double scaleFactor;

    public ImageStageHandler(String imageURL, List<Stage> stages, double scaleFactor) {
        this.imageURL = imageURL;
        this.stages = stages;
        this.scaleFactor=scaleFactor;
    }

    @Override
    public void handle(T event) {
        Image image = new Image(imageURL);
        ImageView imageView = new ImageView(image);
        imageView.setCache(false);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(image.getWidth() * scaleFactor);
        BorderPane pane = new BorderPane();
        pane.setCenter(imageView);
        Scene scene = new Scene(pane);
        Stage stage = new Stage();

        stage.setScene(scene);
        stages.add(stage);
        imageView.onMouseClickedProperty().setValue(event1 -> {
            stages.forEach(listedStage -> {
                Parent root = listedStage.getScene().getRoot();
                listedStage.close();
            });
        });
        stage.showAndWait();
    }
}
