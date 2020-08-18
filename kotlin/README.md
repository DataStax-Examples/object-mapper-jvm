# Object mapper in Kotlin

## Objectives

* Demonstrate how to use the Java Driver object mapper to replace the tedious work of DAO recreation
  in Kotlin. Reference the
  [documentation](https://docs.datastax.com/en/developer/java-driver/latest/manual/mapper/mapper/)
  for details about the object mapper.
* Demonstrate how to model entities as Kotlin data classes.
* Demonstrate how to integrate the annotation processor in a Gradle build.

## Project layout

* [MapperApp.kt](src/main/kotlin/com/datastax/examples/MapperApp.kt) - The main application
  file that uses the entities and DAOs:
  [User](src/main/kotlin/com/datastax/examples/mapper/killrvideo/user) and
  [Video](src/main/kotlin/com/datastax/examples/mapper/killrvideo/video).
* [build.gradle](build.gradle): the Gradle file that configures the dependencies and the annotation
  processor.

## How this sample works

The driver provides a simple object mapper which allows us to avoid writing much of the boilerplate
code required to map query results to and from POJOs.

To use the mapper requires that several components be annotated in such a way as to allow them to
work together:

* Mapper - This is the entry point for this mapper which wraps the core driver session and acts as a
  factory for creating the DAO objects.  In this example the mapper is
  [KillrVideoMapper](src/main/kotlin/com/datastax/examples/mapper/killrvideo/KillrVideoMapper.kt).
* DAO - Acts as the interface which defines the set of operations which can be performed on an
  entity.  In this example
  [VideoDao](src/main/kotlin/com/datastax/examples/mapper/killrvideo/video/VideoDao.kt) and
  [UserDao](src/main/kotlin/com/datastax/examples/mapper/killrvideo/user/UserDao.kt) are DAO
  classes.
* Entity - This is a class which will be mapped to a Cassandra table or UDT.  In this example
  [Video](src/main/kotlin/com/datastax/examples/mapper/killrvideo/video/Video.kt),
  [VideoByTag](src/main/kotlin/com/datastax/examples/mapper/killrvideo/video/VideoByTag.kt),  and
  [User](src/main/kotlin/com/datastax/examples/mapper/killrvideo/user/User.kt) are Entity classes
* Query Provider - These provide a method to define queries which cannont be expressed as static
  strings. In this example
  [CreateVideoQueryProvider](src/main/kotlin/com/datastax/examples/mapper/killrvideo/video/CreateVideoQueryProvider.kt),
  [LoginQueryProvider](src/main/kotlin/com/datastax/examples/mapper/killrvideo/user/LoginQueryProvider.kt)
  and
  [CreateUserQueryProvider](src/main/kotlin/com/datastax/examples/mapper/killrvideo/user/CreateUserQueryProvider.kt)
  are Query Provider classes.

Note that the mapped data model is exactly the same as the [original Java
mapper example](https://github.com/DataStax-Examples/object-mapper-java).

For additional information and details on how to use the Mapper classes please refer to the documentation available
[here](https://docs.datastax.com/en/developer/java-driver/latest/manual/mapper/).

## Setup and running

### Prerequisites

* JDK 11 - 13 (Gradle seems to have an issue with JDK 14).
* Java driver 4.9 or above (configured in [build.gradle](build.gradle)). Note that this is only
  required for immutable data classes (components declared with `val`). If you use mutable data
  classes (components declared with `var`), any driver version above 4.1 will work.
* A Cassandra cluster is running and accessible through the contacts points and data center
  identified in [application.conf](src/main/resources/application.conf)

#### Building and running

At the project root level:

```
./gradlew run
```

This will produce results similar to those below:

```
Created User(userid=31af778f-a504-4bbe-94ef-1af17dfa5260, firstname=test, lastname=user, email=testuser@example.com, createdDate=2020-08-18T15:05:10.054037Z)
Logging in with testuser@example.com/password123: Success
Logging in with testuser@example.com/secret123: Failure
Created video [8221c9a4-878f-49e0-9949-e252d6740884] Accelerate: A NoSQL Original Series (TRAILER)
Videos for test user:
  [8221c9a4-878f-49e0-9949-e252d6740884] Accelerate: A NoSQL Original Series (TRAILER)
Latest videos:
  [8221c9a4-878f-49e0-9949-e252d6740884] Accelerate: A NoSQL Original Series (TRAILER)
Videos tagged with apachecassandra:
  [8221c9a4-878f-49e0-9949-e252d6740884] Accelerate: A NoSQL Original Series (TRAILER)
Updated name for video 8221c9a4-878f-49e0-9949-e252d6740884: Accelerate: A NoSQL Original Series - join us online!
```
