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
package com.datastax.examples;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.mapper.MapperBuilder;
import com.datastax.oss.driver.api.mapper.annotations.Dao;
import com.datastax.oss.driver.api.mapper.annotations.DaoFactory;
import com.datastax.oss.driver.api.mapper.annotations.Entity;
import com.datastax.oss.driver.api.mapper.annotations.Insert;
import com.datastax.oss.driver.api.mapper.annotations.Mapper;
import com.datastax.oss.driver.api.mapper.annotations.PartitionKey;
import com.datastax.oss.driver.api.mapper.annotations.Select;

public class MapperApp {

  private static final CqlIdentifier KEYSPACE_ID = CqlIdentifier.fromCql("record");

  public static void main(String[] args) {

    try (CqlSession session = CqlSession.builder().build()) {

      maybeCreateSchema(session);

      ProductMapper mapper =
          ProductMapper.builder(session).withDefaultKeyspace(KEYSPACE_ID).build();
      ProductDao dao = mapper.dao();

      Product initialProduct = new Product(1, "test");
      System.out.printf("Saving %s...%n", initialProduct);
      dao.save(initialProduct);

      Product retrievedProduct = dao.get(1);
      System.out.printf("Retrieved %s%n", retrievedProduct);
    }
  }

  @Entity
  record Product(@PartitionKey int id, String description) {}

  @Dao
  interface ProductDao {
    @Select
    Product get(int id);

    @Insert
    void save(Product product);
  }

  @Mapper
  interface ProductMapper {
    @DaoFactory
    ProductDao dao();

    static MapperBuilder<ProductMapper> builder(CqlSession session) {
      return new MapperApp_ProductMapperBuilder(session);
    }
  }

  private static void maybeCreateSchema(CqlSession session) {
    session.execute(
        SimpleStatement.newInstance(
                String.format(
                    "CREATE KEYSPACE IF NOT EXISTS %s "
                        + "WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}",
                    KEYSPACE_ID.asCql(false)))
            .setExecutionProfileName("slow"));
    session.execute(
        SimpleStatement.newInstance(
                String.format(
                    "CREATE TABLE IF NOT EXISTS %s.product( "
                        + "id int PRIMARY KEY, description text)",
                    KEYSPACE_ID.asCql(false)))
            .setExecutionProfileName("slow"));
  }
}
