package com.mediddata.strategicmonioring.strategicmonitoringapi

import scala.concurrent.duration.DurationInt
import akka.util.Timeout

trait ImplicitTimeout {

  implicit val timeout = Timeout(5 seconds) //set to network timeout for rest call
}