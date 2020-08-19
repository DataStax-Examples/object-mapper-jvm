# Object mapper in alternative JVM tools and languages

The [Java DataStax Driver](https://docs.datastax.com/en/developer/java-driver/latest/) comes with an
object mapper that removes boilerplate of writing queries and lets you focus on your application
objects. This example shows how to use the mapper with various "alternative" JVM tools and
languages.

**These examples rely on Java driver 4.9.0, which is still in development at the time of writing.**

See each subdirectory for more explanations:

* [kotlin/](kotlin/): models entities with Kotlin data classes, builds with Gradle.
* [scala/](scala/): models entities with Scala case classes, builds with sbt.
* [lombok/](lombok/): models entities with Java classes annotated with Lombok.
* [record/](record/): models entities with Java 14 records.

See also the [object-mapper-java](https://github.com/DataStax-Examples/object-mapper-java) project
for a standard example using JavaBeans.

Contributor(s): [Olivier Michallat](https://github.com/olim7t)

