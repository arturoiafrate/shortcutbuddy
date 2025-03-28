module it.arturoiafrate.shortcutbuddy {
    requires javafx.fxml;
    requires com.github.kwhat.jnativehook;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires org.apache.commons.io;
    requires com.google.gson;
    requires org.apache.commons.lang3;
    requires atlantafx.base;


    opens it.arturoiafrate.shortcutbuddy to javafx.fxml;
    exports it.arturoiafrate.shortcutbuddy;
    exports it.arturoiafrate.shortcutbuddy.controller;
    opens it.arturoiafrate.shortcutbuddy.controller to javafx.fxml;
    opens it.arturoiafrate.shortcutbuddy.model.bean to com.google.gson;
}