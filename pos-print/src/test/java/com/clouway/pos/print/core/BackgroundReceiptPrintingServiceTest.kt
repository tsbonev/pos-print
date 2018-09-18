package com.clouway.pos.print.core

import com.clouway.pos.print.persistent.DatastoreCleaner
import com.clouway.pos.print.persistent.DatastoreRule
import com.clouway.pos.print.printer.Status
import org.jmock.AbstractExpectations.returnValue
import org.jmock.AbstractExpectations.throwException
import org.jmock.Expectations
import org.jmock.Mockery
import org.jmock.integration.junit4.JUnitRuleMockery
import org.jmock.lib.concurrent.Synchroniser
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import java.io.IOException

/**
 * @author Tsvetozar Bonev (tsvetozar.bonev@clouway.com)
 */
class BackgroundReceiptPrintingServiceTest {

  @Rule
  @JvmField
  var context = JUnitRuleMockery()

  init {
      context.setThreadingPolicy(Synchroniser())
  }

  companion object {
    @ClassRule
    @JvmField
    val dataStoreRule = DatastoreRule()
  }

  @Rule
  @JvmField
  var cleaner = DatastoreCleaner(dataStoreRule.db())

  private val repo = context.mock(ReceiptRepository::class.java)
  private val factory = context.mock(PrinterFactory::class.java)
  private val queue = context.mock(PrintQueue::class.java)

  private val service = BackgroundReceiptPrintingService(repo, factory, queue)

  private val printer = context.mock(ReceiptPrinter::class.java)

  private val receipt = Receipt.newReceipt()
    .withReceiptId("::receiptId::")
    .withAmount(200.0)
    .build()

  private val sourceIp = "::sourceIp::"
  private val isFiscal = true

  private val receiptRequest = PrintReceiptRequest(receipt, sourceIp, isFiscal)

  private val acceptedPrintingResponse = PrintReceiptResponse(setOf(Status.FISCAL_RECEIPT_IS_OPEN))
  private val rejectedPrintingResponse = PrintReceiptResponse(setOf(Status.BROKEN_PRINTIN_MECHANISM))

  @Test
  fun printReceipt() {
    context.expecting {
      oneOf(queue).next()
      will(returnValue(receiptRequest))

      oneOf(factory).getPrinter(sourceIp)
      will(returnValue(printer))

      oneOf(printer).printFiscalReceipt(receipt)
      will(returnValue(acceptedPrintingResponse))

      oneOf(repo).finishPrinting(receipt.receiptId)
      will(returnValue(receipt))

      oneOf(printer).close()

      oneOf(queue).next()
      will(returnValue(null))
    }

    service.printReceipts()
  }

  @Test
  fun failPrintingReceiptWhenNotPrinted(){
    context.expecting {
      oneOf(queue).next()
      will(returnValue(receiptRequest))

      oneOf(factory).getPrinter("::sourceIp::")
      will(returnValue(printer))

      oneOf(printer).printFiscalReceipt(receipt)
      will(returnValue(rejectedPrintingResponse))

      oneOf(repo).failPrinting("::receiptId::")
      will(returnValue(receipt))

      oneOf(printer).close()

      oneOf(queue).next()
      will(returnValue(null))
    }

    service.printReceipts()
  }

  @Test
  fun failPrintingWhenDeviceNotFound(){
    context.expecting {
      oneOf(queue).next()
      will(returnValue(receiptRequest))

      oneOf(factory).getPrinter(sourceIp)
      will(throwException(DeviceNotFoundException()))

      oneOf(repo).failPrinting(receipt.receiptId)
      will(returnValue(receipt))

      oneOf(queue).next()
      will(returnValue(null))
    }

    service.printReceipts()
  }

  @Test
  fun failPrintingWithIOException(){
    context.expecting {
      oneOf(queue).next()
      will(returnValue(receiptRequest))

      oneOf(factory).getPrinter(sourceIp)
      will(throwException(IOException()))

      oneOf(repo).failPrinting(receipt.receiptId)
      will(returnValue(receipt))

      oneOf(queue).next()
      will(returnValue(null))
    }

    service.printReceipts()
  }

  private fun Mockery.expecting(block: Expectations.() -> Unit) {
    checking(Expectations().apply(block))
  }
}