package io.descoped.plugins.devmode.mojo;

import com.github.kevinsawicki.http.HttpRequest;
import io.descoped.plugins.devmode.util.CommonUtil;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by oranheim on 08/01/2017.
 */
public class GitHubReleases {

    private static final Log LOGGER = new SystemStreamLog();

    private final String latestVersionUrl;
    private final String releaseUrl;
    private final String latestLinkMatch;
    private final String releaseLinkMatch;
    private final GitHubUrl gitHubLatestVersionUrl;
    private final List<GitHubUrl> gitHubReleaseUrlList;

    public GitHubReleases(String latestVersionUrl, String latestLinkMatch, String releaseUrl, String releaseLinkMatch) throws MojoExecutionException {
        this.latestVersionUrl = latestVersionUrl;
        this.latestLinkMatch = latestLinkMatch;
        this.releaseUrl = releaseUrl;
        this.releaseLinkMatch = releaseLinkMatch;

        try {
            gitHubLatestVersionUrl = downloadLatestVersionURL();
            gitHubReleaseUrlList = downloadReleaseURLs();
        } catch (MojoExecutionException e) {
            e.printStackTrace();
            throw e;
        }
    }

    private List<String> grabHtmlLinks(String html, String hrefContainsToken) throws IOException {
        List<String> links = new ArrayList<>();

        String MATCHER = "href=\"(.*?)\"";
        Pattern p = Pattern.compile(MATCHER, Pattern.DOTALL);
        Pattern p2 = Pattern.compile("(\\d+([.]\\d+)+)");


        BufferedReader reader = new BufferedReader(new StringReader(html));
        String line;
        while ((line = reader.readLine()) != null) {
            Matcher m = p.matcher(line);
            if (m.find()) {
                if (m.groupCount() == 1) {
                    String match = m.group(1);
                    if (hrefContainsToken != null) {
//                        Matcher m2 = p2.matcher(match);
//                        if (m2.find()) {
//                            System.out.println("-------------------> " + m2.groupCount());
//                            for(int n = 0; n<=m2.groupCount(); n++) {
//                                String ss = m2.group(n);
//                                System.out.println("---------------------> " + ss);
//                            }
//                        }
                        if (match.contains(hrefContainsToken)) {
                            links.add(match);
                        }
                    } else {
                        links.add(match);
                    }
                }
            }
        }

        return links;
    }

    private GitHubUrl downloadLatestVersionURL() throws MojoExecutionException {
        try {
            HttpRequest req = HttpRequest.get(latestVersionUrl).followRedirects(false);
            OutputStream out = CommonUtil.newOutputStream();
            req.receive(out);
            List<String> links = grabHtmlLinks(out.toString(), null);
            if (!links.isEmpty()) {
                String downloadLink = links.get(0);
                LOGGER.info("downloadLink: " + downloadLink);
                req = HttpRequest.get(downloadLink);
                links = grabHtmlLinks(req.body(), latestLinkMatch);
                if (!links.isEmpty()) {
                    return new GitHubUrl("https://github.com" + links.get(0));
                }
            }
        } catch (IOException e) {
            new MojoExecutionException("Error resolving Download URL", e);
        }
        throw new MojoExecutionException("Unable to resolve Download URL");
    }

    private List<GitHubUrl> downloadReleaseURLs() throws MojoExecutionException {
        try {
            HttpRequest req = HttpRequest.get(releaseUrl);
            String body = req.body();

            List<GitHubUrl> _links = new ArrayList<>();
            List<String> links = grabHtmlLinks(body, latestLinkMatch);
            for (String link : links) {
                if (link.contains(releaseLinkMatch)) continue;
                _links.add(new GitHubUrl("https://github.com" + link));
            }
            return _links;
        } catch (IOException e) {
            new MojoExecutionException("Error resolving Download URL", e);
        }
        throw new MojoExecutionException("Unable to resolve Download URL");
    }

    public GitHubUrl getLatestVersionUrl() {
        return gitHubLatestVersionUrl;
    }

    public List<GitHubUrl> getReleaseUrlList() {
        return gitHubReleaseUrlList;
    }

    // do a latest version check -DcheckLatestVersion
    public boolean isHotswapperInstalled() {
        return false;
    }
}
