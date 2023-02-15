import scala.io.Source

import org.apache.pekko.actor.typed._
import org.apache.pekko.actor.typed.scaladsl._

import spray.json._
import DefaultJsonProtocol._

import cachedhttp.getCached

case class GitHubRepo(
  name: String
)

object GitHubRepo:
  given JsonReader[GitHubRepo] = jsonFormat1(GitHubRepo.apply)


case class Project(
  doap: String,
  pmc: String,
  repository: Option[List[String]],
)
object Project:
  given JsonReader[Project] = jsonFormat3(Project.apply)

@main
def main =
  implicit val system = ActorSystem[Any](Behaviors.empty, "main")
  val projects = getCached("projects", s"https://projects.apache.org/json/foundation/projects.json")
    .parseJson
    .asJsObject
    .fields
    .mapValues(_.convertTo[Project])
    .toSeq
  val githubRepos = getCached("repos", s"https://api.github.com/orgs/apache/repos?per_page=100")
    .parseJson
    .asInstanceOf[JsArray]
    .elements
    .map(_.convertTo[GitHubRepo])
  println(githubRepos);
