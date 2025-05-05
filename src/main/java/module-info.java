module it.arturoiafrate.shortcutbuddy {
    requires javafx.fxml;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires org.apache.commons.io;
    requires com.google.gson;
    requires org.apache.commons.lang3;
    requires atlantafx.base;
    requires java.desktop;
    requires static lombok;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.feather;
    requires java.net.http;
    requires org.xerial.sqlitejdbc;
    requires flyway.core;
    requires org.slf4j;
    requires jakarta.inject;
    requires dagger;
    requires javax.inject;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.databind;
    requires com.google.errorprone.annotations;
    requires java.compiler;
    requires com.github.benmanes.caffeine;
    requires com.github.kwhat.jnativehook;


    opens it.arturoiafrate.shortcutbuddy to javafx.fxml;
    opens it.arturoiafrate.shortcutbuddy.controller to javafx.fxml;
    opens it.arturoiafrate.shortcutbuddy.model.bean to javafx.base, com.google.gson;
    opens db.migration;
    opens it.arturoiafrate.shortcutbuddy.controller.factory to javafx.fxml;
    opens it.arturoiafrate.shortcutbuddy.model.interceptor.clipboard to com.sun.jna;

    exports it.arturoiafrate.shortcutbuddy.controller.factory;
    exports it.arturoiafrate.shortcutbuddy.model.bean;
    exports it.arturoiafrate.shortcutbuddy;
    exports it.arturoiafrate.shortcutbuddy.controller;
    exports it.arturoiafrate.shortcutbuddy.model.enumerator;
    exports it.arturoiafrate.shortcutbuddy.model.interceptor.clipboard to com.sun.jna;
}
