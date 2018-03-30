/*
 * Copyright 2009-2018 the original author or authors.
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
package org.codehaus.jdt.groovy.internal.compiler;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import groovy.lang.GroovyClassLoader;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.eclipse.GroovyLogManager;
import org.codehaus.groovy.eclipse.TraceCategory;
import org.codehaus.jdt.groovy.internal.compiler.ast.GroovyParser;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.groovy.core.util.ReflectionUtils;
import org.eclipse.jdt.internal.compiler.Compiler;
import org.eclipse.jdt.internal.compiler.batch.FileSystem;
import org.eclipse.jdt.internal.compiler.env.INameEnvironment;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;

public class GroovyClassLoaderFactory {

    /*
     * Each project is allowed a GroovyClassLoader that will be used to load transform definitions and supporting classes. A cache
     * is maintained from project names to the current classpath and associated loader. If the classpath matches the cached version
     * on a call to build a parser then it is reused. If it does not match then a new loader is created and stored (storing it
     * orphans the previously cached one). When either a full build or a clean or project close occurs, we also discard the loader
     * instances associated with the project.
     */
    private static Map<String, Map.Entry<IClasspathEntry[], GroovyClassLoader[]>> projectClassLoaderCache = new ConcurrentHashMap<>();

    public static void clearCache() {
        projectClassLoaderCache.clear();
    }

    public static void clearCache(String projectName) {
        projectClassLoaderCache.remove(projectName);
    }

    public static void closeClassLoader(String projectName) {
        Map.Entry<?, GroovyClassLoader[]> entry = projectClassLoaderCache.get(projectName);
        if (entry != null) {
            Stream.of(entry.getValue()).filter(Objects::nonNull).forEach(GroovyClassLoaderFactory::close);
        }
    }

    /**
     * Closes the jar files that have been kept open by the URLClassLoader.
     */
    private static void close(GroovyClassLoader groovyClassLoader) {
        Object urlClasspath = ReflectionUtils.getPrivateField(URLClassLoader.class, "ucp", groovyClassLoader);
        Object[] jarLoaders = ((Collection<?>) ReflectionUtils.getPrivateField(urlClasspath.getClass(), "loaders", urlClasspath)).toArray();
        for (Object jarLoader : jarLoaders) {
            try {
                JarFile jarFile = (JarFile) ReflectionUtils.getPrivateField(jarLoader.getClass(), "jar", jarLoader);

                String jarFileName = jarFile.getName();
                if (jarFileName.indexOf("cache") != -1 || jarFileName.indexOf("plugins") != -1) {
                    jarFile.close();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    //--------------------------------------------------------------------------

    private GroovyClassLoader batchLoader;
    private final CompilerOptions compilerOptions;
    private final LookupEnvironment lookupEnvironment;

    public GroovyClassLoaderFactory(CompilerOptions compilerOptions, Object requestor) {
        this.compilerOptions = compilerOptions;
        this.lookupEnvironment = (requestor instanceof Compiler ? ((Compiler) requestor).lookupEnvironment : null);
    }

    public GroovyClassLoader[] getGroovyClassLoaders(CompilerConfiguration compilerConfiguration) {
        if (compilerOptions.groovyProjectName == null) {
            return getBatchGroovyClassLoaders(compilerConfiguration);
        } else {
            return getProjectGroovyClassLoaders(compilerConfiguration);
        }
    }

    private GroovyClassLoader[] getBatchGroovyClassLoaders(CompilerConfiguration compilerConfiguration) {
        if (batchLoader == null && lookupEnvironment != null) {
            try {
                INameEnvironment nameEnvironment = lookupEnvironment.nameEnvironment;
                if (nameEnvironment.getClass().getName().endsWith("tests.compiler.regression.InMemoryNameEnvironment")) {
                    nameEnvironment = ((INameEnvironment[]) ReflectionUtils.getPrivateField(nameEnvironment.getClass(), "classLibs", nameEnvironment))[0];
                }
                if (nameEnvironment instanceof FileSystem) {
                    FileSystem.Classpath[] classpaths = (FileSystem.Classpath[]) ReflectionUtils.getPrivateField(FileSystem.class, "classpaths", nameEnvironment);
                    if (classpaths != null) {
                        batchLoader = new GroovyClassLoader();
                        for (FileSystem.Classpath classpath : classpaths) {
                            batchLoader.addClasspath(classpath.getPath());
                        }
                    } else {
                        System.err.println("Cannot find field 'classpaths' on FileSystem instance");
                    }
                }
            } catch (Exception e) {
                System.err.println("Unexpected problem computing classpath for AST transform loader:");
                e.printStackTrace();
            }
        }
        return new GroovyClassLoader[] {new GrapeAwareGroovyClassLoader(batchLoader), batchLoader};
    }

    private GroovyClassLoader[] getProjectGroovyClassLoaders(CompilerConfiguration compilerConfiguration) {
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(compilerOptions.groovyProjectName);
        try {
            IJavaProject javaProject = JavaCore.create(project);
            IClasspathEntry[] classpathEntries = javaProject.getResolvedClasspath(true);

            Map.Entry<IClasspathEntry[], GroovyClassLoader[]> entry = projectClassLoaderCache.computeIfAbsent(compilerOptions.groovyProjectName, key -> {
                String[] parts = calculateClasspath(classpathEntries, javaProject, project).split("###");

                String classPaths = parts[0];
                String xformPaths = parts[parts.length - 1];
                if (GroovyLogManager.manager.hasLoggers()) {
                    GroovyLogManager.manager.log(TraceCategory.AST_TRANSFORM, "transform classpath: " + xformPaths);
                }

                return new java.util.AbstractMap.SimpleEntry<>(classpathEntries, new GroovyClassLoader[] {
                    new GrapeAwareGroovyClassLoader(newClassLoader(classPaths, null/*no parent loader*/)),
                    new GroovyClassLoader(newClassLoader(xformPaths, GroovyParser.class.getClassLoader()))
                });
            });

            if (Arrays.equals(classpathEntries, entry.getKey())) {
                return entry.getValue();
            } else {
                // project classpath has changed; remove and reload
                projectClassLoaderCache.remove(compilerOptions.groovyProjectName);
                return getProjectGroovyClassLoaders(compilerConfiguration);
            }
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String calculateClasspath(IClasspathEntry[] classpathEntries, IJavaProject javaProject, IProject project) {
        Set<String> classpath = new LinkedHashSet<>();
        try {
            for (IClasspathEntry classpathEntry : classpathEntries) {
                if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
                    continue;
                }
                String pathElement = null;
                // Two kinds of entry we are interested in - those relative and those absolute
                // relative example: grails/lib/hibernate3-3.3.1.jar  (where grails is the project name)
                // absolute example: f:/grails-111/dist/grails-core-blah.jar
                // javaProject path is f:\grails\grails
                IPath cpePath = classpathEntry.getPath();
                String segmentZero = cpePath.segment(0);
                if (segmentZero.equals(project.getName())) {
                    pathElement = project.getFile(cpePath.removeFirstSegments(1)).getRawLocation().toOSString();
                } else {
                    // GRECLIPSE-917: Entry is something like /SomeOtherProject/foo/bar/doodah.jar
                    if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
                        IProject project2 = project.getWorkspace().getRoot().getProject(segmentZero);
                        if (project2 != null) {
                            IFile ifile = project2.getFile(cpePath.removeFirstSegments(1));
                            IPath ipath = (ifile == null ? null : ifile.getRawLocation());
                            pathElement = (ipath == null ? null : ipath.toOSString());
                        }
                    }
                    if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
                        IProject project2 = project.getWorkspace().getRoot().getProject(segmentZero);
                        // the classpath entry is a dependency on another project
                        computeDependenciesFromProject(project, project2, classpath);
                        // FIXASC what does all this look like for batch compilation?  Should it be passed in rather than computed here
                    } else if (pathElement == null) {
                        pathElement = classpathEntry.getPath().toOSString();
                    }
                }
                if (pathElement != null) {
                    classpath.add(pathElement);
                }
            }

            String defaultOutputLocation = pathToString(javaProject.getOutputLocation(), project);
            classpath.add(defaultOutputLocation);

            // add output locations which are not default
            if (project.hasNature("org.eclipse.jdt.groovy.core.groovyNature")) {
                for (IClasspathEntry classpathEntry : javaProject.getRawClasspath()) {
                    if (classpathEntry.getOutputLocation() != null) {
                        String location = pathToString(classpathEntry.getOutputLocation(), project);
                        if (!defaultOutputLocation.equals(location)) {
                            classpath.add(location);
                        }
                    }
                }
            }
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }

        return classpath.stream().collect(Collectors.joining(File.pathSeparator));
    }

    /**
     * Determine the exposed (exported) dependencies from the project named
     * 'otherProject' and add them to the accumulatedPathEntries String Set.
     * This will include the output location of the project plus other kinds
     * of entry that are re-exported.  If dependent on another project and
     * that project is re-exported, the method will recurse.
     *
     * @param baseProject the original project for which the classpath is being computed
     * @param requiredProject a project something in the dependency chain for the base project
     * @param classpath a String set of classpath entries, into which new entries should be added
     */
    private static void computeDependenciesFromProject(IProject baseProject, IProject requiredProject, Set<String> classpath)
            throws JavaModelException {

        IJavaProject javaProject = JavaCore.create(requiredProject);

        // add the project's output location
        classpath.add(pathToString(javaProject.getOutputLocation(), requiredProject));

        IClasspathEntry[] cpes = javaProject.getResolvedClasspath(true);
        if (cpes != null) {
            for (IClasspathEntry cpe : cpes) {
                if (cpe.getEntryKind() == IClasspathEntry.CPE_SOURCE && cpe.getOutputLocation() != null) {
                    // add the source folder's output location (if different from the project's)
                    classpath.add(pathToString(cpe.getOutputLocation(), requiredProject));
                } else if (cpe.isExported()) {
                    IPath cpePath = cpe.getPath();
                    String segmentZero = cpePath.segment(0);
                    if (segmentZero != null && segmentZero.equals(requiredProject.getName())) {
                        classpath.add(requiredProject.getFile(cpePath.removeFirstSegments(1)).getRawLocation().toOSString());
                    } else if (cpe.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
                        // segmentZero is a project name
                        computeDependenciesFromProject(requiredProject, baseProject.getWorkspace().getRoot().getProject(segmentZero), classpath);
                    } else {
                        String otherPathElement = null;
                        if (segmentZero != null && segmentZero.equals(requiredProject.getName())) {
                            otherPathElement = requiredProject.getFile(cpePath.removeFirstSegments(1)).getRawLocation().toOSString();
                        } else {
                            otherPathElement = cpePath.toOSString();
                        }
                        classpath.add(otherPathElement);
                    }
                }
            }
        }
    }

    private static String pathToString(IPath path, IProject project) {
        String realLocation = null;
        if (path != null) {
            String prefix = path.segment(0);
            if (prefix.equals(project.getName())) {
                if (path.segmentCount() == 1) {
                    // the path is actually to the project root
                    IPath rawPath = project.getRawLocation();
                    if (rawPath != null) {
                        realLocation = rawPath.toOSString();
                    } else {
                        realLocation = project.getLocation().toOSString();
                    }
                } else {
                    IPath rawLocation = project.getFile(path.removeFirstSegments(1)).getRawLocation();
                    if (rawLocation != null) {
                        realLocation = rawLocation.toOSString();
                    }
                }
            } else {
                realLocation = path.toOSString();
            }
        }
        return realLocation;
    }

    private static URLClassLoader newClassLoader(String classpath, ClassLoader parent) {
        URL[] urls = Stream.of(classpath.split(File.pathSeparator)).map(file -> {
            try {
                return new File(file).toURI().toURL();
            } catch (MalformedURLException ignore) {
                return null;
            }
        }).filter(Objects::nonNull).toArray(URL[]::new);

        if (NONLOCKING) {
            return new org.apache.xbean.classloader.NonLockingJarFileClassLoader("AST Transform loader", urls, parent);
        } else {
            return new URLClassLoader(urls, parent);
        }
    }

    private static final boolean NONLOCKING = Boolean.getBoolean("greclipse.nonlocking");
    static {
        if (NONLOCKING) {
            System.out.println("property set: greclipse.nonlocking: will try to avoid locking jars");
        }
    }

    //--------------------------------------------------------------------------

    public static class GrapeAwareGroovyClassLoader extends GroovyClassLoader {

        /** {@code true} if any grabbing is done */
        public boolean grabbed;

        public GrapeAwareGroovyClassLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        public void addURL(URL url) {
            this.grabbed = true;
            super.addURL(url);
        }
    }
}
