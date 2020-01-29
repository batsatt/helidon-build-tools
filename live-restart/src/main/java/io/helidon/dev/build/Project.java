/*
 * Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
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

package io.helidon.dev.build;

import java.nio.file.Path;
import java.util.List;

/**
 * Accessor for build project.
 */
public interface Project {
    /**
     * Returns the root directory.
     *
     * @return The root.
     */
    ProjectDirectory root();

    /**
     * Returns the build system file (e.g. {@code pom.xml}).
     *
     * @return The file.
     */
    BuildFile buildSystemFile();

    /**
     * Update the project if needed.
     *
     * @param force {@code true} if update should be done regardless of any actual changes.
     */
    void update(boolean force);

    /**
     * Returns a list of paths to all external dependencies. A path may point
     * to a directory, in which case all contained jar files should be considered
     * dependencies.
     *
     * @return The paths.
     */
    List<Path> dependencies();

    /**
     * Returns all components.
     *
     * @return The components.
     */
    List<BuildComponent> components();

    /**
     * Returns the project classpath.
     *
     * @return The classpath.
     */
    String classpath();

    /**
     * Returns a list of build roots of the given type.
     *
     * @param type The type.
     * @return The roots. May be empty.
     */
    List<BuildRoot> buildRoots(BuildType type);

    /**
     * Perform an initial build.
     *
     * @return The errors, if any.
     */
    List<String> build();

    /**
     * Perform an incremental build.
     *
     * @return The errors, if any.
     */
    List<String> incrementalBuild();
}