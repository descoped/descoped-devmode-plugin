package io.descoped.plugins.devmode.test;

import io.descoped.plugins.devmode.mojo.GitHubReleases;
import io.descoped.plugins.devmode.mojo.GitHubUrl;
import io.descoped.plugins.devmode.util.CommonUtil;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

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
        LOGGER.info(buf.toString());
    }

    @Test
    public void testDcevmLatestRelease() throws Exception {
        GitHubReleases installer = new GitHubReleases();
        GitHubUrl url = installer.getDcevmLatestReleaseVersion();
        LOGGER.info("DcevmLatestRelease\t..tagName: " + url.getTag() + " \tbrowser_download_url: " + url.getUrl());
    }

    @Test
    public void testDcevmReleases() throws Exception {
        GitHubReleases installer = new GitHubReleases();
        List<GitHubUrl> urls = installer.getDcevmReleaseList();
        urls.forEach(url -> {
            LOGGER.info("DcevmReleases\t\t..tagName: " + url.getTag() + " \tbrowser_download_url: " + url.getUrl());
        });
    }

    @Test
    public void testHotswapLatestRelease() throws Exception {
        GitHubReleases installer = new GitHubReleases();
        GitHubUrl url = installer.getHotswapLatestReleaseVersion();
        LOGGER.info("HotswapLatestRelease\t..tagName: " + url.getTag() + " \t\t\tbrowser_download_url: " + url.getUrl());
    }

    @Test
    public void testHotswapReleases() throws Exception {
        GitHubReleases installer = new GitHubReleases();
        List<GitHubUrl> urls = installer.getHotswapReleaseList();
        urls.forEach(url -> {
            LOGGER.info("HotswapReleases\t\t..tagName: " + url.getTag() + " \tbrowser_download_url: " + url.getUrl());
        });
    }

}


