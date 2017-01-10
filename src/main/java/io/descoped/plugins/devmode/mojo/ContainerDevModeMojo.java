package io.descoped.plugins.devmode.mojo;

import com.github.kevinsawicki.http.HttpRequest;
import io.descoped.plugins.devmode.util.CommonUtil;
import io.descoped.plugins.devmode.util.FileUtils;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
public class ContainerDevModeMojo extends AbstractMojo {

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

    private static synchronized long getPidOfProcess(Process p) {
        long pid = -1;

        try {
            if (p.getClass().getName().equals("java.lang.UNIXProcess")) {
                Field f = p.getClass().getDeclaredField("pid");
                f.setAccessible(true);
                pid = f.getLong(p);
                f.setAccessible(false);
            }
        } catch (Exception e) {
            pid = -1;
        }
        return pid;
    }

    private void checkLogger() {
        Logger.setLOG(getLog());
        LOGGER = Logger.LOG;
        CommonUtil.printEnvVars();
    }

    private String getCompilePlusRuntimeClasspathJars() throws MojoExecutionException {
        try {
            StringBuffer buf = new StringBuffer();

            String currentPath = FileUtils.getCurrentPath().toString();
            buf.append(currentPath).append("/").append(webContent).append(":");
            buf.append(currentPath).append("/").append("target/classes").append(":");
            if (CommonUtil.isMojoRunningInTestingHarness() || CommonUtil.isMojoRunningStandalone(project)) {
                buf.append(currentPath).append("/").append("target/test-classes").append(":");
            }

            List<String> classpathElements = project.getRuntimeClasspathElements();
            if (classpathElements != null) {
                for (String e : classpathElements) {
                    if (e.endsWith(".jar")) {
                        buf.append(e);
                        if (classpathElements.indexOf(e) < classpathElements.size() - 1) {
                            buf.append(":");
                        }
                    }
                }
            }
            return buf.toString();
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Error resolving classpath deps", e);
        }
    }

    public void execute() throws MojoExecutionException {
        checkLogger();

//        try {
        LOGGER.info("getBasedir: " + project.getBasedir());
//            LOGGER.info("getCompileClasspathElements: " + CommonUtil.printList(project.getCompileClasspathElements()));
        LOGGER.info("getDependencies: " + CommonUtil.printList(project.getDependencies()));

//        } catch (DependencyResolutionRequiredException e) {
//            throw new MojoExecutionException("", e);
//        }

        /*
            Build Classpath:
            1) src/main/resources/
            2) target/classes + test-classes
            3) hotswap-jar
            4) jar-files (compile classes with match for .jar)

            Jvm home: make a find . > jdk-file-snapshot.txt (before and after installation to see what has been changed)
            Java exec: install Dcevm and check that it is installed
            Java exec: add hotswap args before start
         */

        /*
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your name: ");
        String username = scanner.next();
        LOGGER.info("Username: " + username);
        */


        LOGGER.info("outputDirectory: " + outputDirectory);
        LOGGER.info("webContent: " + webContent);
//        LOGGER.info("classpath: " + getCompilePlusRuntimeClasspathJars());
        LOGGER.info("mainClass: " + mainClass);

        Map<Object, Object> map = getPluginContext();
        if (map != null) {
            for (Map.Entry<Object, Object> e : map.entrySet()) {
                LOGGER.info("---> key: " + e.getKey() + " => " + e.getValue());
            }
        }

        validateOutputDirectory();

        validateMainClass(mainClass);

        String installationFile = validateDecevmInstallation();
        if (installationFile != null) {
            installDcevm(installationFile);
        }
        LOGGER.info("------------------------->>");

        exec(mainClass);
    }

    private GitHubUrl selectDcevmOptions() throws MojoExecutionException {
        HotswapInstaller installer = new HotswapInstaller();
        installer.findDcevmUrls();
        List<GitHubUrl> releaseList = installer.getDcevmReleaseList();
        LOGGER.info("Please select which version of Dcevm you want to install:");
        for (int n = 0; n < releaseList.size(); n++) {
            System.out.println(String.format("(%s) - %s", n, releaseList.get(n).getDecodedUrl()));
        }
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter: ");
        String dcevmOption = scanner.next();
        LOGGER.info("-----> You selected option: " + dcevmOption);
        return releaseList.get(Integer.valueOf(dcevmOption));
    }

    private String validateDecevmInstallation() throws MojoExecutionException {
        if (!CommonUtil.isMojoRunningInTestingHarness()) {
            HotswapInstaller installer = new HotswapInstaller();
            if (installer.isHotswapInstalled() && !System.getProperties().containsKey("dcevm.forceUpdate")) {
                LOGGER.info("Hotwap Installed: " + installer.isHotswapInstalled());
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

        } else {
            LOGGER.info("Skipping user input! Setting default values......");
            return null;
        }
        return null;
    }

    private void batchAddComments(StringBuffer bash) {
        bash.append("echo \"\"\n");
        bash.append("echo \"------------------- IMPORTANT NOTICE --------------------").append("\"\n");
        bash.append("echo \"Make sure you close all JVM Processes before you proceed!").append("\"\n");
        bash.append("echo \"---------------------------------------------------------").append("\"\n");
        bash.append("echo \"\"\n");
        bash.append("echo \"* JavaHome: ").append(System.getProperty("java.home")).append("\"\n");
        bash.append("echo \"* Install DCEVM as AltJVM").append("\"\n");
        bash.append("echo \"* Please read the DCEVM documentation at https://github.com/dcevm/dcevm/blob/master/README.md").append("\"\n\n");
        bash.append("echo \"\"\n");
    }

    private StringBuffer batchFileBufferDcevm(String installationFile) {
        StringBuffer bash = new StringBuffer();
        bash.append("#!/bin/sh\n\n");
        batchAddComments(bash);
        bash.append("cd ").append(System.getProperty("java.home")).append("\n");
        bash.append("sudo bash -c ").append("'");
        bash.append(getJavaHomeExecutable()).append(" ");
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

            LOGGER.info("\n" + bash.toString());

            exec(FileUtils.getCurrentPath().toString(), args, false, true, true);
            LOGGER.info("Installation is completed!");
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("Error installing Dcevm!", e);
        }
    }

    private String getJavaHomeExecutable() {
        String separator = System.getProperty("file.separator");
        String path = System.getProperty("java.home") + separator + "bin" + separator + "java";
        return path;
    }

    private void exec(String execDirectory, List<String> args, boolean sudo, boolean waitFor, boolean printCommand) throws IOException, InterruptedException {
//        String path = getJavaHomeExecutable();
//
//        args.add(0, path);
//        if (sudo) {
//            args.add(0, "-c");
//            args.add(0, "bash");
//            args.add(0, "sudo");
//        }

        ProcessBuilder processBuilder = new ProcessBuilder(args);
        if (printCommand) {
            StringBuffer cmd = new StringBuffer();
            for (String i : processBuilder.command()) {
                cmd.append(i + " ");
            }
            LOGGER.info("Command:\n" + cmd);
        }
        processBuilder.directory(Paths.get(execDirectory).toFile());
        processBuilder.redirectInput(ProcessBuilder.Redirect.INHERIT);
        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT);
        processBuilder.inheritIO();

        LOGGER.info("Starting process in DevMode..");
        Process process = processBuilder.start();
        LOGGER.info("Process PID: " + getPidOfProcess(process));
        if (waitFor) {
            process.waitFor();
            LOGGER.info("Process exited with code: " + process.exitValue());
        } else {
            System.exit(-1);
        }
        LOGGER.info("------------------------->");
    }

    private void exec(String clazz) throws MojoExecutionException {
        try {
            List<String> args = new ArrayList<>();
            args.add(getJavaHomeExecutable());
            {
                HotswapInstaller installer = new HotswapInstaller();
                installer.findHotswapUrls();
                List<GitHubUrl> releaseUrlList = installer.getHotswapReleaseList();
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
            args.add(getCompilePlusRuntimeClasspathJars());
            args.add(clazz);

            exec(FileUtils.getCurrentPath().toString(), args, false, true, true);
        } catch (IOException | InterruptedException e) {
            throw new MojoExecutionException("Error starting Java process!", e);
        }
    }

    private boolean validateMainClass(String className) throws MojoExecutionException {
        try {
            List<String> classpathElements = project.getRuntimeClasspathElements();

            // only for mojo testing
            if (CommonUtil.isMojoRunningInTestingHarness() || CommonUtil.isMojoRunningStandalone(project)) {
                if (classpathElements == null) {
                    classpathElements = new ArrayList<>();
                    classpathElements.add(0, FileUtils.getCurrentPath().toString() + "/target/classes");
                }
                classpathElements.add(0, FileUtils.getCurrentPath().toString() + "/target/test-classes");
            }

            // only for mojo testing
            if (classpathElements == null || classpathElements.isEmpty()) {
                try {
                    Class.forName(className);
                    return true;
                } catch (ClassNotFoundException e) {
                    return false;
                }
            }

            // find class on runtime classpath
            boolean ok = false;
            for (String classpathElement : classpathElements) {
                try {
                    URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{new URL("file:" + classpathElement)});
                    classLoader.loadClass(className);
                    classLoader = null;
                    ok = true;
                    break;
                } catch (ClassNotFoundException e) {
                }
            }
            return ok;
        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Error locating class: " + className + "\nClass-path: " + System.getProperty("java.class.path"), e);
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Error locating class: " + className + "\nClass-path: " + System.getProperty("java.class.path"), e);
        }
    }

    private void validateOutputDirectory() throws MojoExecutionException {
        if (!outputDirectory.exists()) {
            Path outputPath = Paths.get(outputDirectory.getAbsolutePath());
            try {
                FileUtils.createDirectories(outputPath);
            } catch (IOException e) {
                throw new MojoExecutionException("Unable to create directory: " + outputDirectory, e);
            }

        }
    }
}
