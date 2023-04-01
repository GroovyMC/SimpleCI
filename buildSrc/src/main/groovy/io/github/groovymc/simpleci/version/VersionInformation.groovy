/*
 * Copyright (c) 2022 GroovyMC
 * SPDX-License-Identifier: MIT
 */

package io.github.groovymc.simpleci.version

import groovy.transform.CompileStatic
import org.eclipse.jgit.revwalk.RevCommit

import javax.annotation.Nullable

@CompileStatic
final class VersionInformation {
    int major, minor, patch
    AlphaBetaTracker alphaBeta = new AlphaBetaTracker()
    @Nullable String metadata

    static VersionInformation fromTagName(String tagName) {
        final VersionInformation versionInformation = new VersionInformation()
        if (tagName.endsWith('-alpha')) {
            versionInformation.parseFromString(tagName.dropRight('-alpha'.length()))
            versionInformation.alphaBeta.startedAsAlpha = true
            versionInformation.alphaBeta.increaseAlpha()
        } else if (tagName.endsWith('-beta')) {
            versionInformation.parseFromString(tagName.dropRight('-beta'.length()))
            versionInformation.alphaBeta.startedAsBeta = true
            versionInformation.alphaBeta.increaseBeta()
        } else {
            versionInformation.parseFromString(tagName)
        }
        return versionInformation
    }

    private void parseFromString(String ver) {
        final spl = ver.split('\\.')
        if (spl.length == 1) {
            major = spl[0] as int
        } else if (spl.length == 2) {
            major = spl[0] as int
            minor = spl[1] as int
        } else {
            major = spl[0] as int
            minor = spl[1] as int
            patch = spl[2] as int
        }
    }

    boolean encounterCommit(boolean ignoreNotOnCI, boolean ciByDefault, RevCommit commit) {
        final message = commit.fullMessage
        if (ignoreNotOnCI) {
            if ((ciByDefault && message.contains('[noci]')) || (!ciByDefault && !message.contains('[ci]'))) {
                return false
            }
        }

        final hasMinor = message.contains('[minor]')
        if (hasMinor) {
            minor++
            // Let's reset alphas and beta versions as we bumped the minor
            alphaBeta.reset()
        }

        if (message.contains('[beta]')) {
            alphaBeta.increaseBeta()
        } else if (message.contains('[alpha]')) {
            alphaBeta.increaseAlpha()
        } else {
            if (!hasMinor) {
                if (!alphaBeta.startedAsAlpha && !alphaBeta.startedAsBeta) {
                    alphaBeta.reset() // We haven't started as neither alpha nor beta so no point in continuing to bump those
                    patch++
                } else if (alphaBeta.startedAsAlpha) {
                    alphaBeta.increaseAlpha()
                } else if (alphaBeta.startedAsBeta) {
                    alphaBeta.increaseBeta()
                }
            }
        }

        return true
    }

    String toString(boolean withMetadata) {
        if (withMetadata && metadata) {
            "$major.$minor.$patch$alphaBeta+$metadata"
        } else {
            "$major.$minor.$patch$alphaBeta"
        }
    }

    @Override
    String toString() {
        return toString(false)
    }

    static final class AlphaBetaTracker {
        int betaNumber = -1
        boolean startedAsBeta

        int alphaNumber = -1
        boolean startedAsAlpha

        boolean lastEncounteredAlpha

        void increaseAlpha() {
            this.alphaNumber++
            lastEncounteredAlpha = true
        }

        void increaseBeta() {
            this.betaNumber++
            lastEncounteredAlpha = false
        }

        void reset() {
            this.alphaNumber = -1
            this.betaNumber = -1

            if (startedAsAlpha) {
                increaseAlpha()
            } else if (startedAsBeta) {
                increaseBeta()
            }
        }

        @Override
        String toString() {
            if (betaNumber == -1 && alphaNumber == -1) {
                return ''
            }

            if (lastEncounteredAlpha && alphaNumber >= 0) {
                return "-alpha${alphaNumber == 0 ? '' : '.' + alphaNumber}"
            } else {
                return "-beta${betaNumber == 0 ? '' : '.' + betaNumber}"
            }
        }

        <T> T ifVersionType(T alpha = null, T beta = null, T release = null) {
            return alphaNumber != -1 ? alpha : (betaNumber != -1 ? beta : release)
        }
    }
}
