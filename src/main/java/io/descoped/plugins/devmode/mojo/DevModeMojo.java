package io.descoped.plugins.devmode.mojo;

import io.descoped.plugins.devmode.util.CommonUtil;
import io.descoped.plugins.devmode.util.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * Descoped Web Developer Plugin enables real time editing of source file when working on your project sources.
 */
@Mojo(name = "run", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class DevModeMojo extends AbstractMojo {

    private static Log LOGGER = Logger.INSTANCE;

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

    @Parameter(property = "devMode", defaultValue = "HOTSWAP")
    private DevMode devMode;

    /**
     * The maven project instance
     */
    @Component
    private MavenProject project;

    private DevModeHelper helper;
    private HotswapMode hotswapMode;

    public DevModeMojo() {
        super();
        Logger.setLogger(getLog());
        getLog().info(CommonUtil.DESCOPED_LOGO);
    }

    public File getOutputDirectory() {
        return outputDirectory;
    }

    public String getWebContent() {
        return webContent;
    }

    public String getMainClass() {
        return mainClass;
    }

    public DevMode getDevMode() {
        return devMode;
    }

    public MavenProject getProject() {
        return project;
    }

    public DevModeHelper getHelper() {
        return helper;
    }

    private void execHotswapMode() throws MojoExecutionException {
        hotswapMode = new HotswapMode(helper);

        if (hotswapMode.shouldPerformInstallation()) {
            hotswapMode.printInstallHelp();

            GitHubUrl hotswapOption = hotswapMode.selectHotswapOption();
            String installationFile = hotswapMode.downloadHotswap(hotswapOption);

            if (installationFile != null) {
                hotswapMode.installHotswap(installationFile);
            }
        }

        hotswapMode.run(mainClass);
    }

    private void execRelProxyMode() throws MojoExecutionException {
        throw new UnsupportedOperationException();
    }

    private void execNoneMode() throws MojoExecutionException {
        throw new UnsupportedOperationException();
    }

    public void execute() throws MojoExecutionException {
        helper = new DevModeHelper(this);
        helper.init();
        helper.validateOutputDirectory();
        helper.validateMainClass();

        if (DevMode.HOTSWAP.equals(devMode)) {
            execHotswapMode();

        } else if (DevMode.RELPROXY.equals(devMode)) {
            execRelProxyMode();

        } else if (DevMode.NONE.equals(devMode)) {
            execNoneMode();
        }
    }

}