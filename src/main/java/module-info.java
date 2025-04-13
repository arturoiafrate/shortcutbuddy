module it.arturoiafrate.shortcutbuddy {
    requires javafx.fxml;
    requires com.github.kwhat.jnativehook;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires org.apache.commons.io;
    requires com.google.gson;
    requires org.apache.commons.lang3;
    requires atlantafx.base;
    requires java.desktop;
    requires static lombok;
    requires org.slf4j;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.feather;


    opens it.arturoiafrate.shortcutbuddy to javafx.fxml;
    exports it.arturoiafrate.shortcutbuddy;
    exports it.arturoiafrate.shortcutbuddy.controller;
    opens it.arturoiafrate.shortcutbuddy.controller to javafx.fxml;
    opens it.arturoiafrate.shortcutbuddy.model.bean to com.google.gson;
}