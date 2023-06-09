/*
 * Copyright (c) 2022 GroovyMC
 * SPDX-License-Identifier: MIT
 */

package org.groovymc.simpleci.task

import groovy.transform.CompileStatic
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.*
import org.groovymc.simpleci.version.VersionInformation
import org.groovymc.simpleci.version.VersioningExtension

@CompileStatic
abstract class ChangelogTask extends DefaultTask {
    private final Property<File> gitDir
    ChangelogTask() {
        this.gitDir = project.objects.property(File)

        getOutput().convention(project.layout.file(project.provider {
            new File(project.buildDir, 'changelog.txt')
        }))
        getOutputs().upToDateWhen { false } // We want to make sure always to generate accurate changelogs
        getGitDir().convention(project.provider { getProject().projectDir })
    }

    @Optional
    @OutputFile
    abstract RegularFileProperty getOutput()

    @Input
    @Optional
    abstract Property<String> getStart()

    @Internal
    Property<File> getGitDir() {
        return gitDir
    }

    @TaskAction
    void run() {
        final Map<Integer, List<String>> changelog = new HashMap<>()
        final git = Git.open(getGitDir().get())

        final VersioningExtension versioningExtension = project.extensions.getByType(VersioningExtension)

        final start = getStart().map {
            VersioningExtension.findTagStart(git, it)
        }.getOrElse(new AbstractMap.SimpleEntry<Ref, String>(null, null))

        final ver = versioningExtension.calculateVersion(
                git,
                { RevCommit commit, VersionInformation info ->
                    changelog.computeIfAbsent(info.major, { new ArrayList<>() })
                        .add("- ${info}: ${commit.fullMessage.replaceAll(/\[beta]|\[alpha]|\[minor]/, '').trim().padRight(2)}".toString())
                },
                git.repository.resolve('HEAD'),
                start
        )

        git.close()

        try (final writer = output.get().asFile.newWriter()) {
            changelog.sort({ -it.key }).forEach { Integer version, List<String> commits ->
                writer.write("==== ${version}.0${ver.metadata === null ? '' : '+' + ver.metadata}\n")
                writer.write(commits.reverse().join('\n'))
                writer.write('\n\n')
            }
        }
    }

    void addArtifact(MavenPublication publication) {
        project.afterEvaluate {
            publication.artifact(output.get().asFile) {
                it.builtBy(this)
                it.extension = 'txt'
                it.classifier = 'changelog'
            }
        }
    }
}
