package io.descoped.plugins.devmode.test;

import io.descoped.plugins.devmode.mojo.DevModeMojo;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by oranheim on 07/01/2017.
 */
public class DevModeMojoTest extends AbstractMojoTestCase {

    private static final Log LOGGER = new SystemStreamLog();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testMojoGoal() throws Exception {
        try {
            File testPom = new File(getBasedir(), "src/test/resources/test-pom.xml");
            DevModeMojo mojo = (DevModeMojo) lookupMojo("run", testPom);
            mojo.setMockGitHubReleases(MockHelper.mockGitHubReleases());
            Map map = new HashMap();
            map.put("project", new MojoMavenProjectStub());
            mojo.setPluginContext(map);
            mojo.execute();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

}
