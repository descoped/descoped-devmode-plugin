package io.descoped.plugins.devmode.test;

import io.descoped.plugins.devmode.mojo.GitHubReleases;
import io.descoped.plugins.devmode.mojo.GitHubUrl;
import io.descoped.plugins.devmode.mojo.JavaVersion;
import io.descoped.plugins.devmode.util.CommonUtil;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
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

    private static final Log LOGGER = new SystemStreamLog();

    private String loadTemplate(String resourceName) throws IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        InputStream in = cl.getResourceAsStream(resourceName);
        OutputStream out = CommonUtil.newOutputStream();
        CommonUtil.writeInputToOutputStream(in, out);
        return out.toString();
    }

    private void batchAddComments(StringBuffer bash) throws IOException {
        String txt = loadTemplate("dcevm-install.txt");
        txt = txt.replace("@JAVA_HOME", System.getProperty("java.home"));
        bash.append(txt);
    }

    @Test
    public void testMessage() throws Exception {
        StringBuffer buf = new StringBuffer();
        batchAddComments(buf);
        assertTrue(buf.toString().contains(System.getProperty("java.home")));
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
    public void testJavaHomeDcevm() throws Exception {
        GitHubReleases installer = mock(GitHubReleases.class);
        when(installer.isHotswapInstalled()).thenReturn(false);
        boolean exists = installer.isHotswapInstalled();
        assertFalse(exists);
    }

    @Test
    public void testDcevmLatestRelease() throws Exception {
        GitHubReleases installer = MockHelper.mockGitHubReleases();
        GitHubUrl url = installer.getDcevmLatestReleaseVersion();
        assertNotNull(url.getTag());
        assertNotNull(url.getUrl());
        LOGGER.info("DcevmLatestRelease\t..tagName: " + url.getTag() + " \tbrowser_download_url: " + url.getUrl());
    }

    @Test
    public void testDcevmReleases() throws Exception {
        GitHubReleases installer = MockHelper.mockGitHubReleases();
        GitHubUrl latestVersion = installer.getDcevmLatestReleaseVersion();
        List<GitHubUrl> urls = installer.getDcevmReleaseList();
        if (!CommonUtil.isTravisCI()) assertEquals(5, urls.size());
        GitHubUrl matchUrl = installer.findMatchingDcevmVersion(urls);
        LOGGER.info("---o  + = latest-release");
        LOGGER.info("---o  * = best-match");
        urls.forEach(url -> {
            boolean latest = (url.equalTo(latestVersion));
            boolean match = (url.equalTo(matchUrl));
            String tail = String.format(" %s", (latest ? "(+)" : ""));
            tail += String.format(" %s", (match ? "(*)" : ""));
            LOGGER.info("DcevmReleases\t\t..tagName: " + url.getTag() + " \tbrowser_download_url: " + url.getUrl() + tail);
        });
    }

    @Test
    public void testHotswapLatestRelease() throws Exception {
        GitHubReleases installer = MockHelper.mockGitHubReleases();
        GitHubUrl url = installer.getHotswapLatestReleaseVersion();
        assertNotNull(url.getTag());
        assertNotNull(url.getUrl());
        LOGGER.info("HotswapLatestRelease\t..tagName: " + url.getTag() + " \t\t\tbrowser_download_url: " + url.getUrl());
    }

    @Test
    public void testHotswapReleases() throws Exception {
        GitHubReleases installer = MockHelper.mockGitHubReleases();
        GitHubUrl latestVersion = installer.getHotswapLatestReleaseVersion();
        List<GitHubUrl> urls = installer.getHotswapReleaseList();
        if (!CommonUtil.isTravisCI()) assertEquals(3, urls.size());
        GitHubUrl matchUrl = installer.findMatchingHotswapVersion(urls);
        urls.forEach(url -> {
            boolean latest = (url.equalTo(latestVersion));
            boolean match = (url.equalTo(matchUrl));
            String tail = String.format(" %s", (latest ? "(+)" : ""));
            tail += String.format(" %s", (match ? "(*)" : ""));
            LOGGER.info("HotswapReleases\t\t..tagName: " + url.getTag() + " \tbrowser_download_url: " + url.getUrl() + tail);
        });
    }

}


