package com.clouway.pos.print.core

import java.util.Optional

/**
 * Provides the methods to persist, save and check the status
 * of requested receipts.
 *
 * @author Tsvetozar Bonev (tsvetozar.bonev@clouway.com)
 */
interface ReceiptRepository {

  /**
   * Registers a receipt in persistence.
   *
   * @param receiptRequest the receipt request to save
   * @return the id of the receipt
   */
  @Throws(ReceiptAlreadyInQueueException::class)
  fun register(receiptRequest: ReceiptRequest): String

  /**
   * Retrieves an optional receipt by id.
   *
   * @param receiptId the id of the receipt
   * @return an optional receipt
   */
  fun getByReceiptId(receiptId: String): Optional<ReceiptRequest>

  /**
   * Retrieves a list of receipts that have the same source ip.
   *
   * @param sourceIp the source ip to query by
   * @return a list of receipts
   */
  fun getBySourceIp(sourceIp: String): List<ReceiptRequest>

  /**
   * Retrieves a list of receipts that were sent by the same operator.
   *
   * @param operatorId the operator id to query by
   * @return a list of receipts
   */
  fun getByOperatorId(operatorId: String): List<ReceiptRequest>

  /**
   * Returns the printing status of a receipt.
   *
   * @param receiptId the id of the receipt
   * @return the status of the receipt
   */
  @Throws(ReceiptNotInQueueException::class)
  fun getStatus(receiptId: String): PrintStatus

  /**
   * Returns a list of exceptions with a given status.
   *
   * @param receiptStatus the status to group by
   * @return the list of receipts with that status
   */
  fun getByStatus(receiptStatus: PrintStatus): List<Receipt>

  /**
   * Marks a receipt as Printed.
   *
   * @param receiptId the id of the receipt to finish
   * @return the finished receipt
   */
  @Throws(ReceiptNotInQueueException::class)
  fun finishPrinting(receiptId: String): Receipt

  /**
   * Marks a receipt as Failed.
   *
   * @param receiptId the id of the receipt to fail.
   * @return the rejected receipt
   */
  @Throws(ReceiptNotInQueueException::class)
  fun failPrinting(receiptId: String): Receipt
}


internal class ReceiptNotInQueueException : Throwable()
internal class ReceiptAlreadyInQueueException : Throwable()

enum class PrintStatus {
  PRINTING, PRINTED, FAILED
}