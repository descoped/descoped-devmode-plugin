package io.descoped.plugins.devmode.test;

import io.descoped.plugins.devmode.mojo.ContainerDevModeMojo;
import io.descoped.plugins.devmode.mojo.GitHubReleases;
import io.descoped.plugins.devmode.mojo.GitHubUrl;
import io.descoped.plugins.devmode.mojo.JavaVersion;
import io.descoped.plugins.devmode.util.CommonUtil;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by oranheim on 07/01/2017.
 */
public class DevmodeTest extends AbstractMojoTestCase {

    private static final Log LOGGER = new SystemStreamLog();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testMojoGoal() throws Exception {
        File testPom = new File(getBasedir(), "src/test/resources/test-pom.xml");
        ContainerDevModeMojo mojo = (ContainerDevModeMojo) lookupMojo("run", testPom);
        Map map = new HashMap();
        map.put("project", new MojoMavenProjectStub());
        mojo.setPluginContext(map);
        try {
            mojo.execute();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void testJavaVersion() throws Exception {
        LOGGER.info("Java version: " + JavaVersion.getVersion() + " ver: " +
                JavaVersion.getMajor() + " major: " + JavaVersion.getMinor()+ " minor: " + JavaVersion.getBuild() +
                " isJdk8: " + JavaVersion.isJdk8());

        LOGGER.info("project.build.sourceDirectory: " + System.getProperty("project.build.sourceDirectory"));
    }

    public void testDcevm() throws Exception {
        LOGGER.info("isMojoRunningInTestingHarness: " + CommonUtil.isMojoRunningInTestingHarness());
        GitHubReleases gitHubReleases = new GitHubReleases(
                "https://github.com/dcevm/dcevm/releases/latest", "installer.jar",
                "https://github.com/dcevm/dcevm/releases", "full-");
        GitHubUrl latestVersion = gitHubReleases.getLatestVersionUrl();
        List<GitHubUrl> releaseUrlList = gitHubReleases.getReleaseUrlList();

        assertNotNull(latestVersion.getUrl());
        assertFalse(releaseUrlList.isEmpty());



        releaseUrlList.forEach(downloadUrl -> {
            String url = downloadUrl.getDecodedUrl();
            if (downloadUrl.equalTo(latestVersion)) {
                url += "(latest)";
            }
            LOGGER.info("Dvevm Release URL: " + url);
        });
    }
    public void _testHotswap() throws Exception {
        GitHubReleases gitHubReleases = new GitHubReleases(
                "https://github.com/HotswapProjects/HotswapAgent/releases/latest", "installer.jar",
                "https://github.com/HotswapProjects/HotswapAgent/releases", "");
        GitHubUrl latestVersion = gitHubReleases.getLatestVersionUrl();
        List<GitHubUrl> releaseUrlList = gitHubReleases.getReleaseUrlList();

        assertNotNull(latestVersion.getUrl());
        assertFalse(releaseUrlList.isEmpty());

        releaseUrlList.forEach(downloadUrl -> {
            String url = downloadUrl.getDecodedUrl();
            if (downloadUrl.equalTo(latestVersion)) {
                url += "(latest)";
            }
            LOGGER.info("Dvevm Release URL: " + url);
        });
    }
}
