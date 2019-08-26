/*
 * Copyright © 2017 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package io.cdap.cdap.data2.replication;

import io.cdap.cdap.replication.ReplicationConstants;
import io.cdap.cdap.replication.StatusUtils;
import io.cdap.cdap.replication.TableUpdater;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.client.TableDescriptor;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Common functionality required by Replication State Coprocessors to hold updates in memory and
 * flush into HBase periodically.
 */
public class HBase20TableUpdater extends TableUpdater {
  private static final Logger LOG = LoggerFactory.getLogger(HBase20TableUpdater.class);
  private final Table table;

  public HBase20TableUpdater(String rowType, Configuration conf, Table table) {
    super(rowType, conf);
    this.table = table;
  }

  @Override
  protected void writeState(Map<String, Long> cachedUpdates) throws IOException {
    List<Put> puts = new ArrayList<>();
    for (Map.Entry<String, Long> entry : cachedUpdates.entrySet()) {
      Put put = new Put(getRowKey(entry.getKey()));
      put.addColumn(columnFamily,
                    Bytes.toBytes(rowType),
                    Bytes.toBytes(entry.getValue()));
      puts.add(put);
    }

    if (!puts.isEmpty()) {
      LOG.debug("Update Replication State table now. {} entries.", puts.size());
      table.put(puts);
    }
  }

  @Override
  protected void createTableIfNotExists(Configuration conf) throws IOException {
    try (Connection connection = ConnectionFactory.createConnection(conf)) {
      Admin admin = connection.getAdmin();
      TableName tableName = TableName.valueOf(StatusUtils.getReplicationStateTableName(conf));
      if (admin.tableExists(tableName)) {
        return;
      }

      TableDescriptor htd = TableDescriptorBuilder.newBuilder(tableName)
        .setColumnFamily(ColumnFamilyDescriptorBuilder.of(ReplicationConstants.ReplicationStatusTool.TIME_FAMILY))
        .build();
      admin.createTable(htd);
      LOG.info("Created Table {}.", tableName);
    }
  }
}
