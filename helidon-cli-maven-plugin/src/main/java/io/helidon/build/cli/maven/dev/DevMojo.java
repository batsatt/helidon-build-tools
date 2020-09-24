/*
 * Copyright (c) 2018, 2020 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.build.cli.maven.dev;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.helidon.build.dev.ProjectSupplier;
import io.helidon.build.dev.maven.DevLoopBuildConfig;
import io.helidon.build.dev.maven.MavenEnvironment;
import io.helidon.build.dev.maven.MavenGoalReferenceResolver;
import io.helidon.build.dev.maven.MavenProjectConfigCollector;
import io.helidon.build.dev.maven.MavenProjectSupplier;
import io.helidon.build.dev.mode.DevLoop;
import io.helidon.build.util.Log;
import io.helidon.build.util.MavenLogWriter;
import io.helidon.build.util.SystemLogWriter;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.lifecycle.LifecycleMappingDelegate;
import org.apache.maven.lifecycle.internal.DefaultLifecycleMappingDelegate;
import org.apache.maven.lifecycle.internal.MojoDescriptorCreator;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import static io.helidon.build.util.FileUtils.ensureDirectory;
import static java.util.Collections.emptyList;

/**
 * Maven plugin that runs a {@link DevLoop}.
 */
@Mojo(name = "dev",
        defaultPhase = LifecyclePhase.NONE,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class DevMojo extends AbstractMojo {

    /**
     * The Maven project this mojo executes on.
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    /**
     * The project directory.
     */
    @Parameter(defaultValue = "${project.basedir}", required = true)
    private File devProjectDir;

    /**
     * Perform an initial clean build.
     */
    @Parameter(defaultValue = "false", property = "dev.clean")
    private boolean clean;

    /**
     * Fork builds.
     */
    @Parameter(defaultValue = "false", property = "dev.fork")
    private boolean fork;

    /**
     * Use terminal mode.
     */
    @Parameter(defaultValue = "false", property = "dev.terminalMode")
    private boolean terminalMode;

    /**
     * Application JVM arguments.
     */
    @Parameter(property = "dev.appJvmArgs")
    private String appJvmArgs;

    /**
     * Application arguments.
     */
    @Parameter(property = "dev.appArgs")
    private String appArgs;

    /**
     * Skip execution for this plugin.
     */
    @Parameter(defaultValue = "false", property = "dev.skip")
    private boolean skip;

    /**
     * DevLoop build lifecycle customization.
     */
    @Parameter
    private DevLoopBuildConfig devLoop;

    /**
     * The current Maven session.
     */
    @Parameter(defaultValue = "${session}", readonly = true)
    private MavenSession session;

    /**
     * The Maven BuildPluginManager component.
     */
    @Component
    private BuildPluginManager plugins;

    /**
     * The Maven MojoDescriptorCreated component, used to resolve
     * plugin prefixes.
     */
    @Component
    private MojoDescriptorCreator mojoDescriptorCreator;

    /**
     * The Maven DefaultLifecycles component, used to map
     * a phase to a list of goals.
     */
    @Component
    private DefaultLifecycles defaultLifeCycles;

    /**
     * The Maven DefaultLifecycleMappingDelegate component, used to map
     * a phase to a list of goals.
     */
    @Component(hint = DefaultLifecycleMappingDelegate.HINT)
    private LifecycleMappingDelegate standardDelegate;

    /**
     * A map of Maven lifecycle ids to LifecycleMappingDelegate instances, used to map
     * a phase to a list of goals.
     */
    @Component
    private Map<String, LifecycleMappingDelegate> delegates;

    /**
     * Enable CLI extension check (see {@link CliExtension}).
     */
    @Parameter(defaultValue = "true", property = "dev.checkCliExtension")
    private boolean checkCliExtension;

    @Override
    public void execute() throws MojoExecutionException {
        if (skip) {
            getLog().info("Skipping execution.");
            return;
        }
        try {
            MavenProjectConfigCollector.assertSupportedProject(session);
            if (checkCliExtension) {
                CliExtension.checkCliExtension(project.getModel().getParent().getVersion());
            }
            if (terminalMode) {
                SystemLogWriter.install(getLog().isDebugEnabled() ? Log.Level.DEBUG : Log.Level.INFO);
            } else {
                MavenLogWriter.install(getLog());
            }

            final DevLoopBuildConfig configuration = buildConfig(true);
            final ProjectSupplier projectSupplier = new MavenProjectSupplier(configuration);
            final List<String> jvmArgs = toList(appJvmArgs);
            final List<String> args = toList(appArgs);
            final Path dir = devProjectDir.toPath();
            final DevLoop loop = new DevLoop(dir, projectSupplier, clean, fork, terminalMode, jvmArgs, args, configuration);
            loop.start(Integer.MAX_VALUE);
        } catch (Exception e) {
            throw new MojoExecutionException("Error", e);
        }
    }

    DevLoopBuildConfig buildConfig(boolean resolve) throws Exception {
        final DevLoopBuildConfig config = devLoop == null ? new DevLoopBuildConfig() : devLoop;
        config.validate();
        if (resolve) {
            final MavenEnvironment env = new MavenEnvironment(project, session, mojoDescriptorCreator, defaultLifeCycles,
                                                              standardDelegate, delegates, plugins);
            final MavenGoalReferenceResolver resolver = new MavenGoalReferenceResolver(env);
            config.resolve(resolver);
        }
        return config;
    }

    private static List<String> toList(String args) {
        return args == null ? emptyList() : Arrays.asList(args.split(" "));
    }

    /**
     * Manages {@code .mvn/extensions.xml} file if needed to activate the {@code EnsureCliExtension}.
     * This extension replaces the use of {@code helidon-maven-plugin} with {@code helidon-cli-maven-plugin}.
     */
    private static class CliExtension {
        private static final String HELIDON_VERSION_WITH_INCORRECT_EXTENSION = "2.0.2";
        private static final Path LOCAL_MAVEN_CONFIG_DIR = Path.of(".mvn");
        private static final Path LOCAL_EXTENSIONS_FILE = Path.of("extensions.xml");
        private static final String EXTENSIONS_RESOURCE_PATH = "maven-model-processor-extensions.xml";
        private static final String EXTENSION_ELEMENT = "<extension>";
        private static final String ARTIFACT_ID_ELEMENT = "<artifactId>helidon-maven-plugin</artifactId>";

        private static void checkCliExtension(String helidonVersion) {

            // If this is the problematic Helidon version, make sure we have a .mvn/extensions.xml file;
            // otherwise, remove it if we created it (i.e. app moves to newer version).

            if (helidonVersion.equals(HELIDON_VERSION_WITH_INCORRECT_EXTENSION)) {
                ensureExtensionsXml();
            } else {
                removeExtensionsXml();
            }
        }

        private static void ensureExtensionsXml() {

            // Create extensions.xml if not present to activate the EnsureCliExtension class.
            // If already present, it might not be ours, but we don't want to try to update.

            final Path extensionsFile = ensureDirectory(LOCAL_MAVEN_CONFIG_DIR).resolve(LOCAL_EXTENSIONS_FILE);
            if (Files.exists(extensionsFile)) {
                Log.debug("Helidon version %s, %s already present", HELIDON_VERSION_WITH_INCORRECT_EXTENSION, extensionsFile);
            } else {
                Log.debug("Helidon version %s, adding %s", HELIDON_VERSION_WITH_INCORRECT_EXTENSION, extensionsFile);
                try (InputStream in = CliExtension.class.getResourceAsStream(EXTENSIONS_RESOURCE_PATH)) {
                    Files.copy(Objects.requireNonNull(in), extensionsFile);
                } catch (Exception e) {
                    Log.debug("failed to write extensions.xml: ", e);
                }
            }
        }

        private static void removeExtensionsXml() {

            // Remove extensions.xml if present and it is our file

            final Path extensionsFile = LOCAL_MAVEN_CONFIG_DIR.resolve(LOCAL_EXTENSIONS_FILE);
            if (Files.exists(extensionsFile)) {
                try {
                    int extensionCount = 0;
                    boolean ourArtifactId = false;
                    for (String line : Files.readAllLines(extensionsFile)) {
                        if (line.contains(EXTENSION_ELEMENT)) {
                            extensionCount++;
                        } else if (line.contains(ARTIFACT_ID_ELEMENT)) {
                            ourArtifactId = true;
                            break;
                        }
                    }
                    if (extensionCount == 1 && ourArtifactId) {
                        Log.debug("removing %s", extensionsFile);
                        Files.delete(extensionsFile);
                    }
                } catch (Exception e) {
                    Log.debug("failed to remove extensions.xml: ", e);
                }
            }
        }
    }
}
