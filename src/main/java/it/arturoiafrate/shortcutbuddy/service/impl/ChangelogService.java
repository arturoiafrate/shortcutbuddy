package it.arturoiafrate.shortcutbuddy.service.impl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import it.arturoiafrate.shortcutbuddy.model.bean.ReleaseInfo;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j; // Usa il tuo logger

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Singleton
public class ChangelogService {

    private static final String CHANGELOG_PATH = "/changelog.json";
    private List<ReleaseInfo> releaseInfoList = null;

    @Inject
    public ChangelogService() {}

    private void loadChangelog() {
        if (releaseInfoList != null) return;
        Gson gson = new Gson();
        Type listType = new TypeToken<List<ReleaseInfo>>() {}.getType();

        try (InputStream is = getClass().getResourceAsStream(CHANGELOG_PATH)) {
            if (is == null) {
                releaseInfoList = Collections.emptyList();
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                releaseInfoList = gson.fromJson(reader, listType);
                if (releaseInfoList == null) {
                    releaseInfoList = Collections.emptyList();
                }
            }
        } catch (Exception e) {
            releaseInfoList = Collections.emptyList();
        }
    }
    public Optional<ReleaseInfo> getReleaseInfo(String version) {
        loadChangelog();
        if (version == null || releaseInfoList.isEmpty()) {
            return Optional.empty();
        }
        return releaseInfoList.stream()
                .filter(info -> version.equals(info.version()))
                .findFirst();
    }

    public List<String> getNotesForVersion(String version) {
        return getReleaseInfo(version)
                .map(ReleaseInfo::notes)
                .orElse(Collections.emptyList());
    }

    public String getReleaseDateForVersion(String version) {
        return getReleaseInfo(version)
                .map(ReleaseInfo::date)
                .orElse(null);
    }

    public List<ReleaseInfo> getLastReleases(int nReleases, String versionToExclude) {
        loadChangelog();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        return releaseInfoList.stream()
                .filter(release -> !release.version().equals(versionToExclude))
                .sorted((r1, r2) -> {
                    try {
                        return dateFormat.parse(r2.date()).compareTo(dateFormat.parse(r1.date()));
                    } catch (Exception e) {
                        log.error("Error parsing date", e);
                        return 0;
                    }
                }).limit(nReleases)
                .toList();
    }
}
