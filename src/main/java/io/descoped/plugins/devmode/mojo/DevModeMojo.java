package io.descoped.plugins.devmode.mojo;

import com.github.kevinsawicki.http.HttpRequest;
import io.descoped.plugins.devmode.util.CommonUtil;
import io.descoped.plugins.devmode.util.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Descoped Web Developer Plugin enables real time editing of source file when working on your project sources.
 * <p>
 * todo:
 * - get classpath
 * - remove compile classes path from classpath
 * - add src directory to class path
 * - exec descoped-container with custom classpath
 */
@Mojo(name = "run", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class DevModeMojo extends AbstractMojo {

    private static Log LOGGER;

    /**
     * Output directory location
     */
    @Parameter(property = "outputDirectory", defaultValue = "target/devmode")
    private File outputDirectory;

    /**
     * Web content location in src
     */
    @Parameter(property = "webContent", defaultValue = "src/main/resources/")
    private String webContent;

    /**
     * Main-class to execute
     */
    @Parameter(property = "mainClass", defaultValue = "io.descoped.container.Main")
    private String mainClass;

    /**
     * The maven project instance
     */
    @Component
    private MavenProject project;

    private DevModeHelper helper;

    public DevModeMojo() {
        super();
        LOGGER = getLog();
    }

    private GitHubReleases gitHubReleases() {
        return GitHubFactory.getInstance();
    }

    public void execute() throws MojoExecutionException {
        helper = new DevModeHelper(project, outputDirectory, webContent, mainClass);
        helper.init();
        helper.validateOutputDirectory();

        if (!helper.findClass(mainClass)) {
            throw new MojoExecutionException("Error locating class: " + mainClass + "\nClass-path: " + System.getProperty("java.class.path"));
        }

        String installationFile = validateHotswapInstallation();
        if (installationFile != null) {
            installHotswap(installationFile);
        }

        exec(mainClass);
    }

    private GitHubUrl selectHotswapOption() throws MojoExecutionException {
        List<GitHubUrl> releaseList = gitHubReleases().getHotswapReleaseList();
        LOGGER.info("Please select which version of DCEVM Hotswap you want to install:");
        for (int n = 0; n < releaseList.size(); n++) {
            System.out.println(String.format("(%s) - %s", n, releaseList.get(n).getTag()));
        }
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter: ");
        String hotswapOption = scanner.next();
        LOGGER.info("-----> You selected option: " + hotswapOption);
        return releaseList.get(Integer.valueOf(hotswapOption));
    }

    private String validateHotswapInstallation() throws MojoExecutionException {
        if (CommonUtil.isMojoRunningInTestingHarness()) {
            LOGGER.info("Skipping user input! Setting default values......");
            return null;
        }
        if (gitHubReleases().isHotswapInstalled() && !System.getProperties().containsKey("hotswapUpdate")) {
            LOGGER.info("Hotswap Installed: " + gitHubReleases().isHotswapInstalled());
            return null;
        }

        GitHubUrl hotswapOption = selectHotswapOption();
        LOGGER.info("Downloading " + hotswapOption.getUrl());

        HttpRequest req = HttpRequest.get(hotswapOption.getUrl());
        if (req.ok()) {
            try {
                File tmp = File.createTempFile("hotswap", ".jar");
                req.receive(tmp);
                String tmpFile = tmp.getAbsoluteFile().toString();
                LOGGER.info("TempFile: " + tmpFile);
                return tmpFile;
            } catch (IOException e) {
                throw new MojoExecutionException("Error downloading Decevm installation file!", e);
            }
        }
        return null;
    }


    private void createHotswapBatchInstallScript(String installationFile) throws IOException {
        String txt = helper.loadTemplate("hotswap-install-help.txt");
        txt = txt.replace("@JAVA_HOME", System.getProperty("java.home"));

        BufferedReader reader = new BufferedReader(new StringReader(CommonUtil.trimRight(txt)));
        StringBuffer bash = new StringBuffer();
        {
            bash.append("#!/bin/sh\n\n");
            bash.append("echo \"\"\n");
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().equals("")) continue;
                bash.append("echo \"").append(line).append("\"\n");
            }
            bash.append("echo \"\"\n");
            bash.append("cd ").append(System.getProperty("java.home").replace("/jre", "/")).append("\n");
            bash.append("sudo bash -c ").append("'");
            bash.append(helper.getJavaHomeExecutable()).append(" ");
            bash.append("-jar ");
            bash.append(installationFile).append("'\n");
        }

        File bashFile = new File(String.format("%s/%s/installJvmHotswap.sh", FileUtils.currentPath(), helper.relativeOutputDirectory()));
        FileUtils.writeContent(bashFile, bash.toString());
        FileUtils.chmodOwnerExec(bashFile);
    }

    private void installHotswap(String installationFile) throws MojoExecutionException {
        try {
            List<String> args = new ArrayList<>();
            args.add("/bin/sh");
            args.add("-i");
            args.add(String.format("./%s/installJvmHotswap.sh", helper.relativeOutputDirectory()));
            LOGGER.info("Install DCEVM Hotswap: " + installationFile);

            createHotswapBatchInstallScript(installationFile);

//            LOGGER.info("\n" + bash.toString());

            helper.exec(FileUtils.currentPath(), args, false, true, true);
            LOGGER.info("Installation is completed!");
            System.exit(-1);
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("Error installing DCEVM Hotswap!", e);
        }
    }

    private void exec(String clazz) throws MojoExecutionException {
        try {
            List<String> args = new ArrayList<>();
            args.add(helper.getJavaHomeExecutable());
            {
                List<GitHubUrl> releaseUrlList = gitHubReleases().getHotswapAgentReleaseList();
                GitHubUrl latestVersion = releaseUrlList.get(0);
                String url = latestVersion.getDecodedUrl();
                Path path = Paths.get(url);
                LOGGER.info("=========> releaseHotswapUrls: " + path.getFileName());

                Path writePath = Paths.get("/tmp/descoped");
                FileUtils.createDirectories(writePath);

                HttpRequest req = HttpRequest.get(latestVersion.getUrl());
                if (req.ok()) {
                    File jarFileHandle = writePath.resolve(path.getFileName()).toFile();
                    FileOutputStream jarFile = new FileOutputStream(jarFileHandle);
                    req.receive(jarFile);
                    LOGGER.info("Received file: " + jarFileHandle.getAbsoluteFile().toString());
                    // add file
                    args.add("-XXaltjvm=dcevm");
                    args.add("-javaagent:" + jarFileHandle.getAbsoluteFile().toString());
                }

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