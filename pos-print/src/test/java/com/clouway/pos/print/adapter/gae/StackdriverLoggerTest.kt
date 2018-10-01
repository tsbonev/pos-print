package com.clouway.pos.print.adapter.gae

import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.logging.LoggingOptions
import org.junit.Test
import org.slf4j.LoggerFactory
import java.io.FileInputStream

/**
 * @author Tsvetozar Bonev (tsvetozar.bonev@clouway.com)
 */
class StackdriverLoggerTest {

  @Test
  fun logToStrackdriver() {

    val credentials = ServiceAccountCredentials
      .fromStream(FileInputStream("/home/tsvetozar/clouway/workspaces/service-accounts/sacred-union-210613-d8ff0a7f406d.json"))

    val loggingService = LoggingOptions.newBuilder().setCredentials(credentials)
      .build().service

    val logger = LoggerFactory.getLogger(StackdriverLoggerTest::class.java)

    logger.info("This was sent in a non-GAE project using SDK Authentication")
  }
}