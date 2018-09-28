package com.clouway.pos.print.core

/**
 * @author Tsvetozar Bonev (tsvetozar.bonev@clouway.com)
 */
data class ReceiptRequest(val receipt: Receipt,
                          val sourceIp: String,
                          val operatorId: String,
                          val isFiscal: Boolean)