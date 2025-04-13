package it.arturoiafrate.shortcutbuddy.service;

import javafx.event.ActionEvent;

import java.awt.*;
import java.util.function.Consumer;

public interface INotificationService {
    void showNotification(String caption, String text, TrayIcon.MessageType messageType);
    void showDialog(String caption, String text, Consumer<ActionEvent> action);
}
