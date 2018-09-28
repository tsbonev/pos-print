package com.clouway.pos.print.persistent

import com.clouway.pos.print.adapter.db.*
import com.clouway.pos.print.core.*
import com.google.inject.util.Providers
import org.jmock.Expectations
import org.jmock.Mockery
import org.jmock.integration.junit4.JUnitRuleMockery
import org.junit.Assert.assertThat
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import org.hamcrest.CoreMatchers.`is` as Is

/**
 * @author Tsvetozar Bonev (tsvetozar.bonev@clouway.com)
 */
class PersistentReceiptRepositoryTest {
  companion object {
    @ClassRule
    @JvmField
    val dataStoreRule = DatastoreRule()
  }

  @Rule
  @JvmField
  val context: JUnitRuleMockery = JUnitRuleMockery()

  @Rule
  @JvmField
  var cleaner = DatastoreCleaner(dataStoreRule.db())

  private val printingListener = context.mock(PrintingListener::class.java)

  private val repository = PersistentReceiptRepository(Providers.of(dataStoreRule.db()), printingListener)

  private val receipt = Receipt.newReceipt().withReceiptId("::receiptId::").build()
  private val sourceIp = "1.1.1.1"
  private val operatorId = "2.2.2.2"
  private val isFiscal = true

  private val receiptRequest = ReceiptRequest(receipt, sourceIp, operatorId, isFiscal)

  @Test
  fun mongoQueue(){
    dataStoreRule.db()
  }

  @Test
  fun happyPath() {
    repository.register(receiptRequest)

    assertThat(repository.getStatus(receipt.receiptId), Is(PrintStatus.PRINTING))
  }

  @Test
  fun queueReceipt() {
    assertThat(repository.register(receiptRequest), Is(receipt.receiptId))
  }

  @Test(expected = ReceiptAlreadyInQueueException::class)
  fun queueingReceiptTwiceThrowsException() {
    repository.register(receiptRequest)
    repository.register(receiptRequest)
  }

  @Test
  fun getReceiptStatus() {
    repository.register(receiptRequest)

    assertThat(repository.getStatus(receipt.receiptId), Is(PrintStatus.PRINTING))
  }

  @Test(expected = ReceiptNotInQueueException::class)
  fun gettingStatusOfNonExistingReceiptThrowsException() {
    repository.getStatus("::non-existent-receiptId::")
  }

  @Test
  fun getReceiptsByStatus() {
    repository.register(receiptRequest)

    val printingReceipts = repository.getByStatus(PrintStatus.PRINTING)
    val printedReceipts = repository.getByStatus(PrintStatus.PRINTED)

    assertThat(printingReceipts, Is(listOf(receipt)))
    assertThat(printedReceipts, Is(emptyList()))
  }

  @Test
  fun finishReceipt() {
    context.expecting {
      oneOf(printingListener).onPrinted(receipt, PrintStatus.PRINTED)
    }

    repository.register(receiptRequest)
    val finishedReceipt = repository.finishPrinting(receipt.receiptId)

    assertThat(repository.getStatus(finishedReceipt.receiptId), Is(PrintStatus.PRINTED))
  }

  @Test(expected = ReceiptNotInQueueException::class)
  fun finishingNonExistingReceiptThrowsException() {
    repository.finishPrinting("::fake-receiptId::")
  }

  @Test
  fun rejectReceipt() {
    context.expecting {
      oneOf(printingListener).onPrinted(receipt, PrintStatus.FAILED)
    }

    repository.register(receiptRequest)
    val finishedReceipt = repository.failPrinting(receipt.receiptId)

    assertThat(repository.getStatus(finishedReceipt.receiptId), Is(PrintStatus.FAILED))
  }

  @Test(expected = ReceiptNotInQueueException::class)
  fun rejectingNonExistingReceiptThrowsException() {
    repository.finishPrinting("::fake-receiptId::")
  }

  private fun Mockery.expecting(block: Expectations.() -> Unit){
          checking(Expectations().apply(block))
  }
}