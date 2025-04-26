package it.arturoiafrate.shortcutbuddy.controller.dialog;

import atlantafx.base.theme.Styles;
import it.arturoiafrate.shortcutbuddy.ShortcutBuddyApp;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.feather.Feather;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * Utility class for creating and displaying dialog windows.
 * Provides methods for showing information and action dialogs with consistent styling.
 */
@Slf4j
public class DialogUtils {
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private DialogUtils() {}

    /**
     * Shows a dialog with two buttons: an action button and a close button.
     * The action button will execute the provided action when clicked.
     * 
     * @param title The title of the dialog
     * @param content The content node to display in the dialog
     * @param icon Optional icon for the dialog window
     * @param owner Optional owner window for the dialog
     * @param action The action to execute when the action button is clicked
     */
    public static void showActionDialog(String title, Node content, Optional<Image> icon, Optional<Window> owner, Consumer<ActionEvent> action) {
        log.debug("Showing action dialog: {}", title);
        Stage dialogStage = createDialogStage(title, content, icon, owner);

        HBox buttonBar = new HBox();
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        Button actionButton = new Button();
        actionButton.setGraphic(new FontIcon(Feather.CHECK_CIRCLE));
        actionButton.getStyleClass().addAll(Styles.SUCCESS, Styles.FLAT, Styles.BUTTON_ICON);
        actionButton.setOnAction(e -> {
            log.debug("Action button clicked in dialog: {}", title);
            action.accept(e);
            dialogStage.close();
        });

        Button closeButton = new Button();
        closeButton.setGraphic(new FontIcon(Feather.X_CIRCLE));
        closeButton.getStyleClass().addAll(Styles.DANGER, Styles.FLAT, Styles.BUTTON_ICON);
        closeButton.setDefaultButton(true);
        closeButton.setCancelButton(true);
        closeButton.setOnAction(e -> {
            log.debug("Close button clicked in dialog: {}", title);
            dialogStage.close();
        });

        buttonBar.getChildren().addAll(actionButton, closeButton);
        ((VBox) dialogStage.getScene().getRoot()).getChildren().add(buttonBar);

        dialogStage.showAndWait();
        log.debug("Action dialog closed: {}", title);
    }

    /**
     * Shows an information dialog with a single close button.
     * 
     * @param title The title of the dialog
     * @param content The content node to display in the dialog
     * @param icon Optional icon for the dialog window
     * @param owner Optional owner window for the dialog
     */
    public static void showInfoDialog(String title, Node content, Optional<Image> icon, Optional<Window> owner) {
        log.debug("Showing info dialog: {}", title);
        Stage dialogStage = createDialogStage(title, content, icon, owner);

        HBox buttonBar = new HBox();
        buttonBar.setAlignment(Pos.CENTER_RIGHT);

        Button closeButton = new Button();
        closeButton.setGraphic(new FontIcon(Feather.CHECK_CIRCLE));
        closeButton.getStyleClass().addAll(Styles.ACCENT, Styles.FLAT, Styles.BUTTON_ICON);
        closeButton.setDefaultButton(true);
        closeButton.setCancelButton(true);
        closeButton.setOnAction(e -> {
            log.debug("Close button clicked in info dialog: {}", title);
            dialogStage.close();
        });

        buttonBar.getChildren().add(closeButton);
        ((VBox) dialogStage.getScene().getRoot()).getChildren().add(buttonBar);

        dialogStage.showAndWait();
        log.debug("Info dialog closed: {}", title);
    }

    /**
     * Creates a dialog stage with consistent styling and configuration.
     * 
     * @param title The title of the dialog
     * @param content The content node to display in the dialog
     * @param icon Optional icon for the dialog window
     * @param owner Optional owner window for the dialog
     * @return A configured Stage ready to be shown
     */
    private static Stage createDialogStage(String title, Node content, Optional<Image> icon, Optional<Window> owner) {
        log.debug("Creating dialog stage: {}", title);
        Stage dialogStage = new Stage();
        dialogStage.setTitle(title);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.initOwner(owner.orElse(null));
        icon.ifPresent(image -> dialogStage.getIcons().add(image));

        // Create the root layout
        VBox rootLayout = new VBox();
        rootLayout.setSpacing(15);
        rootLayout.setPadding(new Insets(20));
        rootLayout.setAlignment(Pos.CENTER);
        rootLayout.getChildren().add(content);
        VBox.setVgrow(content, Priority.SOMETIMES);

        // Configure content size if it's a Region
        if (content instanceof Region) {
            ((Region) content).setPrefWidth(500);
            ((Region) content).setMinHeight(Region.USE_PREF_SIZE);
        }

        // Set up the scene and stage properties
        Scene dialogScene = new Scene(rootLayout);
        dialogStage.setScene(dialogScene);
        dialogStage.sizeToScene();
        dialogStage.setMinWidth(500);
        dialogStage.setMinHeight(350);
        dialogStage.setResizable(false);

        // Register with the application for proper lifecycle management
        ShortcutBuddyApp app = ShortcutBuddyApp.getInstance();
        if (app != null) {
            app.registerStage(dialogStage);
            log.debug("Dialog stage registered with application");
        } else {
            log.warn("Could not register dialog stage: application instance is null");
        }

        return dialogStage;
    }
}
