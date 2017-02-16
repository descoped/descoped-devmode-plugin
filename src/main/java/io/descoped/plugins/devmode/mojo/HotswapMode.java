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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.Boolean.TRUE;

/**
 * Created by oranheim on 14/01/2017.
 */
public class HotswapMode {

    private final Log LOGGER = Logger.INSTANCE;

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
            LOGGER.info("Hotswap is found at: " + CommonUtil.getJavaJreHotswapLib());
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
                if (!helper.getMojo().getUseJarInstaller() && line.contains("@USE_JAR_INSTALLER"))
                    continue;
                else
                    line = line.replace("@USE_JAR_INSTALLER", "");
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
        int selectedOption = (defaultOption == -1 ? 0 : defaultOption);

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

    public static final String WIN_AMD64 = "windows_amd64_compiler2/product/jvm.dll";
    public static final String WIN_X86 = "windows_i486_compiler2/product/jvm.dll";
    public static final String LINUX_AMD64 = "linux_amd64_compiler2/product/libjvm.so";
    public static final String LINUX_X86 = "linux_i486_compiler2/product/libjvm.so";
    public static final String BSD_AMD64 = "bsd_amd64_compiler2/product/libjvm.dylib";
    public static final String JDK_TARGET = "lib/dcevm";

    public static String getHotswapLibFilename() {
        String lib = null;
        if (CommonUtil.isMacOS() && CommonUtil.is64bit()) {
            lib = HotswapMode.BSD_AMD64;
        } else if (CommonUtil.isLinux() && CommonUtil.is64bit()) {
            lib = HotswapMode.LINUX_AMD64;
        } else if (CommonUtil.isLinux() && !CommonUtil.is64bit()) {
            lib = HotswapMode.LINUX_X86;
        } else if (CommonUtil.isWindows() && CommonUtil.is64bit()) {
            lib = HotswapMode.WIN_AMD64;
        } else if (CommonUtil.isWindows() && !CommonUtil.is64bit()) {
            lib = HotswapMode.WIN_X86;
        } else {
            throw new RuntimeException("Unable to resolve JVM Library for your system!");
        }
        return lib;

    }

    public String unzipHotswapJarToTargetDir(String hotswapJarTempFile) throws MojoExecutionException {
        String lib = getHotswapLibFilename();
        String targetFilename = null;
        try {
            ZipInputStream zis = new ZipInputStream(new FileInputStream(hotswapJarTempFile));
            try {
                ZipEntry ze = zis.getNextEntry();
                while (ze != null) {
                    String zipFileNamePath = ze.getName();
                    String zipFileName = Paths.get(zipFileNamePath).getFileName().toString();
                    targetFilename = helper.relativeOutputDirectory() + FileUtils.fileSeparator + JDK_TARGET + FileUtils.fileSeparator + zipFileName;
                    if (zipFileNamePath.equals(lib)) {
                        File targetFile = new File(targetFilename);
                        Path targetPath = Paths.get(targetFile.getAbsolutePath()).getParent();
                        FileUtils.createDirectories(targetPath);
                        FileOutputStream fos = new FileOutputStream(targetFile);
                        try {
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                fos.write(buffer, 0, len);
                            }
                        } finally {
                            fos.close();
                        }
                        break;
                    }
                    ze = zis.getNextEntry();
                }
            } finally {
                zis.closeEntry();
                zis.close();
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Something went wrong during unzip of " + targetFilename, e);
        }
        return targetFilename;
    }

    private void createHotswapBatchInstallScript(String installationFile) throws IOException {
        StringBuffer bash = new StringBuffer();
        {
            bash.append("#!/bin/sh\n\n");
            if (helper.getMojo().getUseJarInstaller()) {
                bash.append("cd ").append(CommonUtil.getJavaHome()).append("\n");
                bash.append("sudo -p ").append("\"Enter password to start Hotswap Installer:\"").append(" bash -c ").append("'");
                bash.append(CommonUtil.getJavaBin()).append(" ");
                bash.append("-jar ");
                bash.append(installationFile).append("'\n");
            } else {
                Path installFilePath = Paths.get(CommonUtil.getJavaJreHotswapLib()).toAbsolutePath();
                Path installPath = Paths.get(CommonUtil.getJavaJreHotswapLib()).getParent().toAbsolutePath();
                bash.append("sudo -p ").append("\"Enter password to start Hotswap Installer:\"").append(" mkdir -p ").append(installPath.toString()).append("\n");
                bash.append("sudo cp -f ").append(installationFile).append(" ").append(installFilePath.toString()).append("\n");
            }
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
            args.add("-Dorg.apache.deltaspike.ProjectStage=Development");
            args.add("-classpath");
            args.add(helper.getProjectClasspathJars());
            args.add(clazz);

            helper.exec(FileUtils.currentPath(), args, false, true, true);
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("Error starting Java process!", e);
        }
    }
}
