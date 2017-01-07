package io.descoped.plugins.devmode.test;

import io.descoped.plugins.devmode.mojo.DescopedWebDeveloperModeMojo;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;

import java.io.File;

/**
 * Created by oranheim on 07/01/2017.
 */
public class DevmodeTest extends AbstractMojoTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testMojoGoal() throws Exception {
        File testPom = new File( getBasedir(), "src/test/resources/test-pom.xml" );
        DescopedWebDeveloperModeMojo mojo = (DescopedWebDeveloperModeMojo) lookupMojo( "run", testPom );
        mojo.execute();
    }
}
