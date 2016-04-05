package com.medidata.strategicmonitoring.tracing

import com.github.kristofa.brave.http.HttpResponse
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.headers._
//import org.slf4j.LoggerFactory
import com.github.kristofa.brave.http.BraveHttpHeaders

case class HttpResponseImpl(akkaHttpResponse: akka.http.scaladsl.model.HttpResponse) extends com.github.kristofa.brave.http.HttpResponse {

  //val logger = LoggerFactory.getLogger(getClass)

  def getHttpStatusCode(): Int = {

    val statusCode = akkaHttpResponse.status.intValue()
    //logger.info("HttpResponseImpl::getHttpStatusCode called, statusCode=|" + statusCode + "|")
    statusCode
  }

  //TODO refactor common code
  private def getId(braveHttpHeaders: BraveHttpHeaders): String = {

    val idOption = akkaHttpResponse.getHeader(braveHttpHeaders.getName())

    if (idOption.isEmpty) {

      "0"
    } else {

      //careful Brave cant handle UUID's with - or uppercase in them - so format UUID to correct format
      //UUID.toString prints in wrong format...
      idOption.get.value.replaceAll("-", "").toLowerCase()
    }
  }

  def getSampled(): String = {

    getId(BraveHttpHeaders.Sampled)
  }

  def getSpanId(): String = {

    getId(BraveHttpHeaders.SpanId)
  }

  def getParentSpanId(): String = {

    getId(BraveHttpHeaders.ParentSpanId)
  }

  def getTraceId(): String = {

    getId(BraveHttpHeaders.TraceId)
  }

  def hasSpanId(): Boolean = {

    !akkaHttpResponse.getHeader(BraveHttpHeaders.SpanId.getName()).isEmpty
  }

  def hasParentSpanId(): Boolean = {

    !akkaHttpResponse.getHeader(BraveHttpHeaders.ParentSpanId.getName()).isEmpty
  }

  def hasTraceId(): Boolean = {

    !akkaHttpResponse.getHeader(BraveHttpHeaders.TraceId.getName()).isEmpty
  }

  def hasSampled(): Boolean = {

    !akkaHttpResponse.getHeader(BraveHttpHeaders.Sampled.getName()).isEmpty
  }
}
