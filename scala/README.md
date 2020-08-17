# Object mapper in Scala

## Objectives

* Demonstrate how to use the Java Driver object mapper to replace the tedious work of DAO recreation
  in Scala. Reference the
  [documentation](https://docs.datastax.com/en/developer/java-driver/latest/manual/mapper/mapper/)
  for details about the object mapper.
* Demonstrate how to model entities as Scala case classes, and DAOs and mappers as Scala traits.
* Demonstrate how to integrate the annotation processor in an sbt build.

## Project layout

* [MapperApp.scala](app/src/main/scala/com/datastax/examples/MapperApp.scala) - The main application
  file that uses the entities and DAOs defined in the [model](model/) subproject:
  [User](model/src/main/scala/com/datastax/examples/mapper/killrvideo/user) and
  [Video](model/src/main/scala/com/datastax/examples/mapper/killrvideo/video).
* [build.sbt](build.sbt) and [project/Dependencies.scala](project/Dependencies.scala): the sbt files
  that configure the dependencies and integrate the annotation processor to the Scala build.

## How this sample works

Scala does not support annotation processing natively, so the mapper processor cannot operate on the
Scala sources directly. But it can process the compiled class files output by the Scala compiler.

So the compilation of the `model` subproject happens in 3 phases:

1. Compile the Scala sources with the regular sbt task. This puts the class files in
  `model/target/scala-2.13/classes`.
2. Execute a custom task that runs the annotation processor (`javac -proc:only ...`) on the compiled
   class files. This generates a set of Java classes in
   `model/target/scala-2.13/src_managed/main/mapper`.
3. Execute another custom task that compiles those Java classes back into
   `model/target/scala-2.13/classes`.

The `model` subproject **cannot** reference generated types anywhere, otherwise we would get a
compile error during phase 1, where those types don't exist yet. In particular, this is why we
don't have a `KillrVideoMapper.builder()` utility method like in other languages. This is also why
the application code lives in a separate `app` subproject.

Apart from that, the mapped data model is exactly the same as the [original Java mapper
example](https://github.com/DataStax-Examples/object-mapper-java):

* Mapper - This is the entry point for this mapper which wraps the core driver session and acts as a
  factory for creating the DAO objects.  In this example the mapper is
  [KillrVideoMapper](model/src/main/scala/com/datastax/examples/mapper/killrvideo/KillrVideoMapper.scala).
* DAO - Acts as the interface which defines the set of operations which can be performed on an
  entity.  In this example
  [VideoDao](model/src/main/scala/com/datastax/examples/mapper/killrvideo/video/VideoDao.scala) and
  [UserDao](model/src/main/scala/com/datastax/examples/mapper/killrvideo/user/UserDao.scala) are DAO
  classes.
* Entity - This is a class which will be mapped to a Cassandra table or UDT.  In this example
  [Video](model/src/main/scala/com/datastax/examples/mapper/killrvideo/video/Video.scala),
  [VideoByTag](model/src/main/scala/com/datastax/examples/mapper/killrvideo/video/VideoByTag.scala),
  and [User](model/src/main/scala/com/datastax/examples/mapper/killrvideo/user/User.scala) are
  Entity classes
* Query Provider - These provide a method to define queries which cannont be expressed as static
  strings. In this example
  [CreateVideoQueryProvider](model/src/main/scala/com/datastax/examples/mapper/killrvideo/video/CreateVideoQueryProvider.scala),
  [LoginQueryProvider](model/src/main/scala/com/datastax/examples/mapper/killrvideo/user/LoginQueryProvider.scala)
  and
  [CreateUserQueryProvider](model/src/main/scala/com/datastax/examples/mapper/killrvideo/user/CreateUserQueryProvider.scala)
  are Query Provider classes.

Notes:
* the main goal of this example is to solve the build issues and show how to declare the
  annotations. Other parts of the code have been copied literally and are not very idiomatic (usage
  of Java collections, `null`, etc).
* the author is not proficient in sbt. The build works, but there are probably nicer ways to
  implement the custom tasks, see the TODOs in `build.sbt`. If you want to contribute improvements,
  feel free to open a pull request.

## Setup and Running

### Prerequisites

* JDK 11 - 14
* Java driver 4.9 or above (configured in [project/Dependencies.scala](project/Dependencies.scala))
* A Cassandra cluster is running and accessible through the contacts points and data center
  identified in [application.conf](app/src/main/resources/application.conf)

#### Building and running

At the project root level:

```
sbt "clean ; app/run"
```

This will produce results similar to those below:

```
Created User(b6b45734-979a-4976-b58c-13ba4d57702b,test,user,testuser@example.com,2020-08-18T15:27:00.919058Z)
Logging in with testuser@example.com/password123: Success
Logging in with testuser@example.com/secret123: Failure
Created video [bc92bd85-533d-4372-8575-64571b45d5e4] Accelerate: A NoSQL Original Series (TRAILER)
Videos for test user:
  [bc92bd85-533d-4372-8575-64571b45d5e4] Accelerate: A NoSQL Original Series (TRAILER)
Latest videos:
  [bc92bd85-533d-4372-8575-64571b45d5e4] Accelerate: A NoSQL Original Series (TRAILER)
Videos tagged with apachecassandra:
  [bc92bd85-533d-4372-8575-64571b45d5e4] Accelerate: A NoSQL Original Series (TRAILER)
Updated name for video bc92bd85-533d-4372-8575-64571b45d5e4: Accelerate: A NoSQL Original Series - join us online!
```
