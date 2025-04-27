package it.arturoiafrate.shortcutbuddy.config;

import dagger.Component;
import it.arturoiafrate.shortcutbuddy.config.module.FxModule;
import it.arturoiafrate.shortcutbuddy.config.module.NotificationModule;
import it.arturoiafrate.shortcutbuddy.config.qualifier.ApplicationTrayNotificationService;
import it.arturoiafrate.shortcutbuddy.controller.*;
import it.arturoiafrate.shortcutbuddy.controller.factory.ControllerFactory;
import it.arturoiafrate.shortcutbuddy.model.interceptor.foreground.ForegroundAppInterceptor;
import it.arturoiafrate.shortcutbuddy.model.interceptor.keylistener.KeyListener;
import it.arturoiafrate.shortcutbuddy.model.keyemulator.KeyEmulator;
import it.arturoiafrate.shortcutbuddy.model.manager.database.DatabaseManager;
import it.arturoiafrate.shortcutbuddy.model.manager.database.repository.SettingsRepository;
import it.arturoiafrate.shortcutbuddy.model.manager.database.repository.ShortcutRepository;
import it.arturoiafrate.shortcutbuddy.model.manager.settings.SettingsManager;
import it.arturoiafrate.shortcutbuddy.model.manager.shortcut.ShortcutManager;
import it.arturoiafrate.shortcutbuddy.model.manager.tray.TrayManager;
import it.arturoiafrate.shortcutbuddy.service.INotificationService;
import it.arturoiafrate.shortcutbuddy.service.impl.ChangelogService;
import it.arturoiafrate.shortcutbuddy.service.impl.GithubApiClient;
import it.arturoiafrate.shortcutbuddy.service.impl.UpdateCheckerService;
import jakarta.inject.Singleton;

@Singleton
@Component(modules = {FxModule.class, NotificationModule.class})
public interface ApplicationComponent {
    DatabaseManager getDatabaseManager();
    ShortcutRepository getShortcutRepository();
    SettingsRepository getSettingsRepository();
    ShortcutManager getShortcutManager();
    SettingsManager getSettingsManager();
    ChangelogService getChangelogService();
    TrayManager getTrayManager();
    GithubApiClient getGithubApiClient();
    UpdateCheckerService getUpdateCheckerService();
    ForegroundAppInterceptor getForegroundAppInterceptor();
    KeyEmulator getKeyEmulator();
    KeyListener getKeyListener();
    SettingsController getSettingsController();
    ShortcutController getShortcutController();
    ShortcutEditorController getShortcutEditorController();
    AppShortcutEditorController getAppShortcutEditorController();
    AppShortcutEditorDialogController getAppShortcutEditorDialogController();
    ControllerFactory getControllerFactory();

    @ApplicationTrayNotificationService
    INotificationService getApplicationTrayNotificationService();

    void inject(ShortcutController shortcutController);
    void inject(SettingsController settingsController);
    void inject(ShortcutEditorController shortcutEditorController);
    void inject(AppShortcutEditorController appShortcutEditorController);
    void inject(AppShortcutEditorDialogController appShortcutEditorDialogController);
}
