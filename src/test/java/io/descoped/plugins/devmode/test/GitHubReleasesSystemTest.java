package io.descoped.plugins.devmode.test;

import io.descoped.plugins.devmode.mojo.GitHubReleases;
import io.descoped.plugins.devmode.mojo.GitHubUrl;
import org.apache.maven.plugin.logging.Log;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

public class GitHubReleasesSystemTest {

    private static Log LOGGER = io.descoped.plugins.devmode.util.Logger.INSTANCE;

    @Ignore
    @Test
    public void testMe() throws Exception {
        GitHubReleases installer = new GitHubReleases();
        List<GitHubUrl> releaseList = installer.getHotswapReleaseList();
        releaseList.forEach(i -> {
            LOGGER.info("" + i.getTag());
        });
    }
}
