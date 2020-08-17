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
package com.datastax.examples.mapper.killrvideo.video

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZoneOffset}
import java.util.UUID

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.{BatchStatement, DefaultBatchType, PreparedStatement}
import com.datastax.oss.driver.api.mapper.MapperContext
import com.datastax.oss.driver.api.mapper.entity.EntityHelper
import com.datastax.oss.driver.api.mapper.entity.saving.NullSavingStrategy

import scala.jdk.CollectionConverters._

/**
 * Provides the implementation of {@link VideoDao#create}.
 *
 * <p>Package-private visibility is sufficient, this will be called only from the generated DAO
 * implementation.
 */
class CreateVideoQueryProvider(val context: MapperContext,
                               val videoHelper: EntityHelper[Video],
                               val userVideoHelper: EntityHelper[UserVideo],
                               val latestVideoHelper: EntityHelper[LatestVideo],
                               val videoByTagHelper: EntityHelper[VideoByTag]) {

  private val session = context.getSession
  private val preparedInsertVideo =
    CreateVideoQueryProvider.prepareInsert(session, videoHelper)
  private val preparedInsertUserVideo =
    CreateVideoQueryProvider.prepareInsert(session, userVideoHelper)
  private val preparedInsertLatestVideo =
    CreateVideoQueryProvider.prepareInsert(session, latestVideoHelper)
  private val preparedInsertVideoByTag =
    CreateVideoQueryProvider.prepareInsert(session, videoByTagHelper)

  def create(video: Video): Video = {
    val videoWithDefaults = video.copy(
      videoid = if (video.videoid == null) UUID.randomUUID() else video.videoid,
      addedDate = if (video.addedDate == null) Instant.now else video.addedDate
    )
    insertInAllViews(videoWithDefaults)
    videoWithDefaults
  }

  private def insertInAllViews(video: Video) {
    val batch = BatchStatement.builder(DefaultBatchType.LOGGED)
    batch.addStatement(CreateVideoQueryProvider.bind(preparedInsertVideo, video, videoHelper))
    batch.addStatement(
      CreateVideoQueryProvider.bind(preparedInsertUserVideo,
        CreateVideoQueryProvider.toUserVideo(video),
        userVideoHelper))
    batch.addStatement(
      CreateVideoQueryProvider.bind(preparedInsertLatestVideo,
        CreateVideoQueryProvider.toLatestVideo(video),
        latestVideoHelper))
    if (video.tags != null) {
      for (tag <- video.tags.asScala) {
        batch.addStatement(CreateVideoQueryProvider.bind(preparedInsertVideoByTag, CreateVideoQueryProvider.toVideoByTag(video, tag), videoByTagHelper))
      }
    }
    session.execute(batch.build)
  }
}

private[video] object CreateVideoQueryProvider {

  def prepareInsert[T](session: CqlSession, entityHelper: EntityHelper[T]) =
    session.prepare(entityHelper.insert.asCql)

  def bind[T](preparedStatement: PreparedStatement, entity: T, entityHelper: EntityHelper[T]) = {
    val boundStatement = preparedStatement.boundStatementBuilder()
    entityHelper.set(entity, boundStatement, NullSavingStrategy.DO_NOT_SET)
    boundStatement.build
  }

  def toUserVideo(video: Video) = UserVideo(video.userid, video.addedDate, video.videoid, video.name, video.previewImageLocation)

  def toLatestVideo(video: Video) = LatestVideo(DayFormatter.format(video.addedDate), video.addedDate, video.videoid, video.userid, video.name, video.previewImageLocation)

  def toVideoByTag(video: Video, tag: String) = VideoByTag(tag, video.videoid, video.addedDate, video.userid, video.name, video.previewImageLocation, video.addedDate)

  private val DayFormatter = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC)
}
