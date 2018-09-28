package com.clouway.pos.print.adapter.http

import com.clouway.pos.print.core.*
import com.clouway.pos.print.transport.GsonTransport
import com.google.inject.name.Named
import com.google.sitebricks.At
import com.google.sitebricks.headless.Reply
import com.google.sitebricks.headless.Request
import com.google.sitebricks.headless.Service
import com.google.sitebricks.http.Get
import com.google.sitebricks.http.Post
import org.slf4j.LoggerFactory
import javax.inject.Inject

/**
 * @author Tsvetozar Bonev (tsvetozar.bonev@clouway.com)
 */
@Service
@At("/v2/receipts/req/print")
class PrintServiceV2 @Inject constructor(private var repository: ReceiptRepository,
                                         private var queue: PrintQueue) {
  private val logger = LoggerFactory.getLogger(PrintServiceV2::class.java)

  @Post
  fun printReceipt(request: Request): Reply<*> {
    return try {
      val dto = request.read(ReceiptDTO::class.java).`as`(GsonTransport::class.java)

      val receiptRequest = ReceiptRequest(dto.receipt, dto.sourceIp, dto.operatorId, dto.fiscal)

      val receiptId = repository.register(receiptRequest)

      queue.queue(PrintReceiptRequest(dto.receipt, dto.sourceIp, dto.fiscal))

      logger.info("Receipt queued for printing")
      Reply.with(receiptId).status(202)
    } catch (ex: ReceiptAlreadyInQueueException) {
      logger.error("Receipt is already in queue")
      Reply.saying<Unit>().badRequest()
    }
  }

  @Get
  @At("/status/:id")
  fun getReceiptStatus(@Named("id") id: String): Reply<*> {
    return try {
      val receiptStatus = repository.getStatus(id)
      logger.info("Receipt status returned as ${receiptStatus.name}")
      Reply.with(receiptStatus).ok()
    }catch (ex: ReceiptNotInQueueException){
      logger.error("Receipt not found")
      Reply.saying<Unit>().notFound()
    }
  }

  internal data class ReceiptDTO(val sourceIp: String = "", val operatorId: String = "", val fiscal: Boolean = false, val receipt: Receipt = Receipt.newReceipt().build())
}