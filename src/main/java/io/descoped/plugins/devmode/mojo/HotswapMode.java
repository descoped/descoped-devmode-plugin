package io.descoped.plugins.devmode.mojo;

import com.github.kevinsawicki.http.HttpRequest;
import io.descoped.plugins.devmode.util.CommonUtil;
import io.descoped.plugins.devmode.util.FileUtils;
import io.descoped.plugins.devmode.util.JavaVersion;
import io.descoped.plugins.devmode.util.Logger;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static java.lang.Boolean.TRUE;

/**
 * Created by oranheim on 14/01/2017.
 */
public class HotswapMode {

    private static final Log LOGGER = Logger.INSTANCE;

    private final DevModeHelper helper;

    public HotswapMode(DevModeHelper helper) {
        this.helper = helper;
    }

    private GitHubReleases gitHubReleases() {
        return GitHubFactory.getInstance();
    }

    private boolean isPropertyHotswapUpdateSet() {
        return System.getProperties().containsKey("updateHotswap") && TRUE.equals(Boolean.valueOf(System.getProperty("updateHotswap")));
    }

    public boolean shouldPerformInstallation() {
        if (CommonUtil.isMojoRunningInTestingHarness()) {
            LOGGER.info("Skipping user input! Setting default values......");
            return false;
        }
        if (gitHubReleases().isHotswapInstalled() && !isPropertyHotswapUpdateSet()) {
            LOGGER.info("Hotswap is found at: " + CommonUtil.getJavaHotswapAgentLib());
            return false;
        }
        return true;
    }

    public void printInstallHelp() throws MojoExecutionException {
        try {
            String txt = helper.loadTemplate("hotswap-install-help.txt");
            txt = txt.replace("@JDK_VERSION", JavaVersion.getVersionInfo());
            BufferedReader reader = new BufferedReader(new StringReader(CommonUtil.trimRight(txt)));
            String line;
            System.out.println("  \\");
            while ((line = reader.readLine()) != null) {
                if (line.trim().equals("")) continue;
                System.out.println(line);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Error with template!", e);
        }
    }

    public GitHubUrl selectHotswapOption() throws MojoExecutionException {
        List<GitHubUrl> releaseList = gitHubReleases().getHotswapReleaseList();
        GitHubUrl latestVersion = gitHubReleases().getHotswapLatestReleaseVersion();
        GitHubUrl defaultOptionUrl = gitHubReleases().findMatchingHotswapVersion(releaseList);
        int defaultOption = releaseList.indexOf(defaultOptionUrl);
        int selectedOption = defaultOption;

        {
            LOGGER.info("Please select which version of DCEVM Hotswap you want to install:");
            System.out.println();
            System.out.println("* = recommended");
            System.out.println("+ = matching system jvm");
            System.out.println();
            System.out.println("Options:");
            System.out.println();
            for (int n = 0; n < releaseList.size(); n++) {
                GitHubUrl url = releaseList.get(n);
                boolean match = (gitHubReleases().isHotswapVersionMatchingJDK(url));
                boolean latest = (url.equalTo(latestVersion));
                String tail = (latest || match ? "(" : "");
                tail += (match ? "+" : "");
                tail += (latest ? "*" : "");
                tail += (latest || match ? ")" : "");
                System.out.println(String.format("(%s) - %s\t%s", n, url.getTag(), tail));
            }
            System.out.println();
        }

        {
            Scanner scanner = new Scanner(System.in);
            CommonUtil.resetSystemInScanner();
            System.out.print(String.format("Choose distribution [default=%s]: ", defaultOption));
            String hotswapOptionToken = scanner.nextLine();
            try {
                selectedOption = Integer.valueOf(hotswapOptionToken);
            } catch (NumberFormatException e) {
            }
            if (selectedOption < 0 || selectedOption >= releaseList.size()) {
                LOGGER.error("You have entered an invalid value! Exiting!");
                System.exit(-1);
            }
        }

        return releaseList.get(selectedOption);
    }

    public String downloadHotswap(GitHubUrl hotswapOption) throws MojoExecutionException {
        LOGGER.info("Download: " + hotswapOption.getUrl());
        HttpRequest req = HttpRequest.get(hotswapOption.getUrl());
        if (req.ok()) {
            try {
                long contentLength = Long.valueOf(req.header("Content-Length"));
                File tmp = File.createTempFile("hotswap-", "-installer.jar");
                Thread progressThread = CommonUtil.consoleProgressThread(tmp, contentLength);
                req.receive(tmp);
                CommonUtil.interruptProgress(progressThread);
                String tmpFile = tmp.getAbsoluteFile().toString();
                LOGGER.debug("TempFile: " + tmpFile);
                return tmpFile;
            } catch (IOException e) {
                throw new MojoExecutionException("Error downloading Hotswap installation file!", e);
            }
        }
        throw new MojoExecutionException("Error when installing Hotswap. Http error code: " + req.code());
    }

    private void createHotswapBatchInstallScript(String installationFile) throws IOException {
        StringBuffer bash = new StringBuffer();
        {
            bash.append("#!/bin/sh\n\n");
            bash.append("cd ").append(CommonUtil.getJavaJdkHome()).append("\n");
            bash.append("sudo -p ").append("\"Enter password to start Hotswap Installer:\"").append(" bash -c ").append("'");
            bash.append(CommonUtil.getJavaBin()).append(" ");
            bash.append("-jar ");
            bash.append(installationFile).append("'\n");
        }
        {
            File bashFile = new File(String.format("%s/%s/installJvmHotswap.sh", FileUtils.currentPath(), helper.relativeOutputDirectory()));
            FileUtils.writeContent(bashFile, bash.toString());
            FileUtils.chmodOwnerExec(bashFile);
        }
    }

    public void installHotswap(String installationFile) throws MojoExecutionException {
        try {
            LOGGER.info("Install DCEVM Hotswap: " + installationFile);

            List<String> args = new ArrayList<>();
            args.add("/bin/sh");
            args.add("-i");
            args.add(String.format("./%s/installJvmHotswap.sh", helper.relativeOutputDirectory()));

            createHotswapBatchInstallScript(installationFile);

            helper.exec(FileUtils.currentPath(), args, false, true, true);

            if (gitHubReleases().isHotswapInstalled()) {
                LOGGER.info("Installation complete successfully! Ready for HOTSWAP DevMode..");
            } else {
                LOGGER.error("Installation failed!");
            }
            System.exit(-1);
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("Error installing DCEVM Hotswap!", e);
        }
    }

    private GitHubUrl findLatestHotswapAgentVersion() throws MojoExecutionException {
        List<GitHubUrl> releaseUrlList = gitHubReleases().getHotswapAgentReleaseList();
        GitHubUrl latestVersion = gitHubReleases().findMatchingHotswapAgentVersion(releaseUrlList);
        return latestVersion;
    }

    private Path getHotswapAgentPath() throws IOException {
        Path writePath = Paths.get("/tmp/descoped");
        FileUtils.createDirectories(writePath);
        return writePath;
    }

    private File getHotswapAgentFile(GitHubUrl hotswapAgentUrl) throws IOException {
        Path writePath = getHotswapAgentPath();
        Path path = Paths.get(hotswapAgentUrl.getDecodedUrl());
        File agentFile = writePath.resolve(path.getFileName()).toFile();
        return agentFile;
    }

    private boolean isHotswapAgentUpToDate(GitHubUrl hotswapAgentUrl) throws IOException {
        return getHotswapAgentFile(hotswapAgentUrl).exists();
    }

    private String downloadHotswapAgent(GitHubUrl hotswapAgentUrl) throws IOException, MojoExecutionException {
        LOGGER.info("Download Hotswap Agent: " + hotswapAgentUrl.getUrl());
        HttpRequest req = HttpRequest.get(hotswapAgentUrl.getUrl());
        if (req.ok()) {
            File jarFileHandle = getHotswapAgentFile(hotswapAgentUrl);
            FileOutputStream jarFile = new FileOutputStream(jarFileHandle);
            Thread progressThread = CommonUtil.consoleProgressThread(jarFileHandle, Long.valueOf(req.header("Content-Length")));
            req.receive(jarFile);
            CommonUtil.interruptProgress(progressThread);
            LOGGER.info("Received file: " + jarFileHandle.getAbsoluteFile().toString());
            return jarFileHandle.getAbsoluteFile().toString();
        }
        throw new MojoExecutionException("Error when downloading Hotswap Agent. Http error code: " + req.code());
    }

    public void run(String clazz) throws MojoExecutionException {
        try {
            List<String> args = new ArrayList<>();
            args.add(CommonUtil.getJavaBin());
            {
                GitHubUrl latestVersion = findLatestHotswapAgentVersion();
                String localHotswapAgent;
                if (!isHotswapAgentUpToDate(latestVersion)) {
                    localHotswapAgent = downloadHotswapAgent(latestVersion);
                } else {
                    localHotswapAgent = getHotswapAgentFile(latestVersion).getAbsoluteFile().toString();
                    LOGGER.info("Hotswap Agent is found at: " + localHotswapAgent);
                }
                args.add("-XXaltjvm=dcevm");
                args.add("-javaagent:" + localHotswapAgent);
            }
            args.add("-classpath");
            args.add(helper.getCompilePlusRuntimeClasspathJars());
            args.add(clazz);

            helper.exec(FileUtils.currentPath(), args, false, true, true);
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("Error starting Java process!", e);
        }
    }
}
