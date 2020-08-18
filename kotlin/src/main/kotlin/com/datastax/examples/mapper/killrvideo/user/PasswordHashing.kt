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

import at.favre.lib.crypto.bcrypt.BCrypt

/**
 * Utility methods to safely store passwords in the database.
 *
 *
 * We rely on a third-party implementation of the bcrypt password hash function.
 *
 * @see [patrickfav/bcrypt](https://github.com/patrickfav/bcrypt)
 */
object PasswordHashing {
    fun hash(password: CharArray?): String {
        return BCrypt.withDefaults().hashToString(12, password)
    }

    fun matches(password: CharArray?, hash: String?): Boolean {
        val result: BCrypt.Result = BCrypt.verifyer().verify(password, hash)
        return result.verified
    }
}
