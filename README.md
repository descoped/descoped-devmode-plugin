# Descoped Devmode Plugin

[![Build Status](https://travis-ci.org/descoped/descoped-devmode-plugin.svg?branch=master)](https://travis-ci.org/descoped/descoped-devmode-plugin)

## About

The Devmode Maven Plugin enables redefinition of loaded classes at runtime. Its purpose is to allow the Descoped Container to be running whilst the developer can write code and (if possible) debug in real time. This is acheived by utilizing the *Dynamic Code Evolution Virtual Machine (DCEVM)* and the *Hotswap Agent*.

When invoking the devmode plugin at compile time it will automatically fork and configure a Java process that executes the `mainClass` defined.

This is useful when working with web development when aiming for rapid sketching and development.

## Maven

Declare maven profile:

```xml
<profile>
    <id>dev</id>
    <build>
        <plugins>
            <plugin>
                <groupId>io.descoped.mojo</groupId>
                <artifactId>web-devmode-plugin</artifactId>
                <version>1.0.0-alpha5</version>
                <executions>
                    <execution>
                        <phase>compile</phase>
                        <goals>
                            <goal>run</goal>
                        </goals>
                    </execution>
                </executions>
                <configuration>
                    ...
                </configuration>
            </plugin>
        </plugins>
    </build>
</profile>
```

Configuration options:

 Option          | Default                    | Description                                     |
-----------------|----------------------------|-------------------------------------------------|
 outputDirectory | target/devmode             | Work directory                                  |
 webContent      | N/A                        | Custom Web resources directory                  |
 mainClass       | io.descoped.container.Main | Main class to be executed                       |


### Maven environment variables

 Option          | Description                                            |
-----------------|--------------------------------------------------------|
 updateHotswap   | Force install process for the DCEVM Hotswap JVM        |
 useJarInstaller | Use DCEVM Jar Installer instead of plugin installation |
 addTestClasses  | Add `target/test-classes` to class path                |

Maven options are added as follows: `mvn -D<option>`


## Use

Run in Devmode:

`mvn clean compile -Pdev -DskipTests`

### Set up Idea


## Plugin developer notes

Install plugin:

`mvn clean install`

Run test suite:

`mvn clean test`

To run the plugin in context of plugin source code:

`mvn clean install -DskipTests -Pmojo`


# Release consideration

Before performing a new release, make sure to:

1) Update to the latest version (first entry of hotswapReleaseList) in MockHelper
2) Make sure to run all tests locally to ensure that the hotswap agent matches your JDK
3) Issue the test steps above first


## Known limitations

* This project has not been tested on Windows. Only Linux is supported and partially MacOS.
  * The DCEVM on Mac for JDK8 doesn't work properly, because of build issues.
  * Please refer to the [Descoped DCEVM for Mac on Travis-CI](/descoped/descoped-dcevm) for DCEVM on Mac
* RelProxy and plain None mode not yet supported


## Resources

* [DCEVM Hotswap Project](https://github.com/dcevm/dcevm)
* [DCEVM Orginial Project](http://ssw.jku.at/dcevm/)
* [DCEVM Hotswap Agent Project](https://github.com/HotswapProjects/HotswapAgent)


## Todo

* Add addJavaArgument
* Add addClassPathElement
* Add an envvar 'descoped.hotswap=true' that can be used to identify if container is running in DCEVM mode
