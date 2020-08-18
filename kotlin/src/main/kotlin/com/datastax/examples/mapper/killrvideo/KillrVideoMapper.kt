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
package com.datastax.examples.mapper.killrvideo

import com.datastax.examples.mapper.killrvideo.user.UserDao
import com.datastax.examples.mapper.killrvideo.video.VideoDao
import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.mapper.MapperBuilder
import com.datastax.oss.driver.api.mapper.annotations.DaoFactory
import com.datastax.oss.driver.api.mapper.annotations.Mapper


@Mapper
interface KillrVideoMapper {

    @DaoFactory
    fun userDao(): UserDao

    @DaoFactory
    fun videoDao(): VideoDao

    companion object {
        fun builder(session: CqlSession): MapperBuilder<KillrVideoMapper> {
            return KillrVideoMapperBuilder(session)
        }
    }
}
