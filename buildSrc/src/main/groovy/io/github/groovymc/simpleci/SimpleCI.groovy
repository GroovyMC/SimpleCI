/*
 * Copyright (c) 2022 GroovyMC
 * SPDX-License-Identifier: MIT
 */

package io.github.groovymc.simpleci

import groovy.transform.CompileStatic
import io.github.groovymc.simpleci.task.ChangelogTask
import io.github.groovymc.simpleci.version.VersioningExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

@CompileStatic
class SimpleCI implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.extensions.create('versioning', VersioningExtension, project)
        project.tasks.register('changelog', ChangelogTask)
    }
}
