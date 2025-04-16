package it.arturoiafrate.shortcutbuddy.config.module;

import dagger.Module;
import dagger.Provides;
import jakarta.inject.Singleton;
import javafx.application.HostServices;

import java.util.ResourceBundle;

@Module
public class FxModule {
    private final ResourceBundle resourceBundle;
    private final HostServices appHostServices;

    public FxModule(ResourceBundle resourceBundle, HostServices appHostServices) {
        this.resourceBundle = resourceBundle;
        this.appHostServices = appHostServices;
    }

    @Provides
    @Singleton
    public ResourceBundle provideResourceBundle() {
        return resourceBundle;
    }

    @Provides
    @Singleton
    public HostServices provideHostServices() {
        return appHostServices;
    }
}
