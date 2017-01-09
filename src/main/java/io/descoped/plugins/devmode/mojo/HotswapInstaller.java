package io.descoped.plugins.devmode.mojo;

import org.apache.maven.plugin.MojoExecutionException;

import java.util.List;

/**
 * Created by oranheim on 08/01/2017.
 */
public class HotswapInstaller {

    private GitHubUrl dcevmLatestReleaseVersion;
    private List<GitHubUrl> dcevmReleaseList;
    private GitHubUrl hotswapLatestReleaseVersion;
    private List<GitHubUrl> hotswapReleaseList;

    public HotswapInstaller() throws MojoExecutionException {
        init();
    }

    public GitHubUrl getDcevmLatestReleaseVersion() {
        return dcevmLatestReleaseVersion;
    }

    public List<GitHubUrl> getDcevmReleaseList() {
        return dcevmReleaseList;
    }

    public GitHubUrl getHotswapLatestReleaseVersion() {
        return hotswapLatestReleaseVersion;
    }

    public List<GitHubUrl> getHotswapReleaseList() {
        return hotswapReleaseList;
    }

    private void init() throws MojoExecutionException {
//        findDcevmUrls();
//        findHotswapUrls();
    }

    public void findDcevmUrls() throws MojoExecutionException {
        GitHubReleases gitHubReleases = new GitHubReleases(
                "https://github.com/dcevm/dcevm/releases/latest", "installer.jar",
                "https://github.com/dcevm/dcevm/releases", "full-");
        dcevmLatestReleaseVersion = gitHubReleases.getLatestVersionUrl();
        dcevmReleaseList = gitHubReleases.getReleaseUrlList();
    }

    public void findHotswapUrls() throws MojoExecutionException {
        GitHubReleases gitHubReleases = new GitHubReleases(
                "https://github.com/HotswapProjects/HotswapAgent/releases/latest", ".jar",
                "https://github.com/HotswapProjects/HotswapAgent/releases", "sources");
        hotswapLatestReleaseVersion = gitHubReleases.getLatestVersionUrl();
        hotswapReleaseList = gitHubReleases.getReleaseUrlList();
    }
}