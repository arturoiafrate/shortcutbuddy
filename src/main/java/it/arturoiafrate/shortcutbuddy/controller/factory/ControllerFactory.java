package it.arturoiafrate.shortcutbuddy.controller.factory;

import it.arturoiafrate.shortcutbuddy.config.ApplicationComponent;
import it.arturoiafrate.shortcutbuddy.controller.SettingsController;
import it.arturoiafrate.shortcutbuddy.controller.ShortcutController;
import it.arturoiafrate.shortcutbuddy.controller.UserShortcutsController;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.util.Callback;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class ControllerFactory implements Callback<Class<?>, Object>{

    private final ApplicationComponent applicationComponent;

    @Inject
    public ControllerFactory(ApplicationComponent applicationComponent) {
        this.applicationComponent = applicationComponent;
    }

    @Override
    public Object call(Class<?> controllerClass) {
        if (controllerClass == SettingsController.class) {
            return applicationComponent.getSettingsController();
        } else if (controllerClass == ShortcutController.class){
            return applicationComponent.getShortcutController();
        } else if (controllerClass == UserShortcutsController.class){
            return applicationComponent.getUserShortcutsController();
        }
        log.error("No controller found for class: {}", controllerClass.getName());
        return null;
    }
}
