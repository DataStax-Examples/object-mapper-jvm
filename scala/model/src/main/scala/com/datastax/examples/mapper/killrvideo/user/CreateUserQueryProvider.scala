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

import java.time.Instant
import java.util.{Objects, UUID}

import com.datastax.oss.driver.api.core.DefaultConsistencyLevel
import com.datastax.oss.driver.api.mapper.MapperContext
import com.datastax.oss.driver.api.mapper.entity.EntityHelper
import com.datastax.oss.driver.api.mapper.entity.saving.NullSavingStrategy
import com.datastax.oss.driver.api.querybuilder.QueryBuilder.bindMarker


/**
 * Provides the implementation of {@link UserDao#create}.
 *
 * <p>Package-private visibility is sufficient, this will be called only from the generated DAO
 * implementation.
 */
private[user] class CreateUserQueryProvider(val context: MapperContext,
                                            val userHelper: EntityHelper[User],
                                            val credentialsHelper: EntityHelper[UserCredentials]) {

  private val session = context.getSession
  private val preparedInsertCredentials = session.prepare(credentialsHelper.insert.ifNotExists.asCql)
  private val preparedInsertUser = session.prepare(userHelper.insert.asCql)
  private val preparedDeleteCredentials = session.prepare(
    credentialsHelper.deleteByPrimaryKey()
      .ifColumn("userid").isEqualTo(bindMarker("userid"))
      .builder
      .setConsistencyLevel(DefaultConsistencyLevel.ANY)
      .build)
  private val preparedDeleteUser = session.prepare(
    userHelper.deleteByPrimaryKey()
      .ifExists.builder
      .setConsistencyLevel(DefaultConsistencyLevel.ANY)
      .build)

  def create(user: User, password: Array[Char]): Boolean = {
    require(user.userid != null)
    require(user.email != null)
    try {
      // Insert the user first: otherwise there would be a short window where we have credentials
      // without a corresponding user in the database, and this is considered an error state in
      // LoginQueryProvider
      insertUser(fillCreatedDate(user))
      if (!insertCredentialsIfNotExists(user.email, password, user.userid)) { // email already exists
        session.execute(preparedDeleteUser.bind(user.userid))
        return false
      }
      true
    } catch {
      case insertException: Exception =>
        // Clean up and rethrow
        try session.execute(preparedDeleteUser.bind(user.userid))
        catch {
          case e: Exception =>
            insertException.addSuppressed(e)
        }
        try session.execute(preparedDeleteCredentials.bind(user.email, user.userid))
        catch {
          case e: Exception =>
            insertException.addSuppressed(e)
        }
        throw insertException
    }
  }

  private def fillCreatedDate(user: User): User =
    if (user.createdDate == null) user.copy(createdDate = Instant.now) else user

  private def insertCredentialsIfNotExists(email: String, password: Array[Char], userId: UUID): Boolean = {
    val passwordHash = PasswordHashing.hash(Objects.requireNonNull(password))
    val credentials = UserCredentials(Objects.requireNonNull(email), passwordHash, userId)
    val insertCredentials = preparedInsertCredentials.boundStatementBuilder()
    credentialsHelper.set(credentials, insertCredentials, NullSavingStrategy.DO_NOT_SET)
    val resultSet = session.execute(insertCredentials.build)
    resultSet.wasApplied
  }

  private def insertUser(user: User): Unit = {
    val insertUser = preparedInsertUser.boundStatementBuilder()
    userHelper.set(user, insertUser, NullSavingStrategy.DO_NOT_SET)
    session.execute(insertUser.build)
  }
}