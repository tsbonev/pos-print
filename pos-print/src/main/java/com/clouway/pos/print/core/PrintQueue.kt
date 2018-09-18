package com.clouway.pos.print.core

/**
 * Provides the methods to iterate over and append to a
 * queue of receipts.
 *
 * @author Tsvetozar Bonev (tsvetozar.bonev@clouway.com)
 */
interface PrintQueue {

  /**
   * Returns the next receipt.
   */
  fun next(): PrintReceiptRequest?

  /**
   * Queues a receipt along with its source and
   * fiscal state to the queue.
   *
   * @param printReceiptRequest the receipt to queue
   */
  fun queue(printReceiptRequest: PrintReceiptRequest)
}

data class PrintReceiptRequest(val receipt: Receipt, val sourceIp: String, val isFiscal: Boolean)