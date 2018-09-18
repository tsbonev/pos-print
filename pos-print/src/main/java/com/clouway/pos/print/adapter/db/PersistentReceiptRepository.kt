package com.clouway.pos.print.adapter.db

import com.clouway.pos.print.core.Receipt
import com.clouway.pos.print.core.ReceiptItem
import com.google.inject.Inject
import com.google.inject.Provider
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates.set
import javafx.application.Application.launch
import org.bson.Document
import kotlin.concurrent.thread

/**
 * @author Tsvetozar Bonev (tsvetozar.bonev@clouway.com)
 */
class PersistentReceiptRepository @Inject constructor(private val database: Provider<MongoDatabase>,
                                                      private val collectionName: String = "receipts",
                                                      private val maxDocuments: Long = 1000L)
  : ReceiptRepository {
  override fun queue(receipt: Receipt): String {
    if (receipts().find(eq("receiptId", receipt.receiptId)).firstOrNull() != null)
      throw ReceiptAlreadyInQueueException()

    receipts().insertOne(adaptReceipt(receipt))

    return receipt.receiptId
  }

  override fun getStatus(receiptId: String): ReceiptStatus {
    val receiptDoc = receipts().find(
      eq("receiptId", receiptId)).firstOrNull()
      ?: throw ReceiptNotInQueueException()

    return ReceiptStatus.valueOf(receiptDoc.getString("status"))
  }

  override fun remove(receiptId: String): Receipt {
    val receiptDoc = receipts().findOneAndDelete(
      eq("receiptId", receiptId))
      ?: throw ReceiptNotInQueueException()

    return adaptReceipt(receiptDoc)
  }

  override fun getByStatus(receiptStatus: ReceiptStatus): List<Receipt> {
    val receiptDocs = receipts().find(eq("status", receiptStatus.name))

    val receiptList = mutableListOf<Receipt>()

    receiptDocs.forEach {
      receiptList.add(adaptReceipt(it))
    }

    return receiptList
  }

  override fun finishPrinting(receiptId: String): Receipt {
    val receiptDoc = receipts().findOneAndUpdate(
      eq("receiptId", receiptId),
      set("status", ReceiptStatus.PRINTED.name))
      ?: throw ReceiptNotInQueueException()

    return adaptReceipt(receiptDoc)
  }

  override fun rejectPrinting(receiptId: String): Receipt {
    val receiptDoc = receipts().findOneAndUpdate(
      eq("receiptId", receiptId),
      set("status", ReceiptStatus.REJECTED.name))
      ?: throw ReceiptNotInQueueException()

    return adaptReceipt(receiptDoc)
  }

  override fun reQueue(receiptId: String): Receipt {
    val receiptDoc = receipts().find(
      eq("receiptId", receiptId)).firstOrNull()
      ?: throw ReceiptNotInQueueException()

    if(receiptDoc.getString("status") == ReceiptStatus.PRINTING.name)
      throw ReceiptAlreadyInQueueException()

    receiptDoc["status"] = ReceiptStatus.PRINTING.name

    receipts().updateOne(eq("receiptId", receiptId),
      set("status", ReceiptStatus.PRINTING.name))

    return adaptReceipt(receiptDoc)
  }

  @Suppress("UNCHECKED_CAST")
  private fun adaptReceipt(receiptDoc: Document): Receipt {
    val receipt = Receipt.Builder()
      .withReceiptId(receiptDoc.getString("receiptId"))
      .withAmount(receiptDoc.getDouble("amount"))
      .currency(receiptDoc.getString("currency"))
      .prefixLines(receiptDoc["prefixLines"] as List<String>)
      .suffixLines(receiptDoc["suffixLines"] as List<String>)

    val itemList = receiptDoc["receiptItems"] as List<Document>

    itemList.forEach {
      receipt.addItem(adaptItem(it))
    }

    return receipt.build()
  }

  private fun adaptReceipt(receipt: Receipt): Document {

    val itemList = receipt.receiptItems

    val docList = mutableListOf<Document>()

    itemList.forEach {
      docList.add(adaptItem(it))
    }

    return Document()
      .append("receiptId", receipt.receiptId)
      .append("amount", receipt.amount)
      .append("prefixLines", receipt.prefixLines())
      .append("suffixLines", receipt.suffixLines())
      .append("currency", receipt.currency)
      .append("receiptItems", docList)
      .append("status", ReceiptStatus.PRINTING.name)
  }

  private fun adaptItem(receiptItem: ReceiptItem): Document {
    return Document()
      .append("name", receiptItem.name)
      .append("price", receiptItem.price)
      .append("quantity", receiptItem.quantity)
  }

  private fun adaptItem(receiptDoc: Document): ReceiptItem {
    return ReceiptItem.newItem()
      .name(receiptDoc.getString("name"))
      .price(receiptDoc.getDouble("price"))
      .quantity(receiptDoc.getDouble("quantity"))
      .build()
  }

  private fun receipts(): MongoCollection<Document> {
    //database.get().createCollection(collectionName, CreateCollectionOptions().capped(true).maxDocuments(maxDocuments))
    return database.get().getCollection(collectionName)
  }
}