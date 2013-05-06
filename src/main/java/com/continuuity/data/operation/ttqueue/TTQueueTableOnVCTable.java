package com.continuuity.data.operation.ttqueue;

import com.continuuity.api.data.OperationException;
import com.continuuity.common.conf.CConfiguration;
import com.continuuity.data.operation.executor.ReadPointer;
import com.continuuity.data.operation.executor.omid.TransactionOracle;
import com.continuuity.data.table.OrderedVersionedColumnarTable;

import java.util.Iterator;

/**
 * A table of {@link TTQueue}s.  See that API for details.
 */
public class TTQueueTableOnVCTable extends TTQueueAbstractTableOnVCTable {

  private final OrderedVersionedColumnarTable table;

  public TTQueueTableOnVCTable(OrderedVersionedColumnarTable   table, TransactionOracle oracle, CConfiguration conf) {
    super(oracle, conf);
    this.table = table;
  }

  protected TTQueue getQueue(byte [] queueName) {
    TTQueue queue = this.queues.get(queueName);
    if (queue != null) return queue;
    queue = new TTQueueOnVCTable(this.table, queueName, this.oracle,
        this.conf);
    TTQueue existing = this.queues.putIfAbsent(queueName, queue);
    return existing != null ? existing : queue;
  }

  @Override
  public void clear() throws OperationException {
    table.clear();
  }

  @Override
  public void configure(byte[] queueName, QueueConsumer newConsumer, ReadPointer readPointer)
    throws OperationException {
    // Nothing to do, only needs to be implemented in com.continuuity.data.operation.ttqueue.TTQueueNewOnVCTable
  }

  @Override
  public Iterator<QueueEntry> getIterator(byte[] queueName, QueueEntryPointer start, QueueEntryPointer end,
                                          ReadPointer readPointer) {
    return getQueue(queueName).getIterator(start, end, readPointer);
  }

}
