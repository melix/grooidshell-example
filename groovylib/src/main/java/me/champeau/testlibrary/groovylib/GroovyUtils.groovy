package me.champeau.testlibrary.groovylib

import groovy.transform.CompileStatic

@CompileStatic
class GroovyUtils {
    static String hello() { 'Hello from library!' }
}