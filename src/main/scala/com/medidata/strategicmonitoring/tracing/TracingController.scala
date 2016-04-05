package com.medidata.strategicmonitoring.tracing

import scala.concurrent.Await
import org.slf4j.LoggerFactory
import java.time.Instant
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Props
import akka.http.scaladsl.marshalling.ToResponseMarshallable.apply
import akka.http.scaladsl.model.HttpResponse
import akka.http.javadsl.model.headers.RawHeader
import akka.http.scaladsl.server.Directive._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import com.mediddata.strategicmonioring.strategicmonitoringapi.ImplicitTimeout
import com.github.kristofa.brave.Brave
import com.github.kristofa.brave.EmptySpanCollectorMetricsHandler
import com.github.kristofa.brave.ServerResponseAdapter
import com.github.kristofa.brave.ClientResponseAdapter
import com.github.kristofa.brave.ServerRequestAdapter
import com.github.kristofa.brave.ClientRequestAdapter
import com.github.kristofa.brave.Sampler
import com.github.kristofa.brave.http.DefaultSpanNameProvider
import com.github.kristofa.brave.http.HttpSpanCollector
import com.github.kristofa.brave.http.HttpServerResponseAdapter
import com.github.kristofa.brave.http.HttpClientResponseAdapter
import com.github.kristofa.brave.http.HttpServerRequestAdapter
import com.github.kristofa.brave.http.HttpClientRequestAdapter
import com.github.kristofa.brave.http.ServiceNameProvider
import com.github.kristofa.brave.http.BraveHttpHeaders
import java.util.Random

//SINGLETON
object TracingController extends ImplicitTimeout with ServiceNameProvider {

  val logger = LoggerFactory.getLogger(getClass)

  val myRandom = new Random()

  //TODO parameterise myServiceName
  //TODO should pass host / port from application.conf
  val braveBuilder = new Brave.Builder(serviceName())

  val spanCollectorMetricsHandler = new EmptySpanCollectorMetricsHandler()

  //TODO custom span name provider
  val spanNameProvider = new DefaultSpanNameProvider()

  //TODO config for zipkin URL
  val spanCollector = HttpSpanCollector.create("http://zipkin-sandbox.imedidata.net", spanCollectorMetricsHandler)

  //TODO read this param from config
  val sampler = Sampler.create(1.0f)

  val tracing = braveBuilder.spanCollector(spanCollector).traceSampler(sampler).build()

  val serverRequestInterceptor = tracing.serverRequestInterceptor()

  val serverResponseInterceptor = tracing.serverResponseInterceptor()

  val clientRequestInterceptor = tracing.clientRequestInterceptor()

  val clientResponseInterceptor = tracing.clientResponseInterceptor()

  def serviceName(request: com.github.kristofa.brave.http.HttpRequest): String = {

    //logger.info("TracingController::serviceName called, request=|" + request + "|")
    serviceName()
  }

  def serviceName(): String = {

    //should be lowercase
    "myservicename".toLowerCase()
  }

  def httpServerRequest(akkaHttpRequest: akka.http.scaladsl.model.HttpRequest): HttpServerRequestImpl = {

    new HttpServerRequestImpl(akkaHttpRequest)
  }

  def httpResponse(akkaHttpResponse: akka.http.scaladsl.model.HttpResponse): HttpResponseImpl = {

    new HttpResponseImpl(akkaHttpResponse)
  }

  def httpClientRequest(akkaHttpRequest: akka.http.scaladsl.model.HttpRequest): HttpClientRequestImpl = {

    new HttpClientRequestImpl(akkaHttpRequest)
  }

  def serverResponseAdapter(httpResponse: com.github.kristofa.brave.http.HttpResponse): com.github.kristofa.brave.ServerResponseAdapter = {

    new HttpServerResponseAdapter(httpResponse)
  }

  def clientResponseAdapter(httpResponse: com.github.kristofa.brave.http.HttpResponse): com.github.kristofa.brave.ClientResponseAdapter = {

    new HttpClientResponseAdapter(httpResponse)
  }

  def serverRequestAdapter(httpServerRequest: com.github.kristofa.brave.http.HttpServerRequest): com.github.kristofa.brave.ServerRequestAdapter = {

    new HttpServerRequestAdapter(httpServerRequest, spanNameProvider)
  }

  def clientRequestAdapter(httpClientRequest: com.github.kristofa.brave.http.HttpClientRequest): com.github.kristofa.brave.ClientRequestAdapter = {

    new HttpClientRequestAdapter(httpClientRequest, this, spanNameProvider)
  }

  def getRoutes() = {

    logger.info("TracingController::getRoutes called")

    extractRequest { request =>

      var myAkkaHttpRequest = request

      val myHttpClientRequestImpl = httpClientRequest(myAkkaHttpRequest)

      val myClientRequestAdapter = clientRequestAdapter(myHttpClientRequestImpl)
      logger.info("\n\n\nTracingController::getRoutes called, calling clientRequestInterceptor.handle")
      clientRequestInterceptor.handle(myClientRequestAdapter)

      //you need to EXTRACT these value to pass to other Actors as params
      //ONLY safe way to pass data as java Brave uses ThreadLocal data that will be LOST / corrupted on context switch between Actors...
      var mySpanId = myHttpClientRequestImpl.getTraceId()
      var myTraceId = myHttpClientRequestImpl.getSpanId()
      var myParentSpanId = myHttpClientRequestImpl.getParentSpanId()
      var mySampled = myHttpClientRequestImpl.getSampled()

      //logger.info("TracingController::getRoutes called, myHttpClientRequestImpl.hasTraceId() = |" + myHttpClientRequestImpl.hasTraceId() + "|")
      //logger.info("TracingController::getRoutes called, myHttpClientRequestImpl.getTraceId() = |" + myHttpClientRequestImpl.getTraceId() + "|")
      //logger.info("TracingController::getRoutes called, myHttpClientRequestImpl.hasSpanId() = |" + myHttpClientRequestImpl.hasSpanId() + "|")
      //logger.info("TracingController::getRoutes called, myHttpClientRequestImpl.getSpanId() = |" + myHttpClientRequestImpl.getSpanId() + "|")
      //logger.info("TracingController::getRoutes called, myHttpClientRequestImpl.hasParentSpanId() = |" + myHttpClientRequestImpl.hasParentSpanId() + "|")
      //logger.info("TracingController::getRoutes called, myHttpClientRequestImpl.getParentSpanId() = |" + myHttpClientRequestImpl.getParentSpanId() + "|")
      //logger.info("TracingController::getRoutes called, myHttpClientRequestImpl.hasSampled() = |" + myHttpClientRequestImpl.hasSampled() + "|")
      //logger.info("TracingController::getRoutes called, myHttpClientRequestImpl.getSampled() = |" + myHttpClientRequestImpl.getSampled() + "|")
      logger.info("TracingController::getRoutes called, mySpanId = |" + mySpanId + "|")
      logger.info("TracingController::getRoutes called, myTraceId = |" + myTraceId + "|")
      logger.info("TracingController::getRoutes called, myParentSpanId = |" + myParentSpanId + "|")
      logger.info("TracingController::getRoutes called, mySampled = |" + mySampled + "|")

      //simulate client calling server
      //careful - addHeader returns new request with header, doesn't add to original request
      myAkkaHttpRequest = myAkkaHttpRequest.addHeader(RawHeader.create(BraveHttpHeaders.Sampled.getName(), mySampled))
      myAkkaHttpRequest = myAkkaHttpRequest.addHeader(RawHeader.create(BraveHttpHeaders.SpanId.getName(), mySpanId))
      myAkkaHttpRequest = myAkkaHttpRequest.addHeader(RawHeader.create(BraveHttpHeaders.TraceId.getName(), myTraceId))
      myAkkaHttpRequest = myAkkaHttpRequest.addHeader(RawHeader.create(BraveHttpHeaders.ParentSpanId.getName(), myRandom.nextLong().toHexString))

      val myHttpServerRequestImpl = httpServerRequest(myAkkaHttpRequest)
      val myServerRequestAdapter = serverRequestAdapter(myHttpServerRequestImpl)
      logger.info("\n\nTracingController::getRoutes called, calling serverRequestInterceptor.handle")
      serverRequestInterceptor.handle(myServerRequestAdapter)

      //you need to EXTRACT these value to pass to other Actors as params
      //ONLY safe way to pass data as java Brave uses ThreadLocal data that will be LOST / corrupted on context switch between Actors...
      mySpanId = myHttpServerRequestImpl.getTraceId()
      myTraceId = myHttpServerRequestImpl.getSpanId()
      myParentSpanId = myHttpServerRequestImpl.getParentSpanId()
      mySampled = myHttpServerRequestImpl.getSampled()

      //logger.info("TracingController::getRoutes called, myHttpServerRequestImpl.hasTraceId() = |" + myHttpServerRequestImpl.hasTraceId() + "|")
      //logger.info("TracingController::getRoutes called, myHttpServerRequestImpl.getTraceId() = |" + myHttpServerRequestImpl.getTraceId() + "|")
      //logger.info("TracingController::getRoutes called, myHttpServerRequestImpl.hasSpanId() = |" + myHttpServerRequestImpl.hasSpanId() + "|")
      //logger.info("TracingController::getRoutes called, myHttpServerRequestImpl.getSpanId() = |" + myHttpServerRequestImpl.getSpanId() + "|")
      //logger.info("TracingController::getRoutes called, myHttpServerRequestImpl.hasParentSpanId() = |" + myHttpServerRequestImpl.hasParentSpanId() + "|")
      //logger.info("TracingController::getRoutes called, myHttpServerRequestImpl.getParentSpanId() = |" + myHttpServerRequestImpl.getParentSpanId() + "|")
      //logger.info("TracingController::getRoutes called, myHttpServerRequestImpl.hasSampled() = |" + myHttpServerRequestImpl.hasSampled() + "|")
      //logger.info("TracingController::getRoutes called, myHttpServerRequestImpl.getSampled() = |" + myHttpServerRequestImpl.getSampled() + "|")
      logger.info("TracingController::getRoutes called, mySpanId = |" + mySpanId + "|")
      logger.info("TracingController::getRoutes called, myTraceId = |" + myTraceId + "|")
      logger.info("TracingController::getRoutes called, myParentSpanId = |" + myParentSpanId + "|")
      logger.info("TracingController::getRoutes called, mySampled = |" + mySampled + "|")

      //server request -> server response
      var myAkkaHttpResponse = new akka.http.scaladsl.model.HttpResponse(status = akka.http.scaladsl.model.StatusCodes.OK)
      myAkkaHttpResponse = myAkkaHttpResponse.addHeader(RawHeader.create(BraveHttpHeaders.Sampled.getName(), mySampled))
      myAkkaHttpResponse = myAkkaHttpResponse.addHeader(RawHeader.create(BraveHttpHeaders.SpanId.getName(), mySpanId))
      myAkkaHttpResponse = myAkkaHttpResponse.addHeader(RawHeader.create(BraveHttpHeaders.TraceId.getName(), myTraceId))
      myAkkaHttpResponse = myAkkaHttpResponse.addHeader(RawHeader.create(BraveHttpHeaders.ParentSpanId.getName(), myParentSpanId))

      val myHttpServerResponseImpl = httpResponse(myAkkaHttpResponse)

      val myServerResponseAdapter = serverResponseAdapter(myHttpServerResponseImpl)
      logger.info("\n\nTracingController::getRoutes called, calling serverResponseInterceptor.handle")
      serverResponseInterceptor.handle(myServerResponseAdapter)

      //you need to EXTRACT these value to pass to other Actors as params
      //ONLY safe way to pass data as java Brave uses ThreadLocal data that will be LOST / corrupted on context switch between Actors...
      mySpanId = myHttpServerResponseImpl.getTraceId()
      myTraceId = myHttpServerResponseImpl.getSpanId()
      myParentSpanId = myHttpServerResponseImpl.getParentSpanId()
      mySampled = myHttpServerResponseImpl.getSampled()

      //logger.info("TracingController::getRoutes called, myHttpServerResponseImpl.hasTraceId() = |" + myHttpServerResponseImpl.hasTraceId() + "|")
      //logger.info("TracingController::getRoutes called, myHttpServerResponseImpl.getTraceId() = |" + myHttpServerResponseImpl.getTraceId() + "|")
      //logger.info("TracingController::getRoutes called, myHttpServerResponseImpl.hasSpanId() = |" + myHttpServerResponseImpl.hasSpanId() + "|")
      //logger.info("TracingController::getRoutes called, myHttpServerResponseImpl.getSpanId() = |" + myHttpServerResponseImpl.getSpanId() + "|")
      //logger.info("TracingController::getRoutes called, myHttpServerResponseImpl.hasParentSpanId() = |" + myHttpServerResponseImpl.hasParentSpanId() + "|")
      //logger.info("TracingController::getRoutes called, myHttpServerResponseImpl.getParentSpanId() = |" + myHttpServerResponseImpl.getParentSpanId() + "|")
      //logger.info("TracingController::getRoutes called, myHttpServerResponseImpl.hasSampled() = |" + myHttpServerResponseImpl.hasSampled() + "|")
      //logger.info("TracingController::getRoutes called, myHttpServerResponseImpl.getSampled() = |" + myHttpServerResponseImpl.getSampled() + "|")
      logger.info("TracingController::getRoutes called, mySpanId = |" + mySpanId + "|")
      logger.info("TracingController::getRoutes called, myTraceId = |" + myTraceId + "|")
      logger.info("TracingController::getRoutes called, myParentSpanId = |" + myParentSpanId + "|")
      logger.info("TracingController::getRoutes called, mySampled = |" + mySampled + "|")

      //server response -> client response
      val myHttpClientResponseImpl = httpResponse(myAkkaHttpResponse)

      val myClientResponseAdapter = clientResponseAdapter(myHttpClientResponseImpl)
      logger.info("\n\nTracingController::getRoutes called, calling clientResponseInterceptor.handle")
      clientResponseInterceptor.handle(myClientResponseAdapter)

      //you need to EXTRACT these value to pass to other Actors as params
      //ONLY safe way to pass data as java Brave uses ThreadLocal data that will be LOST / corrupted on context switch between Actors...
      mySpanId = myHttpClientResponseImpl.getTraceId()
      myTraceId = myHttpClientResponseImpl.getSpanId()
      myParentSpanId = myHttpClientResponseImpl.getParentSpanId()
      mySampled = myHttpClientResponseImpl.getSampled()

      //logger.info("TracingController::getRoutes called, myHttpClientResponseImpl.hasTraceId() = |" + myHttpClientResponseImpl.hasTraceId() + "|")
      //logger.info("TracingController::getRoutes called, myHttpClientResponseImpl.getTraceId() = |" + myHttpClientResponseImpl.getTraceId() + "|")
      //logger.info("TracingController::getRoutes called, myHttpClientResponseImpl.hasSpanId() = |" + myHttpClientResponseImpl.hasSpanId() + "|")
      //logger.info("TracingController::getRoutes called, myHttpClientResponseImpl.getSpanId() = |" + myHttpClientResponseImpl.getSpanId() + "|")
      //logger.info("TracingController::getRoutes called, myHttpClientResponseImpl.hasParentSpanId() = |" + myHttpClientResponseImpl.hasParentSpanId() + "|")
      //logger.info("TracingController::getRoutes called, myHttpClientResponseImpl.getParentSpanId() = |" + myHttpClientResponseImpl.getParentSpanId() + "|")
      //logger.info("TracingController::getRoutes called, myHttpClientResponseImpl.hasSampled() = |" + myHttpClientResponseImpl.hasSampled() + "|")
      //logger.info("TracingController::getRoutes called, myHttpClientResponseImpl.getSampled() = |" + myHttpClientResponseImpl.getSampled() + "|")
      logger.info("TracingController::getRoutes called, mySpanId = |" + mySpanId + "|")
      logger.info("TracingController::getRoutes called, myTraceId = |" + myTraceId + "|")
      logger.info("TracingController::getRoutes called, myParentSpanId = |" + myParentSpanId + "|")
      logger.info("TracingController::getRoutes called, mySampled = |" + mySampled + "|")

      complete(myAkkaHttpResponse)
    } //extractRequest
  } //getRoutes
} //TracingController
