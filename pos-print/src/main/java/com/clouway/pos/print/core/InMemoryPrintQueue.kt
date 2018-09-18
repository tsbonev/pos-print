package com.clouway.pos.print.core

import java.util.concurrent.ArrayBlockingQueue

/**
 * @author Tsvetozar Bonev (tsvetozar.bonev@clouway.com)
 */
class InMemoryPrintQueue : PrintQueue {

  private val queueMax = 50

  private val queue = ArrayBlockingQueue<PrintReceiptRequest>(queueMax)

  override fun next(): PrintReceiptRequest {
    return queue.take()
  }

  override fun queue(printReceiptRequest: PrintReceiptRequest) {
    queue.offer(printReceiptRequest)
  }
}