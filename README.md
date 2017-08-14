# Oh loco, meu! (Maven Plugin)

## Modo de uso

```xml
<build>
  <plugins>
    <plugin>
      <groupId>br.com.involves</groupId>
      <artifactId>ohlocomeu</artifactId>
      <version>0.1.0-SNAPSHOT</version>
      <executions>
        <execution>
          <id>en-properties</id>
          <goals>
            <goal>i18n</goal>
          </goals>
          <configuration>
            <outputDir>{$project.build.directory}/i18n/</outputDir>
            <locales>en</locales>
            <types>properties</types>
            <namePrefix>msg_</namePrefix>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```
