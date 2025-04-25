package it.arturoiafrate.shortcutbuddy.controller.factory;

import it.arturoiafrate.shortcutbuddy.config.ApplicationComponent;
import it.arturoiafrate.shortcutbuddy.controller.*;
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
        } else if (controllerClass == ShortcutEditorController.class){
            return applicationComponent.getShortcutEditorController();
        } else if (controllerClass == AppShortcutEditorController.class){
            return applicationComponent.getAppShortcutEditorController();
        } else if (controllerClass == AppShortcutEditorDialogController.class){
            return applicationComponent.getAppShortcutEditorDialogController();
        }
        log.error("No controller found for class: {}", controllerClass.getName());
        return null;
    }
}
