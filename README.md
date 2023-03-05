# findclass-maven-plugin
  **Originating from [spring-boot#27855](https://github.com/spring-projects/spring-boot/pull/27855), this plugin was built.**  
  **In addition to being able to locate the **main-class**, it also supports more matching patterns to locate other classes.**  

## Dependency  
```xml
<plugin>
    <groupId>io.github.kerwin612</groupId>
    <artifactId>findclass-maven-plugin</artifactId>
    <version>0.0.2</version>
</plugin>
```
**DefaultPhase: `process-classes`**

## Example

**pom.xml**:  
```xml
<build>
    <pluginManagement>
        <plugins>
            <plugin>
                <groupId>io.github.kerwin612</groupId>
                <artifactId>findclass-maven-plugin</artifactId>
                <version>0.0.2</version>
                <configuration>
                    <classAnnotationMatch>org.springframework.boot.autoconfigure.SpringBootApplication</classAnnotationMatch>
                    <methodKeyMatch>public static void .*\.main\(java.lang.String\[\]\).*</methodKeyMatch>
                    <firstFound>true</firstFound>
                    <outputPropertyName>foundMainClass</outputPropertyName>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.1.0</version>
                <configuration>
                    <mainClass>${foundMainClass}</mainClass>
                </configuration>
            </plugin>
        </plugins>
    </pluginManagement>

    <defaultGoal>findclass:find exec:java@run</defaultGoal>
    
    <plugins>
        <plugin>
            <groupId>io.github.kerwin612</groupId>
            <artifactId>findclass-maven-plugin</artifactId>
            <executions>
                <execution>
                    <id>find-main-class</id>
                    <goals>
                        <goal>find</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>exec-maven-plugin</artifactId>
            <executions>
                <execution>
                    <id>run</id>
                    <goals>
                        <goal>java</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```  

**Application.java**:  
```java
package io.github.kerwin612.example;
...
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        ...
    }

}
```

**run: `mvn -Dexec.args="..."`**
* **the **Applicaton** will be located and the path will be assigned to the **foundMainClass** property**
* The value [**io.github.kerwin612.example.Application**] can be obtained through the property [**foundMainClass**]

# Configuration

| **name** | **description** | **required** | **default value** |
| --- | --- | :---: | --- |
| `classesDirectory` | Locate classes based on this directory | Y | `${project.build.outputDirectory}` |
| `outputPropertyName` | Assign the located class to this property | Y | **foundClass** |
| `classAnnotationMatch` | Locate classes based on annotations on the class | N | - |
| `classParentMatch` | Locate classes based on extend the parent class | N | - |
| `methodKeyMatch` | Locate classes based on contains the method signature | N | - |
| `firstFound` | Whether to return only the first match if targeting multiple matching classes | N | **false** |
| `required` | Whether it is necessary to locate a class that meets the rules, otherwise an exception will be thrown | N | **false** |
