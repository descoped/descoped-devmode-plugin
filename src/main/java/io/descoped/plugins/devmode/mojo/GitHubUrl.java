package io.descoped.plugins.devmode.mojo;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

/**
 * Created by oranheim on 08/01/2017.
 */
public class GitHubUrl {

    private  final String tag;
    private final String url;

    public GitHubUrl(String tag, String url) {
        this.tag = tag;
        this.url = url;
    }

    public String getTag() {
        return tag;
    }

    public String getUrl() {
        return url;
    }

    public boolean equalTo(GitHubUrl gitHubUrl) {
        return url.equals(gitHubUrl.getUrl());
    }

    public String getDecodedUrl() {
        try {
            return URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
