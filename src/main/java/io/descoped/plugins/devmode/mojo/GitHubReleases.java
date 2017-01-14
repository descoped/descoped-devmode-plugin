package io.descoped.plugins.devmode.mojo;

import com.github.kevinsawicki.http.HttpRequest;
import io.descoped.plugins.devmode.util.CommonUtil;
import io.descoped.plugins.devmode.util.JavaVersion;
import io.descoped.plugins.devmode.util.Logger;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by oranheim on 08/01/2017.
 */
public class GitHubReleases {

    private static final Log LOGGER = Logger.INSTANCE;
    private static final String HOTSWAP_LATEST_RELEASE = "https://api.github.com/repos/dcevm/dcevm/releases/latest";
    private static final String HOTSWAP_RELEASES = "https://api.github.com/repos/dcevm/dcevm/releases";
    private static final String HOTSWAP_AGENT_LATEST_RELEASE = "https://api.github.com/repos/HotswapProjects/HotswapAgent/releases/latest";
    private static final String HOTSWAP_AGENT_RELEASES = "https://api.github.com/repos/HotswapProjects/HotswapAgent/releases";

    private GitHubUrl hotswapLatestReleaseUrl;
    private List<GitHubUrl> hotswapReleaseUrls;
    private GitHubUrl hotswapAgentLatestReleaseUrl;
    private List<GitHubUrl> hotswapAgentReleaseUrls;

    public GitHubReleases() {
    }

    private GitHubUrl getHotswapReleaseUrl(JSONObject json, String tagNameFilter, String urlFilter) {
        String tagName = json.getString("tag_name");
        if (tagNameFilter != null && tagName.contains(tagNameFilter)) return null;
        JSONObject asset = (JSONObject) json.getJSONArray("assets").get(0);
        String browserUrl = asset.getString("browser_download_url");
        if (urlFilter != null && !browserUrl.contains(urlFilter)) return null;
        return new GitHubUrl(tagName, browserUrl);
    }

    private GitHubUrl getHotswapAgentReleaseUrl(JSONObject json) {
        String tagName = json.getString("tag_name");
        String browserUrl = null;
        Iterator<Object> assets = json.getJSONArray("assets").iterator();
        while (assets.hasNext()) {
            JSONObject asset = (JSONObject) assets.next();
            String dlurl = asset.getString("browser_download_url");
            if (dlurl.contains("-sources")) continue;
            if (dlurl.contains("hotswap-agent") && dlurl.contains(".jar")) {
                browserUrl = asset.getString("browser_download_url");
            }
        }
        return (browserUrl == null ? null : new GitHubUrl(tagName, browserUrl));
    }

    // do a latest version check -DcheckLatestVersion
    public boolean isHotswapInstalled() {
        File file = new File(CommonUtil.getJavaHotswapAgentLib());
        return file.exists();
    }

    public GitHubUrl getHotswapLatestReleaseVersion() throws MojoExecutionException {
        if (hotswapLatestReleaseUrl != null) return hotswapLatestReleaseUrl;
        HttpRequest req = HttpRequest.get(HOTSWAP_LATEST_RELEASE);
        if (req.ok()) {
            String body = req.body();
            JSONObject json = new JSONObject(body);
            return hotswapLatestReleaseUrl = getHotswapReleaseUrl(json, null, null);
        } else {
            throw new MojoExecutionException("Error fetching data due to http-error-code: " + req.code());
        }
    }

    public List<GitHubUrl> getHotswapReleaseList() throws MojoExecutionException {
        if (hotswapReleaseUrls != null) return hotswapReleaseUrls;
        List<GitHubUrl> result = new ArrayList<>();
        HttpRequest req = HttpRequest.get(HOTSWAP_RELEASES);
        if (req.ok()) {
            String body = req.body();
            JSONArray array = new JSONArray(body);
            for (int i = 0; i < array.length(); i++) {
                JSONObject json = (JSONObject) array.get(i);
                GitHubUrl url = getHotswapReleaseUrl(json, "full-", "-installer.jar");
                if (url == null) continue;
                result.add(url);
            }
            result.sort((o1, o2) -> {
                String tag1 = o1.getTag();
                String tag2 = o2.getTag();
                String major1 = tag1.substring(tag1.indexOf("jdk")+3, tag1.indexOf("u"));
                String major2 = tag2.substring(tag2.indexOf("jdk")+3, tag2.indexOf("u"));
                String minor1 = tag1.substring(tag1.indexOf("u")+1, tag1.indexOf("+"));
                String minor2 = tag2.substring(tag2.indexOf("u")+1, tag2.indexOf("+"));
                minor1 = (minor1.length() == 2 ? "0"+minor1 : minor1);
                minor2 = (minor2.length() == 2 ? "0"+minor2 : minor2);
                minor1 = major1 + minor1;
                minor2 = major2 + minor2;
                return minor2.compareTo(minor1);
            });
        } else {
            throw new MojoExecutionException("Error fetching data due to http-error-code: " + req.code());
        }
        return hotswapReleaseUrls = result;
    }

    public GitHubUrl getHotswapAgentLatestReleaseVersion() throws MojoExecutionException {
        if (hotswapAgentLatestReleaseUrl != null) return hotswapAgentLatestReleaseUrl;
        HttpRequest req = HttpRequest.get(HOTSWAP_AGENT_LATEST_RELEASE);
        String body = req.body();
        if (req.ok()) {
            JSONObject json = new JSONObject(body);
            return hotswapAgentLatestReleaseUrl = getHotswapAgentReleaseUrl(json);
        } else {
            throw new MojoExecutionException("Error fetching data due to http-error-code: " + req.code());
        }
    }

    public List<GitHubUrl> getHotswapAgentReleaseList() throws MojoExecutionException {
        if (hotswapAgentReleaseUrls != null) return hotswapAgentReleaseUrls;
        List<GitHubUrl> result = new ArrayList<>();
        HttpRequest req = HttpRequest.get(HOTSWAP_AGENT_RELEASES);
        if (req.ok()) {
            String body = req.body();
            JSONArray array = new JSONArray(body);
            for (int i = 0; i < array.length(); i++) {
                JSONObject json = (JSONObject) array.get(i);
                GitHubUrl url = getHotswapAgentReleaseUrl(json);
                if (url == null) continue;
                result.add(url);
            }
        } else {
            throw new MojoExecutionException("Error fetching data due to http-error-code: " + req.code());
        }
        return hotswapAgentReleaseUrls = result;
    }

    public GitHubUrl findMatchingHotswapVersion(List<GitHubUrl> releases) {
        List<GitHubUrl> options = new ArrayList<>();
        releases.forEach(url -> {
            if (isHotswapVersionMatchingJDK(url)) {
                options.add(url);
            }
        });
        return (options.isEmpty() ? null : options.get(0));
    }

    public boolean isHotswapVersionMatchingJDK(GitHubUrl compareWith) {
        String spec = String.format("light-jdk%su%s", (JavaVersion.isJdk8() ? "8" : "7"), JavaVersion.getMinor());
        return compareWith.getTag().startsWith(spec);
    }

    public GitHubUrl findMatchingHotswapAgentVersion(List<GitHubUrl> urls) throws MojoExecutionException {
        return (urls == null || urls.isEmpty() ? null : urls.get(0));
    }
}
