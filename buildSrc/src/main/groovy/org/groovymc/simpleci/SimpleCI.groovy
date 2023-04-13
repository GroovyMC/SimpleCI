/*
 * Copyright (c) 2022 GroovyMC
 * SPDX-License-Identifier: MIT
 */

package org.groovymc.simpleci

import groovy.transform.CompileStatic
import org.groovymc.simpleci.task.ChangelogTask
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.groovymc.simpleci.version.VersioningExtension

@CompileStatic
class SimpleCI implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.extensions.create('versioning', VersioningExtension, project)
        project.tasks.register('changelog', ChangelogTask)

        project.tasks.register('configureTeamCity') {
            it.setOnlyIf {
                System.getenv('TEAMCITY_VERSION')
            }
            // Print the marker lines into the log which configure the pipeline
            it.doLast {
                project.getLogger().lifecycle('Setting project variables and parameters.')
                println "##teamcity[buildNumber '${project.version}']"
            }
        }
    }
}
