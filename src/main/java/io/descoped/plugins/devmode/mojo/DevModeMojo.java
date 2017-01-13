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
        };

        String installationFile = validateDecevmInstallation();
        if (installationFile != null) {
            installDcevm(installationFile);
        }

        exec(mainClass);
    }

    private GitHubUrl selectDcevmOptions() throws MojoExecutionException {
        List<GitHubUrl> releaseList = gitHubReleases().getDcevmReleaseList();
        LOGGER.info("Please select which version of Dcevm you want to install:");
        for (int n = 0; n < releaseList.size(); n++) {
            System.out.println(String.format("(%s) - %s", n, releaseList.get(n).getTag()));
        }
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter: ");
        String dcevmOption = scanner.next();
        LOGGER.info("-----> You selected option: " + dcevmOption);
        return releaseList.get(Integer.valueOf(dcevmOption));
    }

    private String validateDecevmInstallation() throws MojoExecutionException {
        if (CommonUtil.isMojoRunningInTestingHarness()) {
            LOGGER.info("Skipping user input! Setting default values......");
            return null;
        }
        if (gitHubReleases().isHotswapInstalled() && !System.getProperties().containsKey("dcevm.forceUpdate")) {
            LOGGER.info("Hotwap Installed: " + gitHubReleases().isHotswapInstalled());
            return null;
        }

        GitHubUrl dcevmOption = selectDcevmOptions();
        LOGGER.info("Downloading " + dcevmOption.getUrl());

        HttpRequest req = HttpRequest.get(dcevmOption.getUrl());
        if (req.ok()) {
            try {
                File tmp = File.createTempFile("dcevm", ".jar");
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


    private void batchAddComments(StringBuffer bash) throws IOException {
        String txt = helper.loadTemplate("dcevm-install.txt");
        txt = txt.replace("@JAVA_HOME", System.getProperty("java.home"));
        BufferedReader reader = new BufferedReader(new StringReader(txt));
        String line;
        while((line = reader.readLine()) != null){
            bash.append("echo \"").append(line).append("\"\n");
        }
    }

    private StringBuffer batchFileBufferDcevm(String installationFile) throws IOException {
        StringBuffer bash = new StringBuffer();
        bash.append("#!/bin/sh\n\n");
        batchAddComments(bash);
        bash.append("cd ").append(System.getProperty("java.home")).append("\n");
        bash.append("sudo bash -c ").append("'");
        bash.append(helper.getJavaHomeExecutable()).append(" ");
        bash.append("-jar ");
        bash.append(installationFile).append("'\n");
        return bash;
    }

    private void installDcevm(String installationFile) throws MojoExecutionException {
        try {
            List<String> args = new ArrayList<>();
            args.add("/bin/sh");
            args.add("-i");
            args.add("./target/installDcev.sh");
            LOGGER.info("Install Dcevm: " + installationFile);

            StringBuffer bash = batchFileBufferDcevm(installationFile);
            File bashFile = new File(FileUtils.getCurrentPath().toString() + "/target/installDcev.sh");
            FileUtils.writeContent(bashFile, bash.toString());
            FileUtils.chmodOwnerExec(bashFile);

//            LOGGER.info("\n" + bash.toString());

            helper.exec(FileUtils.getCurrentPath().toString(), args, false, true, true);
            LOGGER.info("Installation is completed!");
            System.exit(-1);
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("Error installing Dcevm!", e);
        }
    }

    private void exec(String clazz) throws MojoExecutionException {
        try {
            List<String> args = new ArrayList<>();
            args.add(helper.getJavaHomeExecutable());
            {
                List<GitHubUrl> releaseUrlList = gitHubReleases().getHotswapReleaseList();
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

            helper.exec(FileUtils.getCurrentPath().toString(), args, false, true, true);
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("Error starting Java process!", e);
        }
    }

}