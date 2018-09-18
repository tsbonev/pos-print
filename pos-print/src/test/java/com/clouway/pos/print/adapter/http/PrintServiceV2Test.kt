package com.clouway.pos.print.adapter.http

import com.clouway.pos.print.FakeRequest
import com.clouway.pos.print.ReplyMatchers
import com.clouway.pos.print.core.*
import org.hamcrest.MatcherAssert
import org.jmock.AbstractExpectations.returnValue
import org.jmock.AbstractExpectations.throwException
import org.jmock.Expectations
import org.jmock.Mockery
import org.jmock.integration.junit4.JUnitRuleMockery
import org.junit.Rule
import org.junit.Test

/**
 * @author Tsvetozar Bonev (tsvetozar.bonev@clouway.com)
 */
class PrintServiceV2Test {

  @Rule
  @JvmField
  var context = JUnitRuleMockery()

  private fun Mockery.expecting(block: Expectations.() -> Unit){
          checking(Expectations().apply(block))
  }

  private val repo = context.mock(ReceiptRepository::class.java)
  private val queue = context.mock(PrintQueue::class.java)

  private val receipt = Receipt.newReceipt().withReceiptId("::receipt-id::").build()

  private val sourceIp = "::sourceIp::"
  private val isFiscal = true

  private val receiptWithSource = PrintReceiptRequest(receipt, sourceIp, isFiscal)

  private val receiptDTO = PrintServiceV2
    .ReceiptDTO(sourceIp, "::operatorId::", isFiscal, receipt = receipt)

  private val service =  PrintServiceV2(repo, queue)

  @Test
  fun queueReceiptForPrinting(){
    context.expecting {
      oneOf(repo).register(receipt)
      will(returnValue(receipt.receiptId))

      oneOf(queue).queue(receiptWithSource)
    }

    val reply = service.printReceipt(FakeRequest.newRequest(receiptDTO))
    MatcherAssert.assertThat(reply, ReplyMatchers.isAccepted)
    MatcherAssert.assertThat(reply, ReplyMatchers.contains(receipt.receiptId))
  }

  @Test
  fun savingAlreadyExistingReceiptThrowsException(){
    context.expecting {
      oneOf(repo).register(receipt)
      will(throwException(ReceiptAlreadyInQueueException()))
    }

    val reply = service.printReceipt(FakeRequest.newRequest(receiptDTO))
    MatcherAssert.assertThat(reply, ReplyMatchers.isBadRequest)
  }

  @Test
  fun getReceiptStatus(){
    context.expecting {
      oneOf(repo).getStatus(receipt.receiptId)
      will(returnValue(PrintStatus.PRINTING))
    }

    val reply = service.getReceiptStatus(receipt.receiptId)
    MatcherAssert.assertThat(reply, ReplyMatchers.isOk)
    MatcherAssert.assertThat(reply, ReplyMatchers.contains(PrintStatus.PRINTING))
  }

  @Test
  fun getReceiptStatusOfNonExisting(){
    context.expecting {
      oneOf(repo).getStatus(receipt.receiptId)
      will(throwException(ReceiptNotInQueueException()))
    }

    val reply = service.getReceiptStatus(receipt.receiptId)
    MatcherAssert.assertThat(reply, ReplyMatchers.isNotFound)
  }
}