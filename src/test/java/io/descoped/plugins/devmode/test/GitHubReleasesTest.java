package io.descoped.plugins.devmode.test;

import io.descoped.plugins.devmode.mojo.GitHubReleases;
import io.descoped.plugins.devmode.mojo.GitHubUrl;
import io.descoped.plugins.devmode.util.CommonUtil;
import io.descoped.plugins.devmode.util.JavaVersion;
import io.descoped.plugins.devmode.util.Logger;
import org.apache.maven.plugin.logging.Log;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by oranheim on 12/01/2017.
 */
public class GitHubReleasesTest {

    private static Log LOGGER = Logger.INSTANCE;
    private static String JDK_VERSION = String.format("%s-u%s_%s", (JavaVersion.isJdk8() ? "8" : "7"), JavaVersion.getMinor(), JavaVersion.getBuild());

    private String loadTemplate(String resourceName) throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream in = cl.getResourceAsStream(resourceName);
        OutputStream out = CommonUtil.newOutputStream();
        CommonUtil.writeInputToOutputStream(in, out);
        return out.toString();
    }

    private void batchAddComments(StringBuffer bash) throws IOException {
        String txt = loadTemplate("hotswap-install-help.txt");
        txt = txt.replace("@JDK_VERSION", JDK_VERSION);
        bash.append(txt);
    }

    @Test
    public void testMessage() throws Exception {
        StringBuffer buf = new StringBuffer();
        batchAddComments(buf);
        assertTrue(buf.toString().contains(JDK_VERSION));
        LOGGER.info(buf.toString());
    }

    @Test
    public void testJavaVersion() throws Exception {
        LOGGER.info("Java version: " + JavaVersion.getVersion() + " ver: " +
                JavaVersion.getMajor() + " major: " + JavaVersion.getMinor()+ " minor: " + JavaVersion.getBuild() +
                " isJdk8: " + JavaVersion.isJdk8());
        assertTrue(JavaVersion.isJdk8());
    }

    @Test
    public void testIfHotswapIsInstalled() throws Exception {
        GitHubReleases releases = mock(GitHubReleases.class);
        when(releases.isHotswapInstalled()).thenReturn(false);
        boolean exists = releases.isHotswapInstalled();
        assertFalse(exists);
    }

    @Test
    public void testHotswapLatestRelease() throws Exception {
        GitHubReleases releases = MockHelper.mockGitHubReleases();
        GitHubUrl url = releases.getHotswapLatestReleaseVersion();
        assertNotNull(url.getTag());
        assertNotNull(url.getUrl());
        LOGGER.info("HotswapLatestRelease\t..tagName: " + url.getTag() + " \tbrowser_download_url: " + url.getUrl());
    }

    @Test
    public void testHotswapReleases() throws Exception {
        GitHubReleases releases = MockHelper.mockGitHubReleases();
        GitHubUrl latestVersion = releases.getHotswapLatestReleaseVersion();
        List<GitHubUrl> urls = releases.getHotswapReleaseList();
        if (!CommonUtil.isTravisCI()) assertEquals(5, urls.size());
        GitHubUrl matchUrl = releases.findMatchingHotswapVersion(urls);
        LOGGER.info("---o  + = latest-release");
        LOGGER.info("---o  * = best-match");
        urls.forEach(url -> {
            boolean latest = (url.equalTo(latestVersion));
            boolean match = (url.equalTo(matchUrl));
            String tail = String.format(" %s", (latest ? "(+)" : ""));
            tail += String.format(" %s", (match ? "(*)" : ""));
            LOGGER.info("HotswapReleases\t\t..tagName: " + url.getTag() + " \tbrowser_download_url: " + url.getUrl() + tail);
        });
    }

    @Test
    public void testHotswapAgentLatestRelease() throws Exception {
        GitHubReleases releases = MockHelper.mockGitHubReleases();
        GitHubUrl url = releases.getHotswapAgentLatestReleaseVersion();
        assertNotNull(url.getTag());
        assertNotNull(url.getUrl());
        LOGGER.info("HotswapAgentLatestRelease\t..tagName: " + url.getTag() + " \t\t\tbrowser_download_url: " + url.getUrl());
    }

    @Test
    public void testHotswapAgentReleases() throws Exception {
        GitHubReleases releases = MockHelper.mockGitHubReleases();
        GitHubUrl latestVersion = releases.getHotswapAgentLatestReleaseVersion();
        List<GitHubUrl> urls = releases.getHotswapAgentReleaseList();
        if (!CommonUtil.isTravisCI()) assertEquals(4, urls.size());
        GitHubUrl matchUrl = releases.findMatchingHotswapAgentVersion(urls);
        urls.forEach(url -> {
            boolean latest = (url.equalTo(latestVersion));
            boolean match = (url.equalTo(matchUrl));
            String tail = String.format(" %s", (latest ? "(+)" : ""));
            tail += String.format(" %s", (match ? "(*)" : ""));
            LOGGER.info("HotswapAgentReleases\t\t..tagName: " + url.getTag() + " \tbrowser_download_url: " + url.getUrl() + tail);
        });
    }

}


