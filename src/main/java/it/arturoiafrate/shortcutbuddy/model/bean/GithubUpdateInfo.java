package it.arturoiafrate.shortcutbuddy.model.bean;

import org.apache.commons.lang3.StringUtils;

public record GithubUpdateInfo(String tag_name, String published_at, String html_url, String body) {
    public GithubUpdateInfo {
        if(!StringUtils.isEmpty(tag_name) && tag_name.startsWith("v")) {
            tag_name = tag_name.replace("v", "");
        }
    }
}
