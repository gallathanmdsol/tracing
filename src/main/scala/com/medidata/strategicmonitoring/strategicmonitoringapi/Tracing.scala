package main.scala

import org.slf4j.LoggerFactory
import com.typesafe.config.ConfigFactory
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.RouteResult.route2HandlerFlow
import akka.stream.ActorMaterializer
import com.medidata.strategicmonitoring.tracing.TracingController
import akka.http.scaladsl.server.Directives._

//boilerplate, not much to see here
object Tracing extends App {

  val logger = LoggerFactory.getLogger(getClass())

  val config = ConfigFactory.load();
  implicit val system = ActorSystem("Tracing", config)
  implicit val materializer = ActorMaterializer()

  import system.dispatcher
    
  val bindingFuture = Http().bindAndHandle(TracingController.getRoutes(), "localhost", 8080)
  logger.info("Tracing online at http://localhost:8080\nPress RETURN to stop...")
  Console.readLine() // for the future transformations
  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ ⇒ system.shutdown()) // and shutdown when done
}
