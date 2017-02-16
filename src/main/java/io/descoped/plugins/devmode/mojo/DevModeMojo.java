package io.descoped.plugins.devmode.mojo;

import io.descoped.plugins.devmode.util.CommonUtil;
import io.descoped.plugins.devmode.util.FileUtils;
import io.descoped.plugins.devmode.util.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import java.io.File;

/**
 * Descoped Web Developer Plugin enables real time editing of source file when working on your project sources.
 */
@Mojo(name = "run", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.TEST)
public class DevModeMojo extends AbstractMojo {

    /**
     * Valid values are: HOTSWAP, RELPROXY, NONE
     */
//    Parameter(property = "devMode", defaultValue = "HOTSWAP")
    private DevMode devMode = DevMode.HOTSWAP;

    /**
     * Work directory
     */
    @Parameter(property = "outputDirectory", defaultValue = "target/devmode")
    private File outputDirectory;

    /**
     * Web content location in src
     */
    @Parameter(property = "webContent", required = false, defaultValue = "")
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
    private HotswapMode hotswapMode;

    public DevModeMojo() {
        super();
        getLog().info(CommonUtil.DESCOPED_LOGO);
    }

    public DevMode getDevMode() {
        return devMode;
    }

     // Execute DCEVM Installer instead of DevMode unpacking method
    public Boolean getUseJarInstaller() {
        String useJarInstaller = System.getProperty("useJarInstaller");
        return  (useJarInstaller != null && "true".equals(useJarInstaller));
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
                if (getUseJarInstaller()) {
                    hotswapMode.installHotswap(installationFile);
                } else {
                    String tmpFile = FileUtils.currentPath().toString() + FileUtils.fileSeparator + hotswapMode.unzipHotswapJarToTargetDir(installationFile);
                    hotswapMode.installHotswap(tmpFile);
                }
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
        Logger.setLogger(getLog());
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