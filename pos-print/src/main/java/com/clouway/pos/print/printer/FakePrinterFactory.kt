package com.clouway.pos.print.printer

import com.clouway.pos.print.core.*
import java.io.File
import java.time.LocalDateTime
import java.util.UUID

/**
 * @author Tsvetozar Bonev (tsvetozar.bonev@clouway.com)
 */
class FakePrinterFactory : PrinterFactory {
  override fun getPrinter(sourceIp: String): ReceiptPrinter {
    return FakePrinter()
  }
}

private val fileLocation = "/home/tsvetozar/clouway/workspaces/idea/receipts"

class FakePrinter : ReceiptPrinter {
  override fun printReceipt(receipt: Receipt?): PrintReceiptResponse {
    return PrintReceiptResponse(setOf(Status.NON_FISCAL_RECEIPT_IS_OPEN))
  }

  override fun printFiscalReceipt(receipt: Receipt): PrintReceiptResponse {

    val uuid = UUID.randomUUID().toString()

    File("$fileLocation/$uuid-${receipt.receiptId}.txt").writeText(receipt.receiptItems.toString())

    Thread.sleep(400)
    return PrintReceiptResponse(setOf(Status.NON_FISCAL_RECEIPT_IS_OPEN))
  }

  override fun reportForOperator(operatorId: String?, state: RegisterState?) {}

  override fun reportForPeriod(start: LocalDateTime?, end: LocalDateTime?, periodType: PeriodType?) {}

  override fun close() {}
}