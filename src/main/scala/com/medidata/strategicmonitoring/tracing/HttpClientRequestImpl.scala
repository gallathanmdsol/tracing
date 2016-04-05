package com.medidata.strategicmonitoring.tracing

import com.github.kristofa.brave.http.HttpClientRequest
import akka.http.scaladsl.model.HttpRequest
import java.net.URI;
import akka.http.scaladsl.model.headers._
//import org.slf4j.LoggerFactory
import com.github.kristofa.brave.http.BraveHttpHeaders

case class HttpClientRequestImpl(var akkaHttpRequest: akka.http.scaladsl.model.HttpRequest) extends com.github.kristofa.brave.http.HttpClientRequest {

  //val logger = LoggerFactory.getLogger(getClass)

  def getUri(): URI = {

    val uri = new java.net.URI(akkaHttpRequest.getUri().path())
    //logger.info ("HttClientRequestImpl::getUri called, uri=|" + uri + "|")
    uri
  }

  def getHttpMethod(): String = {

    val httpMethod = akkaHttpRequest.method.value
    //logger.info ("HttClientRequestImpl::getHttpMethod called, httpMethod=|" + httpMethod + "|")
    httpMethod
  }

  def addHeader (header: String, value: String): Unit = {
    
      akkaHttpRequest = akkaHttpRequest.addHeader(new RawHeader(header, value))  
  }
  
  private def getId(braveHttpHeaders: BraveHttpHeaders): String = {

    val idOption = akkaHttpRequest.getHeader(braveHttpHeaders.getName())

    if (idOption.isEmpty) {

      "0"
    } else {

      //careful Brave can't handle UUID's with - or uppercase in them - so format UUID to correct format
      //UUID.toString prints in wrong format...
      //must be lowercase letters AND <= 16 characters...
      idOption.get.value.replaceAll("-", "").toLowerCase().substring(16)
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

    !akkaHttpRequest.getHeader(BraveHttpHeaders.SpanId.getName()).isEmpty
  }

  def hasParentSpanId(): Boolean = {

    !akkaHttpRequest.getHeader(BraveHttpHeaders.ParentSpanId.getName()).isEmpty
  }

  def hasTraceId(): Boolean = {

    !akkaHttpRequest.getHeader(BraveHttpHeaders.TraceId.getName()).isEmpty
  }

  def hasSampled(): Boolean = {

    !akkaHttpRequest.getHeader(BraveHttpHeaders.Sampled.getName()).isEmpty
  }
}
