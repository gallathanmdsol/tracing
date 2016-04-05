README file for this tracing project

Who: Graham Allathan gallathan@mdsol.com
When: 01/04/2016
Why: As a POC of tracing wishing a Scala Application.
More specifically, we’ve decided to standardise on tracing using Open Pipkin

Roughly Google wrote a paper about distributed tracing called Dapper
http://research.google.com/pubs/pub36356.html

Twitter implemented it in a project called Zipkin
https://blog.twitter.com/2012/distributed-systems-tracing-with-zipkin

We want to use an open source version of Zipkin called OpenZipkin
https://github.com/openzipkin.

This provides the UI to see the data - we need some client libs to send data to this via Http

So we - reuse a Java library called Brave for this
https://github.com/openzipkin/brave

Brave has been used internally in Medidata, BUT there are some really important differences if were going to use it with Scala / Akka-Http (our HTTP lib fro the mid tier) and AKKA/Actors

Brave, out of the box does NOT support kaka-http, it requites some adapter classes to be implemented, look @ the source files

Also, Brave, by default uses Thread local data to store the SpanId, TraceId and ParentSpanId that are central to reliable tracing

We CANNOT use this in Scala / AKKA as different Threads will run different Actors, and the thread running a given Actor may CHANGE over time. This means the 3 aforementioned Id’s will get LOST and/or CORRUPTED

BUT, you can extract the id’s in you code easily, and pass them as immutable parameters as part of the messages used to communicate between actors. Problem solved. Please examine the src code to see an example of doing this.

There is a sandbox zipkin environment, the url is embedded in the source code.



