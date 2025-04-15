/*
The MIT License

Copyright (c) 2016-2023 kong <congcoi123@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package com.tenio.core.bootstrap.injector;

import com.tenio.common.logger.SystemLogger;
import com.tenio.common.utility.ClassLoaderUtility;
import com.tenio.common.utility.StringUtility;
import com.tenio.core.bootstrap.annotation.Autowired;
import com.tenio.core.bootstrap.annotation.AutowiredAcceptNull;
import com.tenio.core.bootstrap.annotation.AutowiredQualifier;
import com.tenio.core.bootstrap.annotation.Bean;
import com.tenio.core.bootstrap.annotation.BeanFactory;
import com.tenio.core.bootstrap.annotation.ClientCommand;
import com.tenio.core.bootstrap.annotation.Component;
import com.tenio.core.bootstrap.annotation.EventHandler;
import com.tenio.core.bootstrap.annotation.RestController;
import com.tenio.core.bootstrap.annotation.RestMapping;
import com.tenio.core.bootstrap.annotation.Setting;
import com.tenio.core.bootstrap.annotation.SystemCommand;
import com.tenio.core.command.client.AbstractClientCommandHandler;
import com.tenio.core.command.client.ClientCommandManager;
import com.tenio.core.command.system.AbstractSystemCommandHandler;
import com.tenio.core.command.system.SystemCommandManager;
import com.tenio.core.entity.Player;
import com.tenio.core.exception.DuplicatedBeanCreationException;
import com.tenio.core.exception.IllegalDefinedAccessControlException;
import com.tenio.core.exception.IllegalReturnTypeException;
import com.tenio.core.exception.InvalidRestMappingClassException;
import com.tenio.core.exception.MultipleImplementedClassForInterfaceException;
import com.tenio.core.exception.NoImplementedClassFoundException;
import jakarta.servlet.http.HttpServlet;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.concurrent.ThreadSafe;
import org.reflections.Reflections;

/**
 * The Injector class is the core component of the dependency injection system.
 * It manages the creation, configuration, and lifecycle of all beans (objects) in the application.
 * 
 * <p>Key features:
 * <ul>
 * <li>Automatic bean discovery and instantiation</li>
 * <li>Support for constructor, field, and method injection</li>
 * <li>Qualifier-based dependency resolution</li>
 * <li>Thread-safe bean management</li>
 * <li>Support for singleton and prototype scopes</li>
 * <li>Integration with REST controllers and servlets</li>
 * <li>System and client command handling</li>
 * </ul>
 * 
 * <p>The injector uses reflection to:
 * <ul>
 * <li>Scan packages for annotated classes</li>
 * <li>Create instances of components</li>
 * <li>Wire dependencies between components</li>
 * <li>Manage bean lifecycle</li>
 * </ul>
 * 
 * <p>Supported annotations:
 * <ul>
 * <li>{@code @Component} - Marks a class as a managed component</li>
 * <li>{@code @Autowired} - Injects dependencies into fields</li>
 * <li>{@code @Bean} - Declares a method that produces a bean</li>
 * <li>{@code @BeanFactory} - Marks a class that produces beans</li>
 * <li>{@code @RestController} - Marks a class as a REST endpoint</li>
 * </ul>
 * 
 * @see Component
 * @see Autowired
 * @see Bean
 * @see BeanFactory
 * @see RestController
 */
@ThreadSafe
public final class Injector extends SystemLogger {

  private static final Injector instance = new Injector();

  /**
   * A map contains keys are interfaces and values hold keys' implemented classes.
   * This map is protected by the class instance to ensure thread-safety.
   * 
   * <p>The map structure is:
   * <ul>
   * <li>Key: Interface class</li>
   * <li>Value: Implementation class</li>
   * </ul>
   */
  private final Map<Class<?>, Class<?>> classesMap;

  /**
   * A map has keys are {@link #classesMap}'s key implemented classes and the value are keys'
   * instances. This map is protected by the class instance to ensure thread-safety.
   * 
   * <p>The map structure is:
   * <ul>
   * <li>Key: BeanClass (class type + qualifier name)</li>
   * <li>Value: Bean instance</li>
   * </ul>
   */
  private final Map<BeanClass, Object> classBeansMap;

  /**
   * A set of classes that are created by {@link Bean} and {@link BeanFactory} annotations.
   * This set is protected by the class instance to ensure thread-safety.
   */
  private final Set<Class<?>> manualClassesSet;

  /**
   * A map for managing REST servlet instances.
   * 
   * <p>The map structure is:
   * <ul>
   * <li>Key: Servlet path/URL pattern</li>
   * <li>Value: HttpServlet instance</li>
   * </ul>
   */
  private final Map<String, HttpServlet> servletBeansMap;

  /**
   * The manager for system-level commands.
   * Handles internal server commands and operations.
   */
  private final SystemCommandManager systemCommandManager;

  /**
   * The manager for client-level commands.
   * Handles commands initiated by connected clients.
   */
  private final ClientCommandManager clientCommandManager;

  private Injector() {
    if (instance != null) {
      throw new ExceptionInInitializerError("Could not re-create the class instance");
    }

    classesMap = new HashMap<>();
    manualClassesSet = new HashSet<>();
    classBeansMap = new HashMap<>();
    servletBeansMap = new HashMap<>();
    systemCommandManager = new SystemCommandManager();
    clientCommandManager = new ClientCommandManager();
  }

  /**
   * Returns the singleton instance of the injector.
   * This method ensures that only one instance of the injector exists in the application.
   *
   * @return the singleton {@link Injector} instance
   */
  public static Injector newInstance() {
    return instance;
  }

  /**
   * Scans all input packages to create classes' instances and put them into maps.
   *
   * @param entryClass the root class which should be located in the parent package of other
   *                   class' packages
   * @param packages   a list of packages' names. It allows to define the scanning packages by
   *                   their names
   * @throws InstantiationException          it is caused by
   *                                         Class#getDeclaredConstructor(Class[])#newInstance()
   * @throws IllegalAccessException          it is caused by
   *                                         Class#getDeclaredConstructor(Class[])#newInstance()
   * @throws ClassNotFoundException          it is caused by
   *                                         {@link #getImplementedClass(Class, String, Class)}
   * @throws IllegalArgumentException        it is related to the illegal argument exception
   * @throws InvocationTargetException       it is caused by
   *                                         Class#getDeclaredConstructor(Class[])#newInstance()
   * @throws NoSuchMethodException           it is caused by
   *                                         {@link Class#getDeclaredConstructor(Class[])}
   * @throws SecurityException               it is related to the security exception
   * @throws DuplicatedBeanCreationException when a same bean was created more than one time
   */
  public void scanPackages(Class<?> entryClass, String... packages)
      throws InstantiationException, IllegalAccessException, ClassNotFoundException,
      IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
      SecurityException, DuplicatedBeanCreationException {

    // clean first
    reset();

    Set<String> setPackageNames = new HashSet<>();

    if (entryClass != null) {
      setPackageNames.add(entryClass.getPackage().getName());
    }

    if (packages != null) {
      setPackageNames.addAll(Arrays.asList(packages));
    }

    // fetches all classes that are in the same package as the root one
    Set<Class<?>> classes = new HashSet<>();
    // declares a reflection object based on the package of root class
    Reflections reflections = new Reflections();
    for (String packageName : setPackageNames) {
      Set<Class<?>> packageClasses = ClassLoaderUtility.getClasses(packageName);
      classes.addAll(packageClasses);

      Reflections reflectionPackage = new Reflections(packageName);
      reflections.merge(reflectionPackage);
    }

    // Step 1: We collect classes
    // The implemented class is defined with the "Component" annotation declared inside it
    // in case you need more annotations with the same effect with this one, you should put them
    // in here
    // Suppressing unchecked warning because generic array creation is not allowed in Java
    @SuppressWarnings("unchecked")
    Class<? extends Annotation>[] listAnnotations = new Class[] {
        Component.class,
        EventHandler.class,
        Setting.class
    };
    Set<Class<?>> implementedComponentClasses = new HashSet<>();
    Arrays.stream(listAnnotations).forEach(
        annotation -> implementedComponentClasses.addAll(
            reflections.getTypesAnnotatedWith(annotation)));
    // scans all interfaces with their implemented classes
    for (Class<?> implementedClass : implementedComponentClasses) {
      Class<?>[] classInterfaces = implementedClass.getInterfaces();
      // in case the class has not implemented any interfaces, it still can be created, so put
      // the class into the map
      if (classInterfaces.length == 0) {
        classesMap.put(implementedClass, implementedClass);
      } else {
        // normal case, put the pair of class and interface
        // the interface will be used to retrieved back the corresponding class when we want to
        // create a bean by its interface
        for (Class<?> classInterface : classInterfaces) {
          classesMap.put(implementedClass, classInterface);
        }
      }
    }

    // Retrieves all classes those are declared by the @Bean annotation
    Set<Class<?>> implementedBeanClasses = new HashSet<>();
    Set<Class<?>> beanFactoryClasses = reflections.getTypesAnnotatedWith(BeanFactory.class);
    for (Class<?> configurationClass : beanFactoryClasses) {
      for (Method method : configurationClass.getMethods()) {
        if (method.isAnnotationPresent(Bean.class)) {
          if (Modifier.isPublic(method.getModifiers())) {
            Class<?> clazz = method.getReturnType();
            if (clazz.isPrimitive()) {
              throw new IllegalReturnTypeException();
            } else if (clazz.equals(Void.TYPE)) {
              throw new IllegalReturnTypeException();
            } else {
              manualClassesSet.add(clazz);
              implementedBeanClasses.add(clazz);
            }
          } else {
            throw new IllegalDefinedAccessControlException();
          }
        }
      }
    }
    // append all classes
    for (Class<?> implementedClass : implementedBeanClasses) {
      classesMap.put(implementedClass, implementedClass);
    }

    // Add all classes annotated by @RestController
    Set<Class<?>> implementedRestClasses =
        new HashSet<>(reflections.getTypesAnnotatedWith(RestController.class));
    // append all classes
    for (Class<?> implementedClass : implementedRestClasses) {
      classesMap.put(implementedClass, implementedClass);
    }

    // Step 2: We create instances
    // create beans (class instances) based on annotations
    for (Class<?> clazz : classes) {
      // in case you need to create a bean with another annotation, put it in here
      // but notices to put it in "implementedClasses" first
      // create beans automatically
      if (isClassAnnotated(clazz, listAnnotations)) {
        BeanClass beanClass = new BeanClass(clazz, "");
        if (classBeansMap.containsKey(beanClass)) {
          throw new DuplicatedBeanCreationException(beanClass.clazz(), beanClass.name());
        }
        Object bean = clazz.getDeclaredConstructor().newInstance();
        classBeansMap.put(beanClass, bean);

        // create beans manually
      } else if (clazz.isAnnotationPresent(BeanFactory.class)) {
        // fetches all bean instances and save them to classes map
        Object beanFactoryInstance = clazz.getDeclaredConstructor().newInstance();
        for (Method method : clazz.getMethods()) {
          if (method.isAnnotationPresent(Bean.class)) {
            if (Modifier.isPublic(method.getModifiers())) {
              Class<?> methodClazz = method.getReturnType();
              if (methodClazz.isPrimitive()) {
                throw new IllegalReturnTypeException();
              } else if (methodClazz.equals(Void.TYPE)) {
                throw new IllegalReturnTypeException();
              } else {
                BeanClass beanClass =
                    new BeanClass(methodClazz, method.getAnnotation(Bean.class).value());
                if (classBeansMap.containsKey(beanClass)) {
                  throw new DuplicatedBeanCreationException(beanClass.clazz(), beanClass.name());
                }
                Object bean = method.invoke(beanFactoryInstance);
                classBeansMap.put(beanClass, bean);
              }
            } else {
              throw new IllegalDefinedAccessControlException();
            }
          }
        }
      } else if (clazz.isAnnotationPresent(RestController.class)) {
        // fetches all bean instances and save them to rest controller map
        Object restControllerInstance = clazz.getDeclaredConstructor().newInstance();
        BeanClass beanClass =
            new BeanClass(clazz, clazz.getAnnotation(RestController.class).value());
        if (classBeansMap.containsKey(beanClass)) {
          throw new DuplicatedBeanCreationException(beanClass.clazz(), beanClass.name());
        }
        classBeansMap.put(beanClass, restControllerInstance);
        for (Method method : clazz.getMethods()) {
          if (method.isAnnotationPresent(RestMapping.class)) {
            if (Modifier.isPublic(method.getModifiers())) {
              Class<?> methodClazz = method.getReturnType();
              if (!methodClazz.equals(HttpServlet.class)) {
                throw new InvalidRestMappingClassException();
              } else {
                String uri = String.join("/", StringUtility.trimStringByString(
                        clazz.getAnnotation(RestController.class).value(), "/"),
                    StringUtility.trimStringByString(
                        method.getAnnotation(RestMapping.class).value(), "/"));
                uri = StringUtility.trimStringByString(uri, "/");

                beanClass = new BeanClass(methodClazz, uri);
                if (servletBeansMap.containsKey(uri)) {
                  throw new DuplicatedBeanCreationException(beanClass.clazz(), beanClass.name());
                }
                Object bean = method.invoke(restControllerInstance);
                servletBeansMap.put(uri, (HttpServlet) bean);
              }
            } else {
              throw new IllegalDefinedAccessControlException();
            }
          }
        }
      } else if (clazz.isAnnotationPresent(SystemCommand.class)) {
        try {
          SystemCommand systemCommandAnnotation = clazz.getAnnotation(SystemCommand.class);
          Object systemCommandInstance = clazz.getDeclaredConstructor().newInstance();
          if (systemCommandInstance instanceof AbstractSystemCommandHandler handler) {
            // manages by the class bean system
            BeanClass beanClass =
                new BeanClass(clazz, String.valueOf(systemCommandAnnotation.label()));
            if (classBeansMap.containsKey(beanClass)) {
              throw new DuplicatedBeanCreationException(beanClass.clazz(), beanClass.name());
            }
            classBeansMap.put(beanClass, systemCommandInstance);
            // add to its own management system
            handler.setCommandManager(systemCommandManager);
            systemCommandManager.registerCommand(systemCommandAnnotation.label(), handler);
          } else {
            if (isErrorEnabled()) {
              error(new IllegalArgumentException("Class " + clazz.getName() + " is not a " +
                  "AbstractSystemCommandHandler"));
            }
          }
        } catch (Exception exception) {
          if (isErrorEnabled()) {
            error(exception, "Failed to register command handler for ", clazz.getSimpleName());
          }
        }
      } else if (clazz.isAnnotationPresent(ClientCommand.class)) {
        try {
          ClientCommand clientCommandAnnotation = clazz.getAnnotation(ClientCommand.class);
          Object clientCommandInstance = clazz.getDeclaredConstructor().newInstance();
          if (clientCommandInstance instanceof AbstractClientCommandHandler<?> handler) {
            // manages by the class bean system
            BeanClass beanClass =
                new BeanClass(clazz, String.valueOf(clientCommandAnnotation.value()));
            if (classBeansMap.containsKey(beanClass)) {
              throw new DuplicatedBeanCreationException(beanClass.clazz(), beanClass.name());
            }
            classBeansMap.put(beanClass, clientCommandInstance);
            // add to its own management system
            handler.setCommandManager(clientCommandManager);

            // Add a type check before casting
            @SuppressWarnings("unchecked") // Safe cast after type verification
            var playerHandler = (AbstractClientCommandHandler<Player>) handler;
            clientCommandManager.registerCommand(clientCommandAnnotation.value(), playerHandler);
          } else {
            if (isErrorEnabled()) {
              error(new IllegalArgumentException("Class " + clazz.getName() + " is not a " +
                  "AbstractClientCommandHandler"));
            }
          }
        } catch (Exception exception) {
          if (isErrorEnabled()) {
            error(exception, "Failed to register command handler for ", clazz.getSimpleName());
          }
        }
      }
    }

    // Step 3: Make mapping between classes and their instances
    // recursively create field instance for this class instance
    classBeansMap.forEach((clazz, bean) -> {
      try {
        autowire(clazz, bean);
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException | DuplicatedBeanCreationException exception) {
        if (isErrorEnabled()) {
          String exceptionDetails =
              "Initialize class: " + clazz.clazz().getName() + "\n" + getStackTrace(exception);
          error(exceptionDetails);
        }
      }
    });
  }

  private String getStackTrace(Exception exception) {
    StringBuilder stringBuilder = new StringBuilder();
    StackTraceElement[] stackTraceElements = exception.getStackTrace();
    stringBuilder.append(exception.getClass().getName()).append(": ").append(exception.getMessage())
        .append("\n");
    for (StackTraceElement stackTraceElement : stackTraceElements) {
      stringBuilder.append("\t at ").append(stackTraceElement.toString()).append("\n");
    }
    return stringBuilder.toString();
  }

  /**
   * Retrieves an instance by using its corresponding declared interface.
   *
   * @param <T>   the returned type of interface
   * @param clazz the interface class
   * @return a bean (an instance of the interface)
   */
  @SuppressWarnings("unchecked") // Safe cast because we ensure type consistency in the scanPackages method
  public <T> T getBean(Class<T> clazz) {
    var optional = classesMap.entrySet().stream()
        .filter(entry -> entry.getValue() == clazz).findFirst();

    return optional.map(
            classClassEntry -> (T) classBeansMap.get(new BeanClass(classClassEntry.getKey(), "")))
        .orElse(null);
  }

  /**
   * Retrieves servlet beans.
   *
   * @return a {@link Map} of servlet beans
   */
  public Map<String, HttpServlet> getServletBeansMap() {
    return servletBeansMap;
  }

  /**
   * Retrieves the system command manager.
   *
   * @return an instance of {@link SystemCommandManager}
   */
  public SystemCommandManager getSystemCommandManager() {
    return systemCommandManager;
  }

  /**
   * Retrieves the client command manager.
   *
   * @return an instance of {@link ClientCommandManager}
   */
  public ClientCommandManager getClientCommandManager() {
    return clientCommandManager;
  }

  /**
   * Retrieves an instance which is declared in a class's field and put it in map of beans as well.
   *
   * @param classInterface the interface using to create a new bean
   * @param fieldName      the name of class's field that holds a reference of a bean in a class
   * @param nameQualifier  this value aims to differentiate which implemented class should be
   *                       used to create the bean (instance)
   * @param classQualifier this value aims to differentiate which implemented class should be
   *                       used to create the bean (instance)
   * @return a bean object, an instance of the implemented class
   * @throws ClassNotFoundException                        it is caused by
   *                                                       {@link #getImplementedClass(Class, String, Class)}
   * @throws NoSuchMethodException                         it is caused by
   *                                                       Class#getDeclaredConstructor(Class[])
   * @throws InvocationTargetException                     it is caused by
   *                                                       Class#getDeclaredConstructor(Class[])#newInstance()
   * @throws InstantiationException                        it is caused by
   * @throws IllegalAccessException                        it is caused by
   *                                                       Class#getDeclaredConstructor(Class[])#newInstance()
   * @throws NoImplementedClassFoundException              this exception should be thrown
   *                                                       when there is no {@link Component} annotation associated class found for the corresponding
   *                                                       declared field in a class
   * @throws MultipleImplementedClassForInterfaceException this exception would be thrown when
   *                                                       there are more than 1 {@link Component} annotation associated with classes that implement
   *                                                       a same interface
   *                                                       Class#getDeclaredConstructor(Class[])#newInstance()
   * @throws DuplicatedBeanCreationException               when a same bean was created more than one time*
   */
  private Object getBeanInstanceForInjector(Class<?> classInterface, String fieldName,
                                            String nameQualifier, Class<?> classQualifier)
      throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException,
      InstantiationException, IllegalAccessException, NoImplementedClassFoundException,
      MultipleImplementedClassForInterfaceException, DuplicatedBeanCreationException {

    // check classes annotated by @Component and fields annotated by @Autowired
    Class<?> implementedClass = getImplementedClass(classInterface, fieldName, classQualifier);

    BeanClass beanClass = new BeanClass(implementedClass, nameQualifier);
    if (classBeansMap.containsKey(beanClass)) {
      return classBeansMap.get(beanClass);
    }

    // Check if the class is in the manual classes set (created by @Bean or @BeanFactory)
    if (implementedClass != null && !manualClassesSet.contains(implementedClass)) {
      if (classBeansMap.containsKey(beanClass)) {
        throw new DuplicatedBeanCreationException(beanClass.clazz(), beanClass.name());
      }
      Object bean = implementedClass.getDeclaredConstructor().newInstance();
      classBeansMap.put(beanClass, bean);
      return bean;
    }

    return null;
  }

  private boolean isClassAnnotated(Class<?> clazz, Class<? extends Annotation>[] annotations) {
    for (Class<? extends Annotation> annotation : annotations) {
      if (clazz.isAnnotationPresent(annotation)) {
        return true;
      }
    }
    return false;
  }

  private Class<?> getImplementedClass(Class<?> classInterface, String fieldName,
                                       Class<?> classQualifier) throws ClassNotFoundException {
    var implementedClasses = classesMap.entrySet().stream()
        .filter(entry -> entry.getValue() == classInterface).collect(Collectors.toSet());

    if (implementedClasses.isEmpty()) {
      throw new NoImplementedClassFoundException(classInterface);
    } else if (implementedClasses.size() == 1) {
      // just only one implemented class for the interface
      var optional = implementedClasses.stream().findFirst();
      return optional.map(Entry::getKey).orElseThrow(ClassNotFoundException::new);
    } else {
      // multiple implemented class from the interface, need to be selected by
      // "qualifier" value
      final var findBy =
          classQualifier == null ? fieldName : classQualifier;
      var optional = implementedClasses.stream()
          .filter(entry -> entry.getKey().equals(findBy)).findAny();
      // in case of could not find an appropriately single instance, so throw an exception
      return optional.map(Entry::getKey)
          .orElseThrow(
              () -> new MultipleImplementedClassForInterfaceException(classInterface));
    }
  }

  /**
   * Assigns bean (instance) values to its corresponding fields in a class.
   *
   * @param beanClass the target class that holds declared bean fields
   * @param bean      the bean (instance) associated with the declared field
   * @throws IllegalArgumentException        it is related to the illegal argument exception
   * @throws SecurityException               it is related to the security exception
   * @throws NoSuchMethodException           it is caused by Class#getDeclaredConstructor(Class[])
   * @throws InvocationTargetException       it is caused by Class#getDeclaredConstructor(Class[])#newInstance()
   * @throws InstantiationException          it is caused by Class#getDeclaredConstructor(Class[])#newInstance()
   * @throws IllegalAccessException          it is caused by Class#getDeclaredConstructor(Class[])#newInstance()
   * @throws InstantiationException          it is caused by Class#getDeclaredConstructor(Class[])#newInstance()
   * @throws DuplicatedBeanCreationException when a same bean was created more than one time
   */
  private void autowire(BeanClass beanClass, Object bean)
      throws InstantiationException, IllegalAccessException, IllegalArgumentException,
      InvocationTargetException,
      NoSuchMethodException, SecurityException, ClassNotFoundException,
      DuplicatedBeanCreationException {
    Set<Field> fields = findFields(beanClass.clazz());
    for (Field field : fields) {
      Class<?> classQualifier = null;
      String nameQualifier = "";

      if (field.isAnnotationPresent(AutowiredQualifier.class)) {
        Class<?> classDefault = field.getAnnotation(AutowiredQualifier.class).clazz();
        if (!classDefault.equals(AutowiredQualifier.DEFAULT.class)) {
          classQualifier = classDefault;
        }
        nameQualifier = field.getAnnotation(AutowiredQualifier.class).name();
      }

      if (field.isAnnotationPresent(AutowiredAcceptNull.class)) {
        try {
          Object fieldInstance =
              getBeanInstanceForInjector(field.getType(), field.getName(), nameQualifier,
                  classQualifier);
          if (fieldInstance != null) {
            field.set(bean, fieldInstance);
            autowire(new BeanClass(fieldInstance.getClass(), nameQualifier), fieldInstance);
          }
        } catch (NoImplementedClassFoundException exception) {
          // do nothing
        }
      } else if (field.isAnnotationPresent(Autowired.class)) {
        Object fieldInstance =
            getBeanInstanceForInjector(field.getType(), field.getName(), nameQualifier,
                classQualifier);
        if (fieldInstance != null) {
          field.set(bean, fieldInstance);
          autowire(new BeanClass(fieldInstance.getClass(), nameQualifier), fieldInstance);
        }
      }
    }
  }

  /**
   * Retrieves all the fields annotated by {@link Autowired} or {@link AutowiredAcceptNull}
   * annotation.
   *
   * @param clazz a target class
   * @return a set of fields in the class
   */
  private Set<Field> findFields(Class<?> clazz) {
    Set<Field> fields = new HashSet<>();

    while (clazz != null) {
      for (Field field : clazz.getDeclaredFields()) {
        if (field.isAnnotationPresent(Autowired.class)
            || field.isAnnotationPresent(AutowiredAcceptNull.class)) {
          field.setAccessible(true);
          fields.add(field);
        }
      }
      // make recursion
      clazz = clazz.getSuperclass();
    }

    return fields;
  }

  /**
   * Clear all references and beans created by the injector.
   */
  private void reset() {
    classesMap.clear();
    classBeansMap.clear();
    manualClassesSet.clear();
    servletBeansMap.clear();
    systemCommandManager.clear();
    clientCommandManager.clear();
  }
}
