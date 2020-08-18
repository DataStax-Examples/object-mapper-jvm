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
import com.datastax.oss.driver.api.core.DefaultConsistencyLevel
import com.datastax.oss.driver.api.core.cql.PreparedStatement
import com.datastax.oss.driver.api.mapper.MapperContext
import com.datastax.oss.driver.api.mapper.entity.EntityHelper
import com.datastax.oss.driver.api.mapper.entity.saving.NullSavingStrategy
import com.datastax.oss.driver.api.querybuilder.QueryBuilder
import java.time.Instant
import java.util.*


/**
 * Provides the implementation of [UserDao.create].
 *
 * Package-private visibility is sufficient, this will be called only from the generated DAO
 * implementation.
 */
internal class CreateUserQueryProvider(
        context: MapperContext,
        private val userHelper: EntityHelper<User>,
        private val credentialsHelper: EntityHelper<UserCredentials>) {

    private val session: CqlSession = context.session
    private val preparedInsertCredentials: PreparedStatement
    private val preparedInsertUser: PreparedStatement
    private val preparedDeleteCredentials: PreparedStatement
    private val preparedDeleteUser: PreparedStatement

    fun create(user: User, password: CharArray): Boolean {
        if (user.userid == null || user.email == null) {
            throw IllegalArgumentException("id and email must not be null")
        }
        return try {
            // Insert the user first: otherwise there would be a short window where we have credentials
            // without a corresponding user in the database, and this is considered an error state in
            // LoginQueryProvider
            insertUser(if (user.createdDate == null) user.copy(createdDate = Instant.now()) else user)
            if (!insertCredentialsIfNotExists(user.email, password, user.userid)) { // email already exists
                session.execute(preparedDeleteUser.bind(user.userid))
                return false
            }
            true
        } catch (insertException: Exception) {
            // Clean up and rethrow
            try {
                session.execute(preparedDeleteUser.bind(user.userid))
            } catch (e: Exception) {
                insertException.addSuppressed(e)
            }
            try {
                session.execute(preparedDeleteCredentials.bind(user.email, user.userid))
            } catch (e: Exception) {
                insertException.addSuppressed(e)
            }
            throw insertException
        }
    }

    private fun insertCredentialsIfNotExists(email: String, password: CharArray, userId: UUID): Boolean {
        val passwordHash = PasswordHashing.hash(Objects.requireNonNull(password))
        val credentials = UserCredentials(Objects.requireNonNull(email), passwordHash, userId)
        val insertCredentials = preparedInsertCredentials.boundStatementBuilder()
        credentialsHelper.set(credentials, insertCredentials, NullSavingStrategy.DO_NOT_SET)
        val resultSet = session.execute(insertCredentials.build())
        return resultSet.wasApplied()
    }

    private fun insertUser(user: User) {
        val insertUser = preparedInsertUser.boundStatementBuilder()
        userHelper.set(user, insertUser, NullSavingStrategy.DO_NOT_SET)
        session.execute(insertUser.build())
    }

    init {
        preparedInsertCredentials = session.prepare(credentialsHelper.insert().ifNotExists().asCql())
        preparedInsertUser = session.prepare(userHelper.insert().asCql())
        preparedDeleteCredentials = session.prepare(
                credentialsHelper
                        .deleteByPrimaryKey()
                        .ifColumn("userid")
                        .isEqualTo(QueryBuilder.bindMarker("userid"))
                        .builder()
                        .setConsistencyLevel(DefaultConsistencyLevel.ANY)
                        .build())
        preparedDeleteUser = session.prepare(
                userHelper
                        .deleteByPrimaryKey()
                        .ifExists()
                        .builder()
                        .setConsistencyLevel(DefaultConsistencyLevel.ANY)
                        .build())
    }
}
