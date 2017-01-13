package io.descoped.plugins.devmode.mojo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by oranheim on 13/01/2017.
 */
public class GitHubFactory {

    private static final String GITHUB_RELEASES_FACTORY = GitHubReleases.class.getName();
    private static final Map<String,Object> instances = new ConcurrentHashMap<>();

    public static void setMockInstance(GitHubReleases mockInstance) {
        instances.put(GITHUB_RELEASES_FACTORY, mockInstance);
    }

    public static GitHubReleases getInstance() {
        if (!instances.containsKey(GITHUB_RELEASES_FACTORY)) {
            GitHubReleases instance = new GitHubReleases();
            instances.put(GITHUB_RELEASES_FACTORY, instance);
        }
        return (GitHubReleases) instances.get(GITHUB_RELEASES_FACTORY);
    }

}
