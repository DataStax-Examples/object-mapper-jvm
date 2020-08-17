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

import java.util.Optional

import com.datastax.oss.driver.api.mapper.MapperContext
import com.datastax.oss.driver.api.mapper.entity.EntityHelper


/**
 * Provides the implementation of {@link UserDao#login}.
 *
 * <p>Package-private visibility is sufficient, this will be called only from the generated DAO
 * implementation.
 */
class LoginQueryProvider(val context: MapperContext,
                         val userHelper: EntityHelper[User],
                         val credentialsHelper: EntityHelper[UserCredentials]) {

  private val session = context.getSession
  private val preparedSelectCredentials = session.prepare(credentialsHelper.selectByPrimaryKey.asCql)
  private val preparedSelectUser = session.prepare(userHelper.selectByPrimaryKey.asCql)

  def login(email: String, password: Array[Char]): Optional[User] = Optional.ofNullable(session.execute(preparedSelectCredentials.bind(email)).one)
    .flatMap(credentialsRow => {
      val hashedPassword = credentialsRow.getString("password")
      if (PasswordHashing.matches(password, hashedPassword)) {
        val userid = credentialsRow.getUuid("userid")
        val userRow = session.execute(preparedSelectUser.bind(userid)).one
        if (userRow == null) {
          throw new IllegalStateException("Should have found matching row for userid " + userid)
        } else {
          return Optional.of(userHelper.get(userRow))
        }
      }
      else return Optional.empty
    })
}