package it.arturoiafrate.shortcutbuddy.controller.dialog;

import atlantafx.base.theme.Styles;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
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
import java.util.function.Consumer;

public class DialogUtils {
    private DialogUtils() {}

    public static void showActionDialog(String title, Node content, Optional<Image> icon, Optional<Window> owner, Consumer<ActionEvent> action) {
        Stage dialogStage = createDialogStage(title, content, icon, owner);
        HBox buttonBar = new HBox();
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        Button actionButton = new Button("OK");
        actionButton.getStyleClass().add(Styles.ACCENT);
        actionButton.setOnAction(e -> {
            action.accept(e);
            dialogStage.close();
        });
        Button closeButton = new Button("Dismiss");
        closeButton.getStyleClass().add(Styles.DANGER);
        closeButton.setDefaultButton(true);
        closeButton.setCancelButton(true);
        closeButton.setOnAction(e -> dialogStage.close());
        buttonBar.getChildren().addAll(actionButton, closeButton);
        ((VBox) dialogStage.getScene().getRoot()).getChildren().add(buttonBar);
        dialogStage.showAndWait();
    }

    public static void showInfoDialog(String title, Node content, Optional<Image> icon, Optional<Window> owner) {
        Stage dialogStage = createDialogStage(title, content, icon, owner);
        HBox buttonBar = new HBox();
        buttonBar.setAlignment(Pos.CENTER_RIGHT);
        Button closeButton = new Button("OK");
        closeButton.setDefaultButton(true);
        closeButton.setCancelButton(true);
        closeButton.setOnAction(e -> dialogStage.close());
        buttonBar.getChildren().add(closeButton);
        ((VBox) dialogStage.getScene().getRoot()).getChildren().add(buttonBar);
        dialogStage.showAndWait();
    }

    private static Stage createDialogStage(String title, Node content, Optional<Image> icon, Optional<Window> owner) {
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
            ((Region) content).setPrefWidth(500);
            ((Region) content).setMinHeight(Region.USE_PREF_SIZE);
        }
        Scene dialogScene = new Scene(rootLayout);
        dialogStage.setScene(dialogScene);
        dialogStage.sizeToScene();
        dialogStage.setMinWidth(500);
        dialogStage.setMinHeight(350);
        dialogStage.setResizable(false);
        return dialogStage;
    }
}
