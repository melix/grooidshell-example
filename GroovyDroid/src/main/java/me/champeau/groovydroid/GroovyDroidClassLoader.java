package me.champeau.groovydroid;

import android.util.Log;

import com.android.dx.Version;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import dalvik.system.DexClassLoader;

/**
 * Created by cchampeau on 12/10/13.
 */
public class GroovyDroidClassLoader {

    private static final String DEX_IN_JAR_NAME = "classes.dex";
    private static final Attributes.Name CREATED_BY = new Attributes.Name("Created-By");

    private File tmpDynamicFiles;
    private ClassLoader classLoader;

    public GroovyDroidClassLoader(File tmpDir, ClassLoader parent) {
        tmpDynamicFiles = tmpDir;
        classLoader = parent;
    }

    public Class defineDynamic(String className, byte[] dalvikBytecode) {
        File tmpDex = new File(tmpDynamicFiles, className+".jar");
        try {
            FileOutputStream fos = new FileOutputStream(tmpDex);
            JarOutputStream jar = new JarOutputStream(fos, makeManifest());
            JarEntry classes = new JarEntry(DEX_IN_JAR_NAME);
            classes.setSize(dalvikBytecode.length);
            jar.putNextEntry(classes);
            jar.write(dalvikBytecode);
            jar.closeEntry();
            jar.finish();
            jar.flush();
            fos.flush();
            fos.close();
            jar.close();
            DexClassLoader loader = new DexClassLoader(tmpDex.getAbsolutePath(), tmpDynamicFiles.getAbsolutePath(), null, classLoader);
            return loader.loadClass(className);
        } catch (Throwable e) {
            Log.e("DynamicLoading", "Unable to load class",e);
        } finally {
            tmpDex.delete();
        }
        return null;
    }

    private static Manifest makeManifest() throws IOException {
        Manifest manifest = new Manifest();
        Attributes attribs = manifest.getMainAttributes();
        attribs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attribs.put(CREATED_BY, "dx " + Version.VERSION);
        attribs.putValue("Dex-Location", DEX_IN_JAR_NAME);
        return manifest;
    }

}
