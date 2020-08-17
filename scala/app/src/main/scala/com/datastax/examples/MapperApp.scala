/*
 * Copyright DataStax, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datastax.examples

import java.net.URI
import java.nio.file.{FileSystems, Files, Paths}
import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset}
import java.util.{Collections, UUID}

import com.datastax.examples.mapper.killrvideo.KillrVideoMapperBuilder
import com.datastax.examples.mapper.killrvideo.user.{User, UserDao}
import com.datastax.examples.mapper.killrvideo.video._
import com.datastax.oss.driver.api.core.cql.SimpleStatement
import com.datastax.oss.driver.api.core.{CqlIdentifier, CqlSession, PagingIterable}

import scala.jdk.CollectionConverters._
import scala.util.Using

object MapperApp {

  private val KeyspaceId = CqlIdentifier.fromCql("killrvideo")

  def main(args: Array[String]): Unit = {
    Using.resource(CqlSession.builder().build()) { session =>

      maybeCreateSchema(session)

      val mapper = new KillrVideoMapperBuilder(session).withDefaultKeyspace(KeyspaceId).build()

      // Create a new user
      val userDao: UserDao = mapper.userDao()

      var user: User = User(UUID.randomUUID, "test", "user", "testuser@example.com", Instant.now)

      if (userDao.create(user, "password123".toCharArray)) {
        println(s"Created $user")
      } else {
        user = userDao.getByEmail("testuser@example.com")
        println(s"Reusing existing $user")
      }

      // Creating another user with the same email should fail
      assert(!userDao.create(User(UUID.randomUUID, "test2", "user", "testuser@example.com", Instant.now),
        "secret123".toCharArray))

      // Simulate login attempts
      tryLogin(userDao, "testuser@example.com", "password123")
      tryLogin(userDao, "testuser@example.com", "secret123")

      // Insert a video
      val videoDao: VideoDao = mapper.videoDao()

      val video: Video = videoDao.create(
        Video(videoid = null,
          user.userid,
          name = "Accelerate: A NoSQL Original Series (TRAILER)",
          description = null,
          location = "https://www.youtube.com/watch?v=LulWy8zmrog",
          locationType = 0,
          previewImageLocation = null,
          tags = Set("apachecassandra", "nosql", "hybridcloud").asJava,
          addedDate = null
        ))

      println(s"Created video [${video.videoid}] ${video.name}")

      // Check that associated denormalized tables have also been updated:
      val userVideos: PagingIterable[UserVideo] = videoDao.getByUser(user.userid)
      println(s"Videos for ${user.firstname} ${user.lastname}:")
      // Note: using `.all()` for convenience since we know there are few results, but this is
      // generally not a good idea. See the javadocs.
      userVideos.all().asScala.foreach(video => println(s"  [${video.videoid}] ${video.name}"))

      val latestVideos: PagingIterable[LatestVideo] = videoDao.getLatest(todaysTimestamp)
      println("Latest videos:")
      latestVideos.all().asScala.foreach(video => println(s"  [${video.videoid}] ${video.name}"))

      val videosByTag: PagingIterable[VideoByTag] = videoDao.getByTag("apachecassandra")
      println("Videos tagged with apachecassandra:")
      videosByTag.all().asScala.foreach(video => println(s"  [${video.videoid}] ${video.name}"))

      // Update the existing video:
      // Creating an update template looks a bit weird in Scala, we have to leave all unimpacted
      // fields to null to take advantage of the null saving strategy
      val template: Video = Video(
        video.videoid,
        name = "Accelerate: A NoSQL Original Series - join us online!",
        userid = null, description = null, location = null, locationType = video.locationType,
        previewImageLocation = null, tags = null, addedDate = null
      )
      videoDao.update(template)
      // Reload the whole entity and check the fields
      val newVideo = videoDao.get(video.videoid)
      println(s"Updated name for video ${newVideo.videoid}: ${newVideo.name}")
    }
  }

  private def tryLogin(userDao: UserDao, email: String, password: String): Unit = {
    val maybeUser = userDao.login(email, password.toCharArray)
    val outcome = if (maybeUser.isPresent) "Success" else "Failure"
    println(s"Logging in with $email/$password: $outcome")
  }

  private def maybeCreateSchema(session: CqlSession) {
    session.execute(
      SimpleStatement.newInstance(
        "CREATE KEYSPACE IF NOT EXISTS killrvideo WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}")
        .setExecutionProfileName("slow"))
    session.execute("USE killrvideo")
    for (statement <- getStatements("killrvideo_schema.cql")) {
      session.execute(SimpleStatement.newInstance(statement).setExecutionProfileName("slow"))
    }
  }

  private def getStatements(fileName: String): List[String] = {
    val uri = MapperApp.getClass.getClassLoader.getResource(fileName).toURI
    val path = if (uri.toString contains "!") {
      // This happens when the file is in a packaged JAR
      val Array(fs, file) = uri.toString.split('!')
      FileSystems.newFileSystem(URI.create(fs), Collections.emptyMap[String, Any])
        .getPath(file)
    } else {
      Paths.get(uri)
    }
    Files.readString(path).split(";")
      .map(_.trim)
      .filter(!_.isEmpty)
      .toList
  }

  /**
   * KillrVideo uses a textual timestamp to partition recent video. Build the timestamp for today to
   * fetch our latest insertions.
   */
  private def todaysTimestamp = DateTimeFormatter.ofPattern("yyyyMMdd")
    .withZone(ZoneOffset.UTC)
    .format(Instant.now)
}
