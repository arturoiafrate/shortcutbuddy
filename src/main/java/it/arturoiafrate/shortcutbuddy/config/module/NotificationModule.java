package it.arturoiafrate.shortcutbuddy.config.module;

import dagger.Module;
import dagger.Provides;
import it.arturoiafrate.shortcutbuddy.config.qualifier.ApplicationTrayNotificationService;
import it.arturoiafrate.shortcutbuddy.model.manager.tray.TrayManager;
import it.arturoiafrate.shortcutbuddy.service.INotificationService;
import jakarta.inject.Singleton;

@Module
public class NotificationModule {

    @Provides
    @Singleton
    @ApplicationTrayNotificationService
    INotificationService provideApplicationTrayNotificationService(TrayManager trayManager) {
        return trayManager;
    }
}
