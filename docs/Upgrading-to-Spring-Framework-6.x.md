# [Upgrading to Version 6.1](https://github.com/spring-projects/spring-framework/wiki/Upgrading-to-Spring-Framework-6.x#parameter-name-retention:)
## Baseline upgrades
Spring Framework 6.1 raises its minimum requirements with the following libraries:
 * SnakeYAML 2.0
 * Jackson 2.14
 * Kotlin Coroutines 1.7
 * Kotlin Serialization 1.5

## Removed APIs
Several deprecated classes, constructors, and methods have been removed across the code base. See [29449](https://github.com/spring-projects/spring-framework/issues/29449) and [30604}(https://github.com/spring-projects/spring-framework/issues/30604).

RPC-style remoting that has been officially and/or effectively deprecated for several years has been removed. This impacts Hessian, HTTP Invoker, JMS Invoker, and JAX-WS support, see [27422](https://github.com/spring-projects/spring-framework/issues/27422).

EJB access has also been removed as part of this effort. If you need to lookup an EJB, use JNDI directly via `JndiObjectFactoryBean` or `<jee:jndi-lookup>`.


## Parameter Name Retention
`LocalVariableTableParameterNameDiscoverer` has been removed in 6.1. 
Consequently, code within the Spring Framework and Spring portfolio frameworks no longer attempts to deduce parameter names by parsing bytecode.
If you experience issues with dependency injection, property binding, SpEL expressions, or other use cases that depend on the names of parameters,
you should compile your Java sources with the common Java 8+ `-parameters` flag for parameter name retention (instead of relying on the `-debug` compiler flag) in order to be compatible with `StandardReflectionParameterNameDiscoverer`. 
The Groovy compiler also supports a -parameters flag for the same purpose. With the Kotlin compiler, use the `-java-parameters` flag.


Maven users need to configure the maven-compiler-plugin for Java source code:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <parameters>true</parameters>
    </configuration>
</plugin>
```

Gradle users need to configure the JavaCompile task for Java source code, either with the Kotlin DSL:
```
tasks.withType<JavaCompile>() {
    options.compilerArgs.add("-parameters")
}
```

Or the Groovy DSL:
```
tasks.withType(JavaCompile).configureEach {
    options.compilerArgs.add("-parameters")
}
```

Similarly, Gradle users need to configure the GroovyCompile task for Groovy source code, either with the Kotlin DSL:
```
tasks.withType<GroovyCompile>() {
    groovyOptions.parameters = true
}
```

Or the Groovy DSL:
```
tasks.withType(GroovyCompile).configureEach {
    groovyOptions.parameters = true
}
```


Sometimes it is also necessary to manually configure your IDE.
In IntelliJ IDEA, open `Settings` and add `-parameters` to the following field.
 * Build, Execution, Deployment → Compiler → Java Compiler → Additional command line parameters
In Eclipse IDE, open `Preferences` and activate the following checkbox.
 * Java → Compiler → Store information about method parameters (usable via reflection)
In VSCode, edit or add the `.settings/org.eclipse.jdt.core.prefs` file with the following content:
```
org.eclipse.jdt.core.compiler.codegen.methodParameters=generate
```