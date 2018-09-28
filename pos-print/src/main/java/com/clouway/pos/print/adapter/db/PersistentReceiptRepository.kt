package com.clouway.pos.print.adapter.db

import com.clouway.pos.print.core.*
import com.google.inject.Inject
import com.google.inject.Provider
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates.set
import org.bson.Document
import java.util.Optional

/**
 * @author Tsvetozar Bonev (tsvetozar.bonev@clouway.com)
 */
class PersistentReceiptRepository @Inject constructor(private val database: Provider<MongoDatabase>,
                                                      private val printingListener: PrintingListener)
  : ReceiptRepository {
  private val collectionName: String = "receipts"

  override fun register(receiptRequest: ReceiptRequest): String {
    val receipt = receiptRequest.receipt

    if (receipts().find(eq("receiptId", receipt.receiptId)).firstOrNull() != null)
      throw ReceiptAlreadyInQueueException()

    val receiptDoc = receiptRequest.toDocument()

    receipts().insertOne(receiptDoc)

    return receipt.receiptId
  }

  override fun getByReceiptId(receiptId: String): Optional<ReceiptRequest> {
    val receiptDoc = receipts().find(
      eq("receiptId", receiptId)).firstOrNull()
      ?: return Optional.empty()

    return Optional.of(receiptDoc.toReceiptRequest())
  }

  override fun getByOperatorId(operatorId: String): List<ReceiptRequest> {
    val docList = receipts().find(
      eq("operatorId", operatorId)).toList()

    val receiptList = mutableListOf<ReceiptRequest>()

    docList.forEach {
      receiptList.add(it.toReceiptRequest())
    }

    return receiptList
  }

  override fun getBySourceIp(sourceIp: String): List<ReceiptRequest> {
    val docList = receipts().find(
      eq("sourceIp", sourceIp)).toList()

    val receiptList = mutableListOf<ReceiptRequest>()

    docList.forEach {
      receiptList.add(it.toReceiptRequest())
    }

    return receiptList
  }

  override fun getStatus(receiptId: String): PrintStatus {
    val receiptDoc = receipts().find(
      eq("receiptId", receiptId)).firstOrNull()
      ?: throw ReceiptNotInQueueException()

    return PrintStatus.valueOf(receiptDoc.getString("status"))
  }

  override fun getByStatus(receiptStatus: PrintStatus): List<Receipt> {
    val receiptDocs = receipts().find(eq("status", receiptStatus.name))

    val receiptList = mutableListOf<Receipt>()

    receiptDocs.forEach {
      receiptList.add(it.toReceipt())
    }

    return receiptList
  }

  override fun finishPrinting(receiptId: String): Receipt {
    val receiptDoc = receipts().findOneAndUpdate(
      eq("receiptId", receiptId),
      set("status", PrintStatus.PRINTED.name))
      ?: throw ReceiptNotInQueueException()

    printingListener.onPrinted(receiptDoc.toReceipt(), PrintStatus.PRINTED)

    return receiptDoc.toReceipt()
  }

  override fun failPrinting(receiptId: String): Receipt {
    val receiptDoc = receipts().findOneAndUpdate(
      eq("receiptId", receiptId),
      set("status", PrintStatus.FAILED.name))
      ?: throw ReceiptNotInQueueException()

    printingListener.onPrinted(receiptDoc.toReceipt(), PrintStatus.FAILED)

    return receiptDoc.toReceipt()
  }

  private fun receipts(): MongoCollection<Document> {
    return database.get().getCollection(collectionName)
  }

  @Suppress("UNCHECKED_CAST")
  private fun Document.toReceipt(): Receipt {
    val receipt = Receipt.Builder()
      .withReceiptId(this.getString("receiptId"))
      .withAmount(this.getDouble("amount"))
      .currency(this.getString("currency"))
      .prefixLines(this["prefixLines"] as List<String>)
      .suffixLines(this["suffixLines"] as List<String>)

    val itemList = this["receiptItems"] as List<Document>

    itemList.forEach {
      receipt.addItem(it.toReceiptItem())
    }

    return receipt.build()
  }

  private fun Document.toReceiptItem(): ReceiptItem{
    return ReceiptItem.newItem()
      .name(this.getString("name"))
      .price(this.getDouble("price"))
      .quantity(this.getDouble("quantity"))
      .build()
  }

  private fun Receipt.toDocument(): Document{
    val itemList = this.receiptItems

    val docList = mutableListOf<Document>()

    itemList.forEach {
      docList.add(it.toDocument())
    }

    return Document()
      .append("receiptId", this.receiptId)
      .append("amount", this.amount)
      .append("prefixLines", this.prefixLines())
      .append("suffixLines", this.suffixLines())
      .append("currency", this.currency)
      .append("receiptItems", docList)
      .append("status", PrintStatus.PRINTING.name)
  }

  private fun ReceiptRequest.toDocument(): Document{
    val receiptDoc = this.receipt.toDocument()

    receiptDoc["sourceI p"] = this.sourceIp
    receiptDoc["operatorId"] = this.operatorId
    receiptDoc["isFiscal"] = this.isFiscal

    return receiptDoc
  }

  private fun Document.toReceiptRequest(): ReceiptRequest{
    val receipt = this.toReceipt()

    return ReceiptRequest(receipt,
      this["sourceIp"] as String,
      this["operatorId"] as String,
      this["isFiscal"] as Boolean)
  }

  private fun ReceiptItem.toDocument(): Document{
    return Document()
      .append("name", this.name)
      .append("price", this.price)
      .append("quantity", this.quantity)
  }
}