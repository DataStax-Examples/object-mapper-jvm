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

import java.util.UUID

import com.datastax.oss.driver.api.core.PagingIterable
import com.datastax.oss.driver.api.mapper.annotations.{Dao, QueryProvider, Select, Update}
import com.datastax.oss.driver.api.mapper.entity.saving.NullSavingStrategy

@Dao
trait VideoDao {

  /** Simple selection by full primary key. */
  @Select
  def get(videoid: UUID): Video

  /**
   * Selection by partial primary key, this will return multiple rows.
   *
   * <p>Also, note that this queries a different table: DAOs are not limited to a single entity, the
   * return type of the method dictates what rows will be mapped to.
   */
  @Select
  def getByUser(userid: UUID): PagingIterable[UserVideo]

  /** Other selection by partial primary key, for another table. */
  @Select
  def getLatest(yyyymmdd: String): PagingIterable[LatestVideo]

  /** Other selection by partial primary key, for yet another table. */
  @Select
  def getByTag(tag: String): PagingIterable[VideoByTag]

  /**
   * Creating a video is a bit more complex: because of denormalization, it involves multiple
   * tables.
   *
   * <p>A query provider is a nice way to wrap all the queries in a single operation, and hide the
   * details from the DAO interface.
   *
   * @return the video, or a new instance if some of the fields were assigned automatically
   */
  @QueryProvider(
    providerClass = classOf[CreateVideoQueryProvider],
    entityHelpers = Array(classOf[Video], classOf[UserVideo], classOf[LatestVideo], classOf[VideoByTag]))
  def create(video: Video): Video

  /**
   * Update using a template: the template must have its full primary key set; beyond that, any
   * non-null field will be considered as a value to SET on the target row.
   *
   * <p>Note that we specify the null saving strategy for emphasis, but this is the default.
   */
  @Update(nullSavingStrategy = NullSavingStrategy.DO_NOT_SET)
  def update(template: Video): Unit
}