package cachedhttp

import java.nio.file._

import scala.concurrent.ExecutionContext
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.io.Source

import org.apache.pekko.actor.typed._
import org.apache.pekko.actor.typed.scaladsl._

import org.apache.commons.codec.digest.DigestUtils

val parent: Path = Paths.get("cache")

def getCached(id: String, url: String)(implicit sys: ActorSystem[_]): String =
  require(Files.isDirectory(parent), "directory cache/ should exist")
  // sparql url commonly too long for fs..
  //val file = parent.resolve(url.filter(_.isLetterOrDigit))
  val file = parent.resolve(id + "-" + DigestUtils.sha256Hex(url))
  
  if Files.exists(file) then
    Source.fromFile(file.toFile).mkString
  else
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
    val bytes = Await.result(res.entity.toStrict(10.seconds), 10.seconds).data.toArray
    Files.write(file, bytes)
    new String(bytes)
