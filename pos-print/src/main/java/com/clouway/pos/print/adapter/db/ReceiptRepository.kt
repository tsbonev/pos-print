package com.clouway.pos.print.adapter.db

import com.clouway.pos.print.core.Receipt

/**
 * Provides the methods to persist, queue and check the status
 * of requested receipts.
 *
 * @author Tsvetozar Bonev (tsvetozar.bonev@clouway.com)
 */
interface ReceiptRepository {

  /**
   * Adds a receipt to the queue.
   *
   * @param receipt the receipt to queue
   * @return the id of the receipt
   */
  @Throws(ReceiptAlreadyInQueueException::class)
  fun queue(receipt: Receipt): String

  /**
   * Returns the printing status of a receipt.
   *
   * @param receiptId the id of the receipt
   * @return the status of the receipt
   */
  @Throws(ReceiptNotInQueueException::class)
  fun getStatus(receiptId: String): ReceiptStatus

  /**
   * Returns a list of exceptions with a given status.
   *
   * @param receiptStatus the status to group by
   * @return the list of receipts with that status
   */
  fun getByStatus(receiptStatus: ReceiptStatus): List<Receipt>

  /**
   * Finishes a receipt by updating its status to Printed.
   *
   * @param receiptId the id of the receipt to finish
   * @return the finished receipt
   */
  @Throws(ReceiptNotInQueueException::class)
  fun finishPrinting(receiptId: String): Receipt

  /**
   * Rejects a receipt by updating its status to Rejected.
   *
   * @param receiptId the id of the receipt to reject.
   * @return the rejected receipt
   */
  @Throws(ReceiptNotInQueueException::class)
  fun rejectPrinting(receiptId: String): Receipt

  /**
   * Requeues a receipt by id.
   *
   * @param receiptId the id of the receipt to requeue
   * @return the requeued receipt.
   */
  @Throws(ReceiptNotInQueueException::class,
    ReceiptAlreadyInQueueException::class)
  fun reQueue(receiptId: String): Receipt

  /**
   * Removes a receipt.
   *
   * @param receiptId the id of the receipt
   * @return the removed receipt
   */
  @Throws(ReceiptNotInQueueException::class)
  fun remove(receiptId: String): Receipt
}


internal class ReceiptNotInQueueException : Throwable()
internal class ReceiptAlreadyInQueueException : Throwable()

enum class ReceiptStatus{
  PRINTING, PRINTED, REJECTED
}