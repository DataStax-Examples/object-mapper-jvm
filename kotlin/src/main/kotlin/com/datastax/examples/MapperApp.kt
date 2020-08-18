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

import com.datastax.examples.mapper.killrvideo.KillrVideoMapper
import com.datastax.examples.mapper.killrvideo.user.User
import com.datastax.examples.mapper.killrvideo.user.UserDao
import com.datastax.examples.mapper.killrvideo.video.*
import com.datastax.oss.driver.api.core.CqlIdentifier
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.PagingIterable
import com.datastax.oss.driver.api.core.cql.SimpleStatement
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

object MapperApp {

    private val KEYSPACE_ID = CqlIdentifier.fromCql("killrvideo")

    @JvmStatic
    fun main(args: Array<String>) {
        CqlSession.builder().build().use { session ->
            maybeCreateSchema(session)
            val mapper: KillrVideoMapper = KillrVideoMapper.builder(session).withDefaultKeyspace(KEYSPACE_ID).build()
            // Create a new user
            val userDao: UserDao = mapper.userDao()
            var user = User(UUID.randomUUID(), "test", "user", "testuser@example.com", Instant.now())
            if (userDao.create(user, "password123".toCharArray())) {
                println("Created $user")
            } else {
                // This should be non null if the creation failed
                user = UserDao.getByEmail(userDao, "testuser@example.com")!!
                println("Reusing existing $user")
            }
            assert(!userDao.create(
                    User(UUID.randomUUID(), "test2", "user", "testuser@example.com", Instant.now()),
                    "secret123".toCharArray()))
            // Simulate login attempts
            tryLogin(userDao, "testuser@example.com", "password123")
            tryLogin(userDao, "testuser@example.com", "secret123")
            // Insert a video
            val videoDao: VideoDao = mapper.videoDao()
            val video = videoDao.create(
                    Video(userid = user.userid,
                            name = "Accelerate: A NoSQL Original Series (TRAILER)",
                            location = "https://www.youtube.com/watch?v=LulWy8zmrog",
                            tags = setOf("apachecassandra", "nosql", "hybridcloud"),
                            videoid = null, addedDate = null, description = null,
                            locationType = null, previewImageLocation = null))

            println("Created video [${video.videoid}] ${video.name}")
            // Check that associated denormalized tables have also been updated:
            val userVideos: PagingIterable<UserVideo> = videoDao.getByUser(user.userid!!)
            println("Videos for ${user.firstname} ${user.lastname}:")
            for (userVideo in userVideos) {
                println("  [${userVideo.videoid}] ${userVideo.name}")
            }
            val latestVideos: PagingIterable<LatestVideo> = videoDao.getLatest(todaysTimestamp())
            println("Latest videos:")
            for (latestVideo in latestVideos) {
                println("  [${latestVideo.videoid}] ${latestVideo.name}")
            }
            val videosByTag: PagingIterable<VideoByTag> = videoDao.getByTag("apachecassandra")
            println("Videos tagged with apachecassandra:")
            for (videoByTag in videosByTag) {
                println("  [${videoByTag.videoid}] ${videoByTag.name}")
            }
            // Update the existing video:
            val template = Video(videoid = video.videoid,
                    name = "Accelerate: A NoSQL Original Series - join us online!",
                    userid = null, location = null, tags = null, addedDate = null, description = null,
                    locationType = null, previewImageLocation = null)
            videoDao.update(template)
            // Reload the whole entity and check the fields
            val updatedVideo = videoDao.get(video.videoid!!)
            println("Updated name for video ${updatedVideo?.videoid}: ${updatedVideo?.name}")
        }
    }

    private fun tryLogin(userDao: UserDao, email: String, password: String) {
        val maybeUser: Optional<User> = userDao.login(email, password.toCharArray())
        val outcome = if (maybeUser.isPresent) "Success" else "Failure"
        println("Logging in with $email/$password: $outcome")
    }

    @Throws(Exception::class)
    private fun maybeCreateSchema(session: CqlSession) {
        session.execute(
                SimpleStatement.newInstance(
                        "CREATE KEYSPACE IF NOT EXISTS killrvideo WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}")
                        .setExecutionProfileName("slow"))
        session.execute("USE killrvideo")
        for (statement in getStatements("killrvideo_schema.cql")) {
            session.execute(SimpleStatement.newInstance(statement).setExecutionProfileName("slow"))
        }
    }

    @Throws(Exception::class)
    private fun getStatements(fileName: String): List<String> {
        val uri = this.javaClass.getClassLoader().getResource(fileName)!!.toURI()
        val path = if (uri.toString().contains("!")) {
            // This happens when the file is in a packaged JAR
            val (fs, file) = uri.toString().split("!")
            FileSystems.newFileSystem(URI.create(fs), emptyMap<String, Any>())
                    .getPath(file)
        } else {
            Paths.get(uri)
        }
        return Files.readString(path).split(";")
                .map(String::trim)
                .filter(String::isNotEmpty)
                .toList()
    }

    /**
     * KillrVideo uses a textual timestamp to partition recent video. Build the timestamp for today to
     * fetch our latest insertions.
     */
    private fun todaysTimestamp(): String {
        return DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC).format(Instant.now())
    }
}
