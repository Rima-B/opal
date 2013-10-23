/*******************************************************************************
 * Copyright (c) 2012 OBiBa. All rights reserved.
 *
 * This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.obiba.opal.core.runtime.jdbc;

import javax.annotation.Nonnull;
import javax.sql.DataSource;

import org.obiba.opal.core.domain.database.SqlDatabase;
import org.springframework.stereotype.Component;

import bitronix.tm.resource.jdbc.PoolingDataSource;

@Component
public class DataSourceFactory {

  public static final int MIN_POOL_SIZE = 3;

  public static final int MAX_POOL_SIZE = 50;

  public DataSource createDataSource(@Nonnull SqlDatabase database) {
    PoolingDataSource dataSource = new PoolingDataSource();
    dataSource.setUniqueName(database.getName());
//    dataSource.setClassName(database.getDriverClass());
    dataSource.setClassName("com.mysql.jdbc.jdbc2.optional.MysqlXADataSource");
    dataSource.getDriverProperties().setProperty("url", database.getUrl());
    dataSource.getDriverProperties().setProperty("user", database.getUsername());
    dataSource.getDriverProperties().setProperty("password", database.getPassword());
    dataSource.setMinPoolSize(MIN_POOL_SIZE);
    dataSource.setMaxPoolSize(MAX_POOL_SIZE);
    dataSource.setAllowLocalTransactions(true);

    if("com.mysql.jdbc.Driver".equals(database.getDriverClass())) {
      dataSource.setTestQuery("select 1");
    } else if("org.hsqldb.jdbcDriver".equals(database.getDriverClass())) {
      dataSource.setTestQuery("select 1 from INFORMATION_SCHEMA.SYSTEM_USERS");
    }
    //TODO validation query for PostgreSQL

    return dataSource;
  }

}
