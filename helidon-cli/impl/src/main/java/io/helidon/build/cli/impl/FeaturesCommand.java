/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
package io.helidon.build.cli.impl;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import io.helidon.build.cli.harness.Command;
import io.helidon.build.cli.harness.CommandContext;
import io.helidon.build.cli.harness.CommandContext.ExitStatus;
import io.helidon.build.cli.harness.CommandExecution;
import io.helidon.build.cli.harness.Creator;
import io.helidon.build.cli.harness.Option.Flag;
import io.helidon.build.cli.harness.Option.KeyValues;
import io.helidon.build.util.PomUtils;
import io.helidon.build.util.ProjectConfig;
import io.helidon.build.util.ProjectDependency;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

import static io.helidon.build.util.ProjectConfig.PROJECT_FEATURES;

/**
 * The {@code features} command.
 */
@Command(name = "features", description = "List or add features to the project")
public final class FeaturesCommand extends BaseCommand implements CommandExecution {

    private final CommonOptions commonOptions;
    private final Collection<String> add;
    private final boolean list;
    private final boolean all;

    @Creator
    FeaturesCommand(
            CommonOptions commonOptions,
            @KeyValues(name = "add", description = "Add features to the project") Collection<String> add,
            @Flag(name = "list", description = "List the features used in the project") boolean list,
            @Flag(name = "all", description = "List all available features") boolean all) {

        this.commonOptions = commonOptions;
        this.add = add;
        this.list = list;
        this.all = all;
    }

    @Override
    public void execute(CommandContext context) {
        if (!add.isEmpty()) {
            if (list || all) {
                exitAction(context);
            } else {
                addFeatures(context);
            }
        } else if (list ^ all) {
            if (list) {
                listProjectFeatures(context);
            } else {
                listAllFeatures(context);
            }
        } else {
            exitAction(context);
        }
    }

    private void listAllFeatures(CommandContext context) {
        ProjectConfig projectConfig = projectConfig(commonOptions.project().toPath());
        projectConfig.listFeatures().forEach(context::logInfo);
    }

    private void listProjectFeatures(CommandContext context) {
        ProjectConfig projectConfig = projectConfig(commonOptions.project().toPath());
        projectConfig.propertyAsList(PROJECT_FEATURES).forEach(context::logInfo);
    }

    private void addFeatures(CommandContext context) {
        Path projectDir = commonOptions.project().toPath();
        Path pomFile = projectDir.resolve("pom.xml");
        Model model = PomUtils.readPomModel(pomFile);
        ProjectConfig projectConfig = projectConfig(commonOptions.project().toPath());

        // Get info of features already added
        List<String> features = new ArrayList<>(projectConfig.propertyAsList(PROJECT_FEATURES));
        int featuresSize = features.size();

        // Update pom model adding dependencies for each feature
        add.forEach(featureName -> {
            // Get list of deps for this feature -- must be non-empty
            List<ProjectDependency> featureDeps = projectConfig.featureDeps(featureName);
            if (featureDeps.isEmpty()) {
                context.exitAction(ExitStatus.FAILURE, "Feature '" + featureName + "' does not exist");
                return;
            }

            if (!features.contains(featureName)) {
                List<Dependency> existingDeps = model.getDependencies();
                featureDeps.forEach(fd -> {
                    // Check if dependency already there
                    Optional<Dependency> found = existingDeps.stream().filter(d ->
                            d.getGroupId().equals(fd.groupId())
                                    && d.getArtifactId().equals(fd.artifactId())
                                    && Objects.equals(d.getVersion(), fd.version())).findFirst();

                    // Now add dependency if necessary
                    if (found.isEmpty()) {
                        Dependency newDep = new Dependency();
                        newDep.setGroupId(fd.groupId());
                        newDep.setArtifactId(fd.artifactId());
                        newDep.setVersion(fd.version());
                        model.addDependency(newDep);
                        context.logInfo("Adding '" + fd + "' to the project's pom");
                    } else {
                        context.logInfo("Dependency '" + fd + "' already in project's pom");
                    }
                });
                features.add(featureName);
            } else {
                context.logInfo("Feature '" + featureName + "' already added to project");
            }
        });

        // Update project config if necessary
        if (features.size() > featuresSize) {
            projectConfig.property(PROJECT_FEATURES, features);
            projectConfig.store();
        }

        // Write model back to pom file
        PomUtils.writePomModel(pomFile, model);
    }

    private void exitAction(CommandContext context) {
        context.exitAction(ExitStatus.FAILURE, "Invalid command options, use --help");
    }
}
