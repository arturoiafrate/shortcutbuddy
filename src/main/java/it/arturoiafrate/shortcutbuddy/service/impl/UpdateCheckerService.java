package it.arturoiafrate.shortcutbuddy.service.impl;

import com.google.gson.Gson;
import it.arturoiafrate.shortcutbuddy.config.qualifier.ApplicationTrayNotificationService;
import it.arturoiafrate.shortcutbuddy.model.bean.GithubUpdateInfo;
import it.arturoiafrate.shortcutbuddy.model.constant.Label;
import it.arturoiafrate.shortcutbuddy.service.INotificationService;
import it.arturoiafrate.shortcutbuddy.service.IUpdateCheckerService;
import it.arturoiafrate.shortcutbuddy.utility.AppInfo;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import javafx.application.HostServices;
import javafx.application.Platform;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.awt.TrayIcon;
import java.text.MessageFormat;
import java.util.ResourceBundle;

@Slf4j
@Singleton
public class UpdateCheckerService implements IUpdateCheckerService {

    private final GithubApiClient apiClient;
    private final INotificationService notificationService;
    private final ResourceBundle bundle;
    private final HostServices appHostServices;
    private final Gson gson;

    @Inject
    public UpdateCheckerService(GithubApiClient githubApiClient, @ApplicationTrayNotificationService INotificationService notificationService, ResourceBundle bundle, HostServices appHostServices) {
        this.apiClient = githubApiClient;
        this.notificationService = notificationService;
        this.appHostServices = appHostServices;
        this.bundle = bundle;
        this.gson = new Gson();
    }

    @Override
    public void checkForUpdatesAsync(boolean showNotification) {
        apiClient.fetchLatestReleaseJsonAsync().thenAcceptAsync(jsonResponse -> {
            if (!StringUtils.isEmpty(jsonResponse)) {
                try {
                    GithubUpdateInfo latestRelease = gson.fromJson(jsonResponse, GithubUpdateInfo.class);
                    if (latestRelease != null && latestRelease.tag_name() != null) {
                        log.info("Latest Github release: {}", latestRelease);
                        compareAndNotify(latestRelease, showNotification);
                    } else {
                        log.warn("Latest release info invalid or missing tag_name in JSON.");
                        if(showNotification) notificationService.showNotification(
                                MessageFormat.format(bundle.getString(Label.NOTIFICATION_APPUPDATE_FETCH_TITLE), AppInfo.getName()),
                                bundle.getString(Label.NOTIFICATION_APPUPDATE_FETCH_ERROR_GENERIC),
                                TrayIcon.MessageType.ERROR
                        );
                    }
                } catch (Exception e) {
                    log.error("Error while parsing JSON response from GitHub API", e);
                    if(showNotification) notificationService.showNotification(
                            MessageFormat.format(bundle.getString(Label.NOTIFICATION_APPUPDATE_FETCH_TITLE), AppInfo.getName()),
                            bundle.getString(Label.NOTIFICATION_APPUPDATE_FETCH_ERROR_GENERIC),
                            TrayIcon.MessageType.ERROR
                    );
                }
            } else {
                log.warn("Empty or null response from GitHub API.");
                if(showNotification) notificationService.showNotification(
                        MessageFormat.format(bundle.getString(Label.NOTIFICATION_APPUPDATE_FETCH_TITLE), AppInfo.getName()),
                        bundle.getString(Label.NOTIFICATION_APPUPDATE_FETCH_ERROR_GENERIC),
                        TrayIcon.MessageType.ERROR
                );
            }
        });
    }

    private void compareAndNotify(GithubUpdateInfo latestRelease, boolean showNotification) {
        String currentVersion = AppInfo.getVersion();
        String latestVersion = latestRelease.tag_name();
        log.info("Comparing versions: Local='{}', Latest='{}'", currentVersion, latestVersion);

        int comparison = compareVersions(currentVersion, latestVersion);

        if (comparison < 0) {

            log.info("New app version available: {}", latestRelease.tag_name());
            String caption = MessageFormat.format(
                    bundle.getString(Label.NOTIFICATION_APPUPDATE_AVAILABLE_TITLE),
                            AppInfo.getName());
            String text = MessageFormat.format(
                    bundle.getString(Label.NOTIFICATION_APPUPDATE_AVAILABLE_TEXT),
                    latestRelease.tag_name(),
                    latestRelease.body()
            );
            Platform.runLater(() -> {
                if (notificationService != null) {
                    notificationService.showDialog(caption, text, actionEvent -> {
                        if(appHostServices != null) appHostServices.showDocument(latestRelease.html_url());

                    });
                }
            });
        } else if (comparison == 0) {
            log.info("The application is up to date (Version {}).", currentVersion);
            if(showNotification) notificationService.showNotification(
                    MessageFormat.format(
                            bundle.getString(Label.NOTIFICATION_APPUPDATE_FETCH_TITLE),
                            AppInfo.getName()),
                    MessageFormat.format(
                            bundle.getString(Label.NOTIFICATION_APPUPDATE_FETCH_UPTODATE),
                            currentVersion),
                    TrayIcon.MessageType.INFO
            );
        } else {
            log.warn("The local version ({}) seems newer than the latest GitHub release ({}).", currentVersion, latestVersion);
            if(showNotification) notificationService.showNotification(
                    MessageFormat.format(bundle.getString(Label.NOTIFICATION_APPUPDATE_FETCH_TITLE), AppInfo.getName()),
                    MessageFormat.format(bundle.getString(Label.NOTIFICATION_APPUPDATE_FETCH_LOCALWARNING),
                            currentVersion, latestVersion),
                    TrayIcon.MessageType.WARNING
            );
        }
    }

    private int compareVersions(String v1, String v2) {
        if (StringUtils.isEmpty(v1) || StringUtils.isEmpty(v2)) return 0;
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int part1 = (i < parts1.length && parts1[i].matches("\\d+")) ? Integer.parseInt(parts1[i]) : 0;
            int part2 = (i < parts2.length && parts2[i].matches("\\d+")) ? Integer.parseInt(parts2[i]) : 0;
            if (part1 < part2) return -1;
            if (part1 > part2) return 1;
        }
        return 0;
    }
}