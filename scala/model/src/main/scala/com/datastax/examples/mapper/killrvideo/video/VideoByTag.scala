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

import java.time.Instant
import java.util.UUID

import com.datastax.oss.driver.api.mapper.annotations.{ClusteringColumn, CqlName, Entity, PartitionKey}

import scala.annotation.meta.field

@Entity
@CqlName("videos_by_tag")
case class VideoByTag(@(PartitionKey@field) tag: String,
                      @(ClusteringColumn@field)(0) videoid: UUID,
                      addedDate: Instant,
                      userid: UUID,
                      name: String,
                      previewImageLocation: String,
                      taggedDate: Instant)
