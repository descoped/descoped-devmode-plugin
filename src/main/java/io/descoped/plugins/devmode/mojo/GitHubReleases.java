package io.descoped.plugins.devmode.mojo;

import com.github.kevinsawicki.http.HttpRequest;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
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

    private static final Log LOGGER = new SystemStreamLog();
    private static final String DCEVM_LATEST_RELEASE = "https://api.github.com/repos/dcevm/dcevm/releases/latest";
    private static final String DCEVM_RELEASES_ = "https://api.github.com/repos/dcevm/dcevm/releases";
    private static final String HOTSWAP_LATEST_RELEASE = "https://api.github.com/repos/HotswapProjects/HotswapAgent/releases/latest";
    private static final String HOTSWAP_RELEASES = "https://api.github.com/repos/HotswapProjects/HotswapAgent/releases";

    public GitHubReleases() throws MojoExecutionException {
    }

    private GitHubUrl getDcevmReleaseUrl(JSONObject json, String tagNameFilter, String urlFilter) {
        String tagName = json.getString("tag_name");
        if (tagNameFilter != null && tagName.contains(tagNameFilter)) return null;
        JSONObject asset = (JSONObject) json.getJSONArray("assets").get(0);
        String browserUrl = asset.getString("browser_download_url");
        if (urlFilter != null && !browserUrl.contains(urlFilter)) return null;
        return new GitHubUrl(tagName, browserUrl);
    }

    private GitHubUrl getHotswapReleaseUrl(JSONObject json) {
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
        String javaHome = System.getProperty("java.home") + "/lib/dcevm/libjvm.dylib";
        File file = new File(javaHome);
        return file.exists();
    }

    public GitHubUrl getDcevmLatestReleaseVersion() throws MojoExecutionException {
        HttpRequest req = HttpRequest.get(DCEVM_LATEST_RELEASE);
        if (req.ok()) {
            String body = req.body();
            JSONObject json = new JSONObject(body);
            GitHubUrl url = getDcevmReleaseUrl(json, null, null);
            return url;
        } else {
            throw new MojoExecutionException("Error fetching data: " + req.code());
        }
    }

    public List<GitHubUrl> getDcevmReleaseList() throws MojoExecutionException {
        List<GitHubUrl> result = new ArrayList<>();
        HttpRequest req = HttpRequest.get(DCEVM_RELEASES_);
        if (req.ok()) {
            String body = req.body();
            JSONArray array = new JSONArray(body);
            for (int i = 0; i < array.length(); i++) {
                JSONObject json = (JSONObject) array.get(i);
                GitHubUrl url = getDcevmReleaseUrl(json, "full-", "-installer.jar");
                if (url == null) continue;
                result.add(url);
            }
        } else {
            throw new MojoExecutionException("Error fetching data: " + req.code());
        }
        return result;
    }

    public GitHubUrl getHotswapLatestReleaseVersion() throws MojoExecutionException {
        HttpRequest req = HttpRequest.get(HOTSWAP_LATEST_RELEASE);
        String body = req.body();
        if (req.ok()) {
            JSONObject json = new JSONObject(body);
            GitHubUrl url = getHotswapReleaseUrl(json);
            return url;
        } else {
            throw new MojoExecutionException("Error fetching data: " + req.code());
        }
    }

    public List<GitHubUrl> getHotswapReleaseList() throws MojoExecutionException {
        List<GitHubUrl> result = new ArrayList<>();
        HttpRequest req = HttpRequest.get(HOTSWAP_RELEASES);
        if (req.ok()) {
            String body = req.body();
            JSONArray array = new JSONArray(body);
            for (int i = 0; i < array.length(); i++) {
                JSONObject json = (JSONObject) array.get(i);
                GitHubUrl url = getHotswapReleaseUrl(json);
                if (url == null) continue;
                result.add(url);
            }
        } else {
            throw new MojoExecutionException("Error fetching data: " + req.code());
        }
        return result;
    }

}
