package com.clouway.pos.print.persistent

import com.clouway.pos.print.adapter.db.*
import com.clouway.pos.print.core.Receipt
import com.google.inject.util.Providers
import com.mongodb.CursorType
import com.mongodb.client.MongoCollection
import org.bson.Document
import org.junit.Assert.assertThat
import org.junit.ClassRule
import org.junit.Rule
import org.junit.Test
import kotlin.concurrent.thread
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
  var cleaner = DatastoreCleaner(dataStoreRule.db())

  private val receiptCollection = "test-receipts"
  private val receiptQueue = "test-receipts-queue"

  private val repository = PersistentReceiptRepository(Providers.of(dataStoreRule.db()),
    receiptCollection, receiptQueue)

  private val anyReceipt = Receipt.newReceipt().withReceiptId("::receiptId::").build()

  @Test
  fun happyPath() {
    thread {
      tailCursor(repository, dataStoreRule.db()!!.getCollection(receiptQueue))
    }

    repository.queue(anyReceipt)

    assertThat(repository.getStatus(anyReceipt.receiptId), Is(ReceiptStatus.PRINTING))

    Thread.sleep(500)

    assertThat(repository.getStatus(anyReceipt.receiptId), Is(ReceiptStatus.PRINTED))
  }

  @Test
  fun queueReceipt() {
    assertThat(repository.queue(anyReceipt), Is(anyReceipt.receiptId))
  }

  @Test(expected = ReceiptAlreadyInQueueException::class)
  fun queueingReceiptTwiceThrowsException() {
    repository.queue(anyReceipt)
    repository.queue(anyReceipt)
  }

  @Test
  fun getReceiptStatus() {
    repository.queue(anyReceipt)

    assertThat(repository.getStatus(anyReceipt.receiptId), Is(ReceiptStatus.PRINTING))
  }

  @Test(expected = ReceiptNotInQueueException::class)
  fun gettingStatusOfNonExistingReceiptThrowsException() {
    repository.getStatus("::non-existent-receiptId::")
  }

  @Test
  fun removeReceipt() {
    repository.queue(anyReceipt)

    assertThat(repository.remove(anyReceipt.receiptId), Is(anyReceipt))
  }

  @Test(expected = ReceiptNotInQueueException::class)
  fun removingNonExistingReceiptThrowsException() {
    repository.remove("::non-existent-receiptId::")
  }

  @Test
  fun getReceiptsByStatus() {
    repository.queue(anyReceipt)

    val printingReceipts = repository.getByStatus(ReceiptStatus.PRINTING)
    val printedReceipts = repository.getByStatus(ReceiptStatus.PRINTED)

    assertThat(printingReceipts, Is(listOf(anyReceipt)))
    assertThat(printedReceipts, Is(emptyList()))
  }

  @Test
  fun finishReceipt() {
    repository.queue(anyReceipt)
    val finishedReceipt = repository.finishPrinting(anyReceipt.receiptId)

    assertThat(repository.getStatus(finishedReceipt.receiptId), Is(ReceiptStatus.PRINTED))
  }

  @Test(expected = ReceiptNotInQueueException::class)
  fun finishingNonExistingReceiptThrowsException() {
    repository.finishPrinting("::fake-receiptId::")
  }

  @Test
  fun rejectReceipt() {
    repository.queue(anyReceipt)
    val finishedReceipt = repository.rejectPrinting(anyReceipt.receiptId)

    assertThat(repository.getStatus(finishedReceipt.receiptId), Is(ReceiptStatus.REJECTED))
  }

  @Test(expected = ReceiptNotInQueueException::class)
  fun rejectingNonExistingReceiptThrowsException() {
    repository.finishPrinting("::fake-receiptId::")
  }

  @Test
  fun requeueReceipt() {
    repository.queue(anyReceipt)
    repository.rejectPrinting(anyReceipt.receiptId)

    val requeuedReceipt = repository.reQueue(anyReceipt.receiptId)

    assertThat(repository.getStatus(requeuedReceipt.receiptId), Is(ReceiptStatus.PRINTING))
  }

  @Test(expected = ReceiptNotInQueueException::class)
  fun requeueingNonExistingReceiptThrowsException() {
    repository.reQueue("::fake-receiptId::")
  }

  @Test(expected = ReceiptAlreadyInQueueException::class)
  fun requeueingQueuedReceiptThrowsException() {
    repository.queue(anyReceipt)

    repository.reQueue(anyReceipt.receiptId)
  }

  private fun tailCursor(repo: ReceiptRepository,
                         collection: MongoCollection<Document>) {
    while (true) {
      collection.find()
        .cursorType(CursorType.TailableAwait)
        .noCursorTimeout(true)
        .iterator().use {
          while (it.hasNext()) {
            val receiptDoc = it.next()
            repo.finishPrinting(receiptDoc["receiptId"].toString())
          }
        }
    }
  }
}