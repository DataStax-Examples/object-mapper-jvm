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
package com.datastax.examples.mapper.killrvideo.user

import com.datastax.oss.driver.api.core.CqlSession
import com.datastax.oss.driver.api.core.cql.PreparedStatement
import com.datastax.oss.driver.api.core.cql.Row
import com.datastax.oss.driver.api.mapper.MapperContext
import com.datastax.oss.driver.api.mapper.entity.EntityHelper
import java.util.*

/**
 * Provides the implementation of [UserDao.login].
 *
 * Package-private visibility is sufficient, this will be called only from the generated DAO
 * implementation.
 */
internal class LoginQueryProvider(
        context: MapperContext,
        private val userHelper: EntityHelper<User>,
        credentialsHelper: EntityHelper<UserCredentials?>) {

    private val session: CqlSession = context.session
    private val preparedSelectCredentials: PreparedStatement
    private val preparedSelectUser: PreparedStatement

    fun login(email: String, password: CharArray): Optional<User> {
        return Optional.ofNullable(session.execute(preparedSelectCredentials.bind(email)).one())
                .flatMap<User> { credentialsRow: Row ->
                    val hashedPassword = credentialsRow.getString("password")
                    if (PasswordHashing.matches(password, hashedPassword)) {
                        val userid = credentialsRow.getUuid("userid")
                        val userRow = session.execute(preparedSelectUser.bind(userid)).one()
                        checkNotNull(userRow) { "Should have found matching row for userid $userid" }
                        Optional.of(userHelper[userRow])
                    } else {
                        Optional.empty()
                    }
                }
    }

    init {
        preparedSelectCredentials = session.prepare(credentialsHelper.selectByPrimaryKey().asCql())
        preparedSelectUser = session.prepare(userHelper.selectByPrimaryKey().asCql())
    }
}
