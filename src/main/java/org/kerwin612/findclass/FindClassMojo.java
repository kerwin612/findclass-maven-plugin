package org.kerwin612.findclass;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.reflections.Store;
import org.reflections.adapters.JavaReflectionAdapter;
import org.reflections.adapters.MetadataAdapter;
import org.reflections.scanners.AbstractScanner;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.vfs.Vfs;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Mojo(
    name = "find",
    threadSafe = true,
    defaultPhase = LifecyclePhase.PROCESS_CLASSES,
    requiresDependencyResolution = ResolutionScope.RUNTIME)
public class FindClassMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
  private File classesDirectory;

  @Parameter(defaultValue = "foundClass", required = true)
  private String outputPropertyName;

  @Parameter private String classAnnotationMatch;

  @Parameter private String classParentMatch;

  @Parameter private String methodKeyMatch;

  @Parameter(defaultValue = "false")
  private Boolean firstFound;

  @Parameter(defaultValue = "false")
  private Boolean required;

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  protected File getClassesDirectory() {
    return classesDirectory;
  }

  public void execute() throws MojoExecutionException {
    List<String> foundClass;
    try {
      foundClass = findClass(getClassesDirectory());
    } catch (Exception ex) {
      throw new MojoExecutionException(ex.getMessage(), ex);
    }
    getLog().info("found class: " + foundClass);
    if (required && (foundClass == null || foundClass.size() < 1)) {
      throw new MojoExecutionException("Unable to find class");
    }
    project
        .getProperties()
        .put(
            outputPropertyName,
            firstFound ? (foundClass.size() > 0 ? foundClass.get(0) : "") : foundClass);
    getLog()
        .info(
            String.format(
                "set property <%s> = <%s>",
                outputPropertyName, project.getProperties().get(outputPropertyName)));
  }

  private List<String> findClass(File classesDirectory) throws MalformedURLException {
    final String matchKey = "_match";
    final URL targetUrl = classesDirectory.toURI().toURL();
    List<URL> compileClasspathUrls = new ArrayList<>(Arrays.asList(targetUrl));
    try {
      compileClasspathUrls.addAll(
          project.getCompileClasspathElements().stream()
              .map(
                  c -> {
                    try {
                      return new File(c).toURI().toURL();
                    } catch (MalformedURLException e) {
                      getLog().warn(e);
                    }
                    return null;
                  })
              .filter(u -> u != null)
              .collect(Collectors.toList()));
    } catch (Exception e) {
      getLog().warn(e);
    }
    return new Reflections(
            ConfigurationBuilder.build(
                    targetUrl,
                    new URLClassLoader(
                        compileClasspathUrls.toArray(new URL[compileClasspathUrls.size()])),
                    new AbstractScanner() {
                      @Override
                      public Object scan(Vfs.File file, Object classObject, Store store) {
                        if (classObject == null) {
                          try {
                            classObject =
                                ((JavaReflectionAdapter) getConfiguration().getMetadataAdapter())
                                    .getOrCreateClassObject(
                                        file, getConfiguration().getClassLoaders());
                          } catch (Exception e) {
                            throw new ReflectionsException(
                                "could not create class object from file " + file.getRelativePath(),
                                e);
                          }
                        }
                        return super.scan(file, classObject, store);
                      }

                      @Override
                      public void scan(Object cls, Store store) {
                        final MetadataAdapter md = getMetadataAdapter();
                        final String className = md.getClassName(cls);

                        if (!classParentMatch(md, cls)) return;
                        if (!classAnnotationMatch(md, cls)) return;
                        if (!methodKeyMatch(md, cls)) return;

                        store.put(matchKey, cls.toString(), className);
                      }
                    })
                .setMetadataAdapter(new JavaReflectionAdapter()))
        .getStore().values(matchKey).stream().collect(Collectors.toList());
  }

  public boolean classParentMatch(MetadataAdapter md, Object cls) {
    if (!StringUtils.isNotBlank(classParentMatch)) return true;
    try {
      Class superclass = null;
      List<Class> superclassList = new ArrayList<>();
      Collections.addAll(superclassList, ((Class) cls).getInterfaces());
      Function<Class, Class> getSuperClass = aClass -> aClass.getSuperclass();
      while ((superclass = getSuperClass.apply(superclass == null ? (Class) cls : superclass))
          != null) {
        superclassList.add(superclass);
      }
      return (superclassList.stream()
          .map(s -> md.getClassName(s))
          .anyMatch(s -> (classParentMatch.equals(s) || Pattern.matches(classParentMatch, s))));
    } catch (Throwable t) {
      getLog().warn(t);
    }
    return false;
  }

  public boolean classAnnotationMatch(MetadataAdapter md, Object cls) {
    if (!StringUtils.isNotBlank(classAnnotationMatch)) return true;
    boolean classIsMatch = false;
    try {
      for (String annotationType : (List<String>) md.getClassAnnotationNames(cls)) {
        if (classAnnotationMatch.equals(annotationType)
            || Pattern.matches(classAnnotationMatch, annotationType)) {
          classIsMatch = true;
          break;
        }
      }
    } catch (Throwable t) {
      getLog().warn(t);
    }
    return classIsMatch;
  }

  public boolean methodKeyMatch(MetadataAdapter md, Object cls) {
    if (StringUtils.isBlank(methodKeyMatch)) return true;
    boolean methodIsMatch = false;
    try {
      for (Object method : md.getMethods(cls)) {
        String key = method.toString();
        if (key.equals(methodKeyMatch) || Pattern.matches(methodKeyMatch, key)) {
          methodIsMatch = true;
          break;
        }
      }
    } catch (Throwable t) {
      getLog().warn(t);
    }
    return methodIsMatch;
  }
}
