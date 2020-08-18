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

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.BatchStatement
import com.datastax.oss.driver.api.core.cql.BoundStatement
import com.datastax.oss.driver.api.core.cql.DefaultBatchType
import com.datastax.oss.driver.api.core.cql.PreparedStatement
import com.datastax.oss.driver.api.mapper.MapperContext
import com.datastax.oss.driver.api.mapper.entity.EntityHelper
import com.datastax.oss.driver.api.mapper.entity.saving.NullSavingStrategy
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*


/**
 * Provides the implementation of [VideoDao.create].
 *
 * Package-private visibility is sufficient, this will be called only from the generated DAO
 * implementation.
 */
internal class CreateVideoQueryProvider(
        context: MapperContext,
        private val videoHelper: EntityHelper<Video>,
        private val userVideoHelper: EntityHelper<UserVideo>,
        private val latestVideoHelper: EntityHelper<LatestVideo>,
        private val videoByTagHelper: EntityHelper<VideoByTag>) {

    private val session: CqlSession = context.session
    private val preparedInsertVideo: PreparedStatement
    private val preparedInsertUserVideo: PreparedStatement
    private val preparedInsertLatestVideo: PreparedStatement
    private val preparedInsertVideoByTag: PreparedStatement

    fun create(video: Video): Video {
        val videoWithDefaults = video.copy(
                videoid = video.videoid ?: UUID.randomUUID(),
                addedDate = video.addedDate ?: Instant.now()
        )
        insertInAllViews(videoWithDefaults)
        return videoWithDefaults
    }

    private fun insertInAllViews(video: Video) {
        val batch = BatchStatement.builder(DefaultBatchType.LOGGED)
        batch.addStatement(bind(preparedInsertVideo, video, videoHelper))
        batch.addStatement(bind(preparedInsertUserVideo, toUserVideo(video), userVideoHelper))
        batch.addStatement(bind(preparedInsertLatestVideo, toLatestVideo(video), latestVideoHelper))
        if (video.tags != null) {
            for (tag in video.tags) {
                batch.addStatement(
                        bind(preparedInsertVideoByTag, toVideoByTag(video, tag), videoByTagHelper))
            }
        }
        session.execute(batch.build())
    }

    init {
        preparedInsertVideo = prepareInsert(session, videoHelper)
        preparedInsertUserVideo = prepareInsert(session, userVideoHelper)
        preparedInsertLatestVideo = prepareInsert(session, latestVideoHelper)
        preparedInsertVideoByTag = prepareInsert(session, videoByTagHelper)
    }

    companion object {
        private fun <T> prepareInsert(
                session: CqlSession, entityHelper: EntityHelper<T>): PreparedStatement {
            return session.prepare(entityHelper.insert().asCql())
        }

        private fun <T> bind(
                preparedStatement: PreparedStatement, entity: T, entityHelper: EntityHelper<T>): BoundStatement {
            val boundStatement = preparedStatement.boundStatementBuilder()
            entityHelper.set(entity, boundStatement, NullSavingStrategy.DO_NOT_SET)
            return boundStatement.build()
        }

        private fun toUserVideo(video: Video): UserVideo {
            return UserVideo(
                    video.userid,
                    video.addedDate,
                    video.videoid,
                    video.name,
                    video.previewImageLocation)
        }

        private fun toLatestVideo(video: Video): LatestVideo {
            return LatestVideo(
                    DAY_FORMATTER.format(video.addedDate),
                    video.addedDate,
                    video.videoid,
                    video.userid,
                    video.name,
                    video.previewImageLocation)
        }

        private fun toVideoByTag(video: Video, tag: String): VideoByTag {
            return VideoByTag(
                    tag,
                    video.videoid,
                    video.addedDate,
                    video.userid,
                    video.name,
                    video.previewImageLocation,
                    video.addedDate)
        }

        private val DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd").withZone(ZoneOffset.UTC)
    }
}
