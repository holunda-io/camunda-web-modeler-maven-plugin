# Camunda 8 Web Modeler Maven Plugin

This plugin provides an easy to use way of downloading files from the Camunda 8 Web Modeler via REST-Api
into your source code for version control.

## Usage

```xml  

<build>
    <plugins>
        <plugin>
            <groupId>io.holunda</groupId>
            <artifactId>camunda-web-modeler-maven-plugin</artifactId>
            <version>${camunda-web-modeler-maven-plugin.version}</version>
        </plugin>
    </plugins>
</build>

```
Either execute in your maven lifecycle (defaultPhase = COMPILE)
```xml
<executions>
    <execution>
        <phase>compile</phase>
        <goals>
            <goal>bpmn-download</goal>
        </goals>
    </execution>
</executions>
```


or via Command
```
mvn io.holunda:camunda-web-modeler-maven-plugin:your-version:bpmn-download
```


## Configuration

| value        | description                                                                                                                       |
|--------------|-----------------------------------------------------------------------------------------------------------------------------------|
| clientId     | ClientId for your Camunda Cluster                                                                                                 |
| clientSecret | ClientSecret for you Camunda Cluster                                                                                              |
| path         | Path for the downloaded files, defaults to `${basedir}/src/main/resources`. The directory must exists before executing the plugin |
| documents    | List of documents to download                                                                                                     |
| name         | Name of the document to download                                                                                                  |
| milestone    | Optional - If provided the content of the related milestone gets saved. If not provided, the latest version of the file is saved  |

Example:
```xml

<configuration>
    <clientId>my-client-id</clientId>
    <clientSecret>my-client-secret</clientSecret>
    <path>${basedir}/src/main/resources/process</path> 
    <documents>
        <document>
            <name>File-1</name>
            <mileStone>v1.0.0</mileStone>
        </document>
        <document>
            <name>File-2</name>
        </document>
    </documents>
</configuration>
```


## Limitations
Currently only the SaaS Version of Camunda-8 Web Modeler is supported.
