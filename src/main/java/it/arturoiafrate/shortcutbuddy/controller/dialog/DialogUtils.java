package it.arturoiafrate.shortcutbuddy.controller.dialog;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Optional;

public class DialogUtils {
    private DialogUtils() {}

    public static void showInfoDialog(String title, Node content, Optional<Image> icon, Optional<Window> owner) {
        Stage dialogStage = new Stage();
        dialogStage.setTitle(title);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(owner.orElse(null));
        icon.ifPresent(image -> dialogStage.getIcons().add(image));
        VBox rootLayout = new VBox();
        rootLayout.setSpacing(15);
        rootLayout.setPadding(new Insets(20));
        rootLayout.setAlignment(Pos.CENTER);
        rootLayout.getChildren().add(content);
        VBox.setVgrow(content, Priority.SOMETIMES);
        if (content instanceof Region) {
            ((Region)content).setPrefWidth(500);
            ((Region)content).setMinHeight(Region.USE_PREF_SIZE);
        }
        HBox buttonBar = new HBox();
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        Button closeButton = new Button("OK");
        closeButton.setDefaultButton(true);
        closeButton.setCancelButton(true);
        closeButton.setOnAction(e -> dialogStage.close());
        buttonBar.getChildren().add(closeButton);
        rootLayout.getChildren().add(buttonBar);
        Scene dialogScene = new Scene(rootLayout);
        dialogStage.setScene(dialogScene);
        dialogStage.sizeToScene();
        dialogStage.setMinWidth(500);
        dialogStage.setMinHeight(350);
        dialogStage.setResizable(false);
        dialogStage.showAndWait();
    }

    private static void setupDialogIcon(DialogPane dialogPane, final Image icon) {
        ChangeListener<Scene> sceneListener = getSceneChangeListener(icon);

        dialogPane.sceneProperty().addListener(sceneListener);

        dialogPane.getScene().windowProperty().addListener(new ChangeListener<Window>() {
            @Override
            public void changed(ObservableValue<? extends Window> observable, Window oldValue, Window newValue) {
                if (newValue != null) {
                    dialogPane.sceneProperty().removeListener(sceneListener);
                    observable.removeListener(this);
                }
            }
        });
    }

    private static ChangeListener<Scene> getSceneChangeListener(Image icon) {
        ChangeListener<Window> windowListener = (windowObs, oldWin, newWin) -> {
            if (newWin instanceof Stage) {
                ((Stage) newWin).getIcons().setAll(icon);
            }
        };
        return (sceneObs, oldScene, newScene) -> {
            if (oldScene != null) {
                oldScene.windowProperty().removeListener(windowListener);
            }
            if (newScene != null) {
                Window currentWindow = newScene.getWindow();
                if (currentWindow instanceof Stage) {
                    ((Stage) currentWindow).getIcons().setAll(icon);
                }
                newScene.windowProperty().addListener(windowListener);
            }
        };
    }
}
