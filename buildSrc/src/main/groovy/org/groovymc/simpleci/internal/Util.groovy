/*
 * Copyright (c) 2022 GroovyMC
 * SPDX-License-Identifier: MIT
 */

package org.groovymc.simpleci.internal

import groovy.transform.CompileStatic

import javax.annotation.Nullable

@CompileStatic
class Util {
    @Nullable
    static <T> T catchAny(Closure<T> clos) {
        try {
            clos()
        } catch (Throwable ignored) {
            return null
        }
    }
}
