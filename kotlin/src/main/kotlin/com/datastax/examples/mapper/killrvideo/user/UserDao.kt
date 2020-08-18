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

import com.datastax.oss.driver.api.mapper.annotations.Dao
import com.datastax.oss.driver.api.mapper.annotations.QueryProvider
import com.datastax.oss.driver.api.mapper.annotations.Select
import java.util.*


@Dao
interface UserDao {

    /** Simple selection by full primary key.  */
    @Select
    fun get(userid: UUID): User?

    @Select
    fun getCredentials(email: String): UserCredentials?

    /**
     * Creating a user is more than a single insert: we have to update two different tables, check
     * that the email is not used already, and handle password encryption.
     *
     * We use a query provider to wrap everything into a single method.
     *
     * Note that you could opt for a more layered approach: only expose basic operations on the DAO
     * (insertCredentialsIfNotExists, insertUser...) and add a service layer on top for more complex
     * logic. Both designs are valid, this is a matter of personal choice.
     *
     * @return `true` if the new user was created, or `false` if this email address was
     * already taken.
     */
    @QueryProvider(
            providerClass = CreateUserQueryProvider::class,
            entityHelpers = [User::class, UserCredentials::class])
    fun create(user: User, password: CharArray): Boolean

    /**
     * Similar to [.create], this encapsulates encryption so we use a query provider.
     *
     * @return the authenticated user, or [Optional.empty] if the credentials are invalid.
     */
    @QueryProvider(
            providerClass = LoginQueryProvider::class,
            entityHelpers = [User::class, UserCredentials::class])
    fun login(email: String, password: CharArray): Optional<User>

    companion object {

        /**
         * In other languages, we implement this as a default method on the interface.
         *
         * This doesn't work in Kotlin because of a bug:
         * [KT-4779](https://youtrack.jetbrains.com/issue/KT-4779).
         * Use a static method as a lesser alternative -- we could also implement a QueryProvider,
         * as already shown for a few other methods.
         */
        fun getByEmail(dao: UserDao, email: String): User? {
            val credentials = dao.getCredentials(email)
            return if (credentials == null) {
                null
            } else {
                dao.get(credentials.userid)
            }
        }
    }
}
