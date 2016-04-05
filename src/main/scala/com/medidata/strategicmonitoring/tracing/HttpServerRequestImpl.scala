package com.medidata.strategicmonitoring.tracing

import com.github.kristofa.brave.http.HttpServerRequest
import akka.http.scaladsl.model.HttpRequest
import java.net.URI;
import akka.http.scaladsl.model.headers._
//import org.slf4j.LoggerFactory
import com.github.kristofa.brave.http.BraveHttpHeaders

//TODO refactor common code
case class HttpServerRequestImpl(var akkaHttpRequest: akka.http.scaladsl.model.HttpRequest) extends com.github.kristofa.brave.http.HttpServerRequest {

  //val logger = LoggerFactory.getLogger(getClass)

  def getUri(): URI = { 
    
    val uri = new java.net.URI(akkaHttpRequest.getUri().path())
    //logger.info ("HttServerRequestImpl::getUri called, uri=|" + uri + "|")
    uri   
 }

  def getHttpMethod(): String = { 
    
    val httpMethod = akkaHttpRequest.method.value
    //logger.info ("HttServerRequestImpl::getHttpMethod called, httpMethod=|" + httpMethod + "|")
    httpMethod
  }
   
  def getHttpHeaderValue(headerName: String): String = { 
    
    val httpHeaderOption = akkaHttpRequest.getHeader(headerName)
    
    if (httpHeaderOption.isEmpty) {
    
      //logger.info ("HttServerRequestImpl::getHttpHeaderValue called, headerName=|" + headerName + "|, returning null")
      null
    } else {
      
      val value = httpHeaderOption.get.value
      //logger.info ("HttServerRequestImpl::getHttpHeaderValue called, headerName=|" + headerName + "|, value = |" + value + "|")
      value
    }
  }
  
  //TODO refactor common code
  private def getId(braveHttpHeaders: BraveHttpHeaders): String = {
  
    val idOption = akkaHttpRequest.getHeader(braveHttpHeaders.getName())
    
    if (idOption.isEmpty) {
    
      "0"
    } else {
      
      //careful Brave cant handle UUID's with - or uppercase in them - so format UUID to correct format
      //UUID.toString prints in wrong format...
      idOption.get.value.replaceAll("-", "").toLowerCase()
    }
  }
  
  def getSampled() : String = {

    getId (BraveHttpHeaders.Sampled)
  }
  
  def getSpanId() : String = {
    
    getId (BraveHttpHeaders.SpanId)
  }
  
  def getParentSpanId(): String = {
 
    getId (BraveHttpHeaders.ParentSpanId)
  }
  
  def getTraceId(): String = {

    getId (BraveHttpHeaders.TraceId)
  }
  
  def hasSpanId() : Boolean = {
    
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
