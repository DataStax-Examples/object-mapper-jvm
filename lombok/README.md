# Object Mapper with Lombok

The [Java DataStax Driver](https://docs.datastax.com/en/developer/java-driver/latest/) comes with an
object mapper that removes boilerplate of writing queries and lets you focus on your application
objects. [Lombok](https://projectlombok.org/) is a popular library that automates boilerplate code,
such as getters and setters. This example shows how to use the two together.

Contributor(s): [Olivier Michallat](https://github.com/olim7t)

## Objectives

* Demonstrate how to map classes annotated with Lombok's `@Data` as mutable entities.
* Demonstrate how to map classes annotated with Lombok's `@Value` as immutable entities.
* Demonstrate how to configure the Lombok and Object Mapper annotation processors in conjunction in
  the build.

## Project layout

There is a single source file: [MapperApp](src/main/java/com/datastax/examples/MapperApp.java).
For concision, we define the mapped types (entities, DAO and mapper) as nested types, but they
could equally be top-level types.

## How this Sample works

Unlike other mapper examples, the model is very simple: there is only a single table. Our goal is
to show the specifics of the Lombok integration, not demonstrate every mapper feature (see the
[original Java example](https://github.com/DataStax-Examples/object-mapper-java) for a more complete
scenario).

We show both a mutable entity (`Product`), and an immutable one (`ImmutableProduct`). In practice,
you would probably choose the style that best fits your programming style, and use it consistently
throughout your model.

The build integration is configured in [pom.xml](pom.xml). One important detail is that the Lombok
annotation processor must run _before_ the mapper's.

For additional information and details on how to use the Mapper classes please refer to the
documentation available
[here](https://docs.datastax.com/en/developer/java-driver/latest/manual/mapper/).

## Setup and Running

### Prerequisites

* JDK 8 - 14
* Java driver 4.9 or above (configured in [pom.xml](pom.xml)). Note that this is only required for
  immutable entities (`@Value`). If you only use mutable entities (`@Data`), any driver version
  above 4.1 will work.
* A Cassandra cluster is running and accessible through the contacts points and data center
  identified in [application.conf](src/main/resources/application.conf)

#### Building

At the project root level

```
mvn clean package
```

This builds the JAR file located at `target/object-mapper-lombok-1.0.jar`

#### Run the program

To run this application, use the following command:

```
java -jar target/object-mapper-lombok-1.0.jar
```

This will produce results similar to those below.

```
Saving MapperApp.Product(id=1, description=test)...
Retrieved MapperApp.Product(id=1, description=test)
Retrieved MapperApp.ImmutableProduct(id=1, description=test)
```
