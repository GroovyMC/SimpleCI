/*
 * Copyright (c) 2022 GroovyMC
 * SPDX-License-Identifier: MIT
 */

package io.github.groovymc.simpleci.version


import groovy.transform.CompileStatic
import groovy.transform.NamedVariant
import groovy.transform.stc.ClosureParams
import io.github.groovymc.simpleci.internal.Util
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.AnyObjectId
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTag
import org.gradle.api.Project
import org.gradle.api.provider.Property

import javax.annotation.Nullable
import javax.inject.Inject
import java.util.function.BiConsumer
import java.util.stream.Collectors
import java.util.stream.Stream
import java.util.stream.StreamSupport

@CompileStatic
abstract class VersioningExtension {
    private final Project project
    @Inject
    VersioningExtension(final Project project) {
        this.project = project

        getIgnoreNotOnCI().convention(true)
        getCIByDefault().convention(true)

        getFromTag().convention(project.provider {
            Util.catchAny {
                Git.open(project.projectDir).withCloseable { it.describe().setTags(true).setLong(false).call() }
            }
        })
    }

    String getCalculatedVersion() {
        final ver = Git.open(project.projectDir).withCloseable { this.calculateVersion(it, { cm, inf -> }) }
        return (ver ?: {throw new NullPointerException('Count not calculate version number!')}()).toString()
    }

    @NamedVariant
    VersionInformation calculateVersion(Git git, BiConsumer<RevCommit, VersionInformation> onEncounter,
                                        AnyObjectId until = git.repository.resolve('HEAD'),
                                        Map.Entry<Ref, String> start = findTagStart(git, getFromTag().get())) {
        if (start === null) return null

        final commitByTag = git.tagList().call().stream()
            .collect(Collectors.toMap({ Ref ref ->
                final peeledRef = git.repository.refDatabase.peel(ref)
                return peeledRef.peeledObjectId ?: peeledRef.objectId
            }, { Ref ref -> ref }))

        def version = start.value === null ? null : VersionInformation.fromTagName(start.value)
        final peeled = start.key === null ? null : git.repository.refDatabase.peel(start.key)

        final log = git.log()
        if (peeled !== null) {
            log.addRange(peeled.peeledObjectId ?: peeled.objectId, until)
        } else {
            log.all()
        }
        final allCommits =
                StreamSupport.stream(log.setMaxCount(Integer.MAX_VALUE).call().spliterator(), false).collect(Collectors.toList())

        final ignoreNotOnCI = getIgnoreNotOnCI().get()
        final ciByDefault = getCIByDefault().get()
        for (int i = allCommits.size() - 1; i >= 0; i--) {
            final commit = allCommits[i]
            final asTag = commitByTag[commit.id]
            if (asTag === null) {
                version?.tap {
                    if (it.encounterCommit(ignoreNotOnCI, ciByDefault, commit))
                        onEncounter.accept(commit, it)
                }
            } else {
                version = VersionInformation.fromTagName(asTag.name.drop('refs/tags/'.length()))
                onEncounter.accept(commit, version)
            }
        }

        version.metadata = getVersionMetadata().getOrElse(null)
        return version
    }

    @Nullable
    static Map.Entry<Ref, String> findTagStart(Git git, String startTag) {
        final startRef = git.getRepository().getRefDatabase().getRefsByPrefix(Constants.R_TAGS + startTag).stream()
                .filter { it.name == "refs/tags/$startTag" }.findFirst().get()
        if (startRef == null) return null
        return new AbstractMap.SimpleEntry<Ref, String>(startRef, startTag)
    }

    abstract Property<String> getFromTag()
    abstract Property<String> getVersionMetadata()

    abstract Property<Boolean> getIgnoreNotOnCI()
    abstract Property<Boolean> getCIByDefault()
}
