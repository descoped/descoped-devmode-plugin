package io.descoped.plugins.devmode.test;

import io.descoped.plugins.devmode.mojo.GitHubReleases;
import io.descoped.plugins.devmode.mojo.GitHubUrl;
import io.descoped.plugins.devmode.util.CommonUtil;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.SystemStreamLog;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by oranheim on 12/01/2017.
 */
public class MockHelper {

    private static final boolean USE_MOCK = true;

    public static GitHubReleases mockGitHubReleases() throws MojoExecutionException {
        if ((!CommonUtil.isTravisCI() && !USE_MOCK) || CommonUtil.isTravisCI()) return new GitHubReleases();

        new SystemStreamLog().info("Creation of MockGitHubReleases");

        GitHubReleases installer = mock(GitHubReleases.class);
        when(installer.getHotswapLatestReleaseVersion()).thenReturn(new GitHubUrl("light-jdk8u112+8", "https://github.com/dcevm/dcevm/releases/download/light-jdk8u112%2B8/DCEVM-light-8u112-installer.jar"));

        List<GitHubUrl> dcevmReleaseList = new ArrayList<>();
        dcevmReleaseList.add(new GitHubUrl("light-jdk8u112+8", "https://github.com/dcevm/dcevm/releases/download/light-jdk8u112%2B8/DCEVM-light-8u112-installer.jar"));
        dcevmReleaseList.add(new GitHubUrl("light-jdk8u112+7", "https://github.com/dcevm/dcevm/releases/download/light-jdk8u112%2B7/DCEVM-light-8u112-installer.jar"));
        dcevmReleaseList.add(new GitHubUrl("light-jdk8u112+6", "https://github.com/dcevm/dcevm/releases/download/light-jdk8u112%2B6/DCEVM-light-8u112-installer.jar"));
        dcevmReleaseList.add(new GitHubUrl("light-jdk8u92+1", "https://github.com/dcevm/dcevm/releases/download/light-jdk8u92%2B1/DCEVM-light-8u92-installer.jar"));
        dcevmReleaseList.add(new GitHubUrl("light-jdk7u79+3", "https://github.com/dcevm/dcevm/releases/download/light-jdk7u79%2B3/DCEVM-light-7u79-installer.jar"));
        when(installer.getHotswapReleaseList()).thenReturn(dcevmReleaseList);

        when(installer.getHotswapAgentLatestReleaseVersion()).thenReturn(new GitHubUrl("1.0", "https://github.com/HotswapProjects/HotswapAgent/releases/download/1.0/hotswap-agent-1.0.jar"));

        List<GitHubUrl> hotswapReleaseList = new ArrayList<>();
        hotswapReleaseList.add(new GitHubUrl("1.1.0-SNAPSHOT", "https://github.com/HotswapProjects/HotswapAgent/releases/download/1.1.0-SNAPSHOT/hotswap-agent-1.1.0-SNAPSHOT.jar"));
        hotswapReleaseList.add(new GitHubUrl("1.0.1-SNAPSHOT", "https://github.com/HotswapProjects/HotswapAgent/releases/download/1.0.1-SNAPSHOT/hotswap-agent-1.0.1-SNAPSHOT.jar"));
        hotswapReleaseList.add(new GitHubUrl("1.0", "https://github.com/HotswapProjects/HotswapAgent/releases/download/1.0/hotswap-agent-1.0.jar"));
        hotswapReleaseList.add(new GitHubUrl("0.3.0-SNAPSHOT@2016/02/15", "https://github.com/HotswapProjects/HotswapAgent/releases/download/0.3.0-SNAPSHOT%402016/02/15/hotswap-agent.jar"));
        when(installer.getHotswapAgentReleaseList()).thenReturn(hotswapReleaseList);

        when(installer.findMatchingHotswapVersion(installer.getHotswapReleaseList())).thenCallRealMethod();
        when(installer.findMatchingHotswapAgentVersion(installer.getHotswapAgentReleaseList())).thenCallRealMethod();

        return installer;
    }

}
