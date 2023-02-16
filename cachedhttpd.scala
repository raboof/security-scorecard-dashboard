package cachedhttp

import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.duration._

import org.apache.pekko.actor.typed._
import org.apache.pekko.actor.typed.scaladsl._

import cached.Cached

class HttpCache(implicit sys: ActorSystem[_]) extends Cached[String](identity):
  override def fetchUncached(url: String): Array[Byte] =
    import org.apache.pekko.http.scaladsl.client.RequestBuilding.Get
    import org.apache.pekko.http.scaladsl.client._
    import org.apache.pekko.http.scaladsl.model._
    import org.apache.pekko.http.scaladsl.model.headers.Accept
    import org.apache.pekko.http.scaladsl._
    import org.apache.pekko.http._
    println(s"Not cached, fetching $url")
    val res = Await.result(Http(sys.classicSystem).singleRequest(
      Get(url).addHeader(Accept(MediaRanges.`*/*`))),
      30.seconds
    )  
    Await.result(res.entity.toStrict(10.seconds), 10.seconds).data.toArray
