# Object Mapper with Java 14 records

The [Java DataStax Driver](https://docs.datastax.com/en/developer/java-driver/latest/) comes with an
object mapper that removes boilerplate of writing queries and lets you focus on your application
objects. Java
[Records](https://docs.oracle.com/en/java/javase/14/docs/api/java.base/java/lang/Record.html) are
shallowly immutable, transparent carriers for a fixed set of values. This example shows how to use
the two together.

Note: records are a **preview feature** of Java 14. As such the mapper's support for them is also
provided as a preview.

Contributor(s): [Olivier Michallat](https://github.com/olim7t)

## Objectives

* Demonstrate how to map Java records as immutable entities.

## Project layout

There is a single source file: [MapperApp](src/main/java/com/datastax/examples/MapperApp.java).
For concision, we define the mapped types (entity, DAO and mapper) as nested types, but they could
equally be top-level types.

## How this Sample works

Unlike other mapper examples, the model is very simple: there is only a single table. Our goal is
to show the specifics of Java records integration, not demonstrate every mapper feature (see the
[original Java example](https://github.com/DataStax-Examples/object-mapper-java) for a more complete
scenario).

Sources must be compiled using Java 14 with the `--enable-preview` flag. See the compiler plugin
configuration in [pom.xml](pom.xml).

For additional information and details on how to use the Mapper classes please refer to the
documentation available
[here](https://docs.datastax.com/en/developer/java-driver/latest/manual/mapper/).

## Setup and Running

### Prerequisites

* JDK 14
* Java driver 4.9 or above (configured in [pom.xml](pom.xml))
* A Cassandra cluster is running and accessible through the contacts points and data center
  identified in [application.conf](src/main/resources/application.conf)

#### Building

At the project root level:

```
mvn clean package
```

This builds the JAR file located at `target/object-mapper-record-1.0.jar`

#### Run the program

To run this application, use the following command:

```
java --enable-preview -jar target/object-mapper-record-1.0.jar
```

Note that, like for compilation, you also need to pass the `--enable-preview` flag when you start
the VM.

This will produce results similar to those below.

```
Saving Product[id=1, description=test]...
Retrieved Product[id=1, description=test]
```
