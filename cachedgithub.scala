package cachedgithub

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.Try
import scala.util.Success

import org.apache.pekko.actor.typed._
import org.apache.pekko.actor.typed.scaladsl._

import org.apache.pekko.stream.scaladsl._

import org.apache.pekko.util._

import org.apache.pekko.http.scaladsl.client.RequestBuilding.Get
import org.apache.pekko.http.scaladsl.client._
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.model.headers.Accept
import org.apache.pekko.http.scaladsl.model.headers.Link
import org.apache.pekko.http.scaladsl.model.headers.LinkParams
import org.apache.pekko.http.scaladsl._
import org.apache.pekko.http._

import spray.json._
import DefaultJsonProtocol._

import cached.Cached

class GitHubCache(using sys: ActorSystem[_]) extends Cached[String](identity):
  val token: Option[String] = scala.sys.env.get("GITHUB_TOKEN")
  override def fetchUncached(url: String): Array[Byte] =
    given ExecutionContext = sys.classicSystem.dispatcher
    println(s"Not cached, fetching $url page 0")
    val authz = token.map(t => headers.Authorization(headers.OAuth2BearerToken(t))).toList
    val resp1 = Await.result(Http(sys.classicSystem).singleRequest(Get(s"$url?per_page=100").mapHeaders(_ ++ authz)), 30.seconds)
    val page1 = resp1.entity.toStrict(10.seconds)
    val (prefix, pages) =
      val last = resp1
        .headers[Link]
        .flatMap(_.values)
        .filter(_.params.contains(LinkParams.last))
        .headOption match {
            case Some(last) => last.uri.toString
            case None => throw new IllegalStateException(s"No last header - TODO handle rate limit expiration ${resp1.headers.mkString}")
        }
      val split = last.split("&page=")
      (split(0) + "&page=", split(1).toInt)
    val responses: Future[Seq[Future[HttpEntity.Strict]]] = Source(Range(2, pages))
      .map(page => (Get(prefix + page).mapHeaders(_ ++ authz), page))
      .via(Http().superPool())
      .map {
          case (Success(res), i) => (res.entity.toStrict(10.seconds), i)
      }
      .runWith(Sink.seq[(Future[HttpEntity.Strict], Int)])
      .map(seq => page1 +: (seq.sortBy(_._2).map(_._1)))

    val entities: Future[Seq[HttpEntity.Strict]] =
      responses.flatMap(pages => Future.sequence(pages))

    val data: Future[JsArray] = entities.map { e =>
      JsArray(e.map(_.data.utf8String.parseJson match {
          case JsArray(elements) => elements
          case other => throw new IllegalStateException(s"Expected array, got $other")
        }).reduce(_ ++ _))
    }

    Await.result(data, 60.seconds).prettyPrint.getBytes
