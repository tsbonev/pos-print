package com.clouway.pos.print.core

import org.slf4j.LoggerFactory

/**
 * @author Tsvetozar Bonev (tsvetozar.bonev@clouway.com)
 */
class ReceiptPrintingListener : PrintingListener {
  override fun onPrinted(receipt: Receipt, printStatus: PrintStatus) {
    val logger = LoggerFactory.getLogger(PrintingListener::class.java)

    logger.info("Receipt was processed")
    logger.info(receipt.receiptId)
    logger.info(printStatus.name)
  }
}