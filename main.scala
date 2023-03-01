import scala.io.Source

import org.apache.pekko.actor.typed._
import org.apache.pekko.actor.typed.scaladsl._

import spray.json._
import DefaultJsonProtocol._

import cachedhttp.HttpCache
import cachedgithub.GitHubCache
import scorecard.Scorecard
import scorecard.Scorecards

case class GitHubRepo(
  name: String,
  archived: Boolean
)

object GitHubRepo:
  given JsonReader[GitHubRepo] = jsonFormat2(GitHubRepo.apply)

case class Project(
  doap: Option[String],
  pmc: String,
  repository: Option[List[String]],
)
object Project:
  given JsonReader[Project] = jsonFormat3(Project.apply)

case class Committee(id: String, name: String)
object Committee:
  given JsonReader[Committee] = jsonFormat2(Committee.apply)

@main
def main =
  implicit val system = ActorSystem[Any](Behaviors.empty, "main")
  val http = new HttpCache
  val github = new GitHubCache
  val podlings = http.get("podlings", s"https://projects.apache.org/json/foundation/podlings.json")
    .parseJson
    .asJsObject
    .fields
    .mapValues(_.convertTo[Project])
    .toSeq
  val projects = http.get("projects", s"https://projects.apache.org/json/foundation/projects.json")
    .parseJson
    .asJsObject
    .fields
    .mapValues(_.convertTo[Project])
    .toSeq
  val ps = podlings ++ projects
  val committees = http.get("committees", s"https://projects.apache.org/json/foundation/committees.json")
    .parseJson
    .asInstanceOf[JsArray]
    .elements
    .map(_.convertTo[Committee])
    .toSeq
  val githubRepos = github.get("repos", s"https://api.github.com/orgs/apache/repos")
    .parseJson
    .asInstanceOf[JsArray]
    .elements
    .map(_.convertTo[GitHubRepo])
    .filterNot(_.archived)
    .filterNot(_.name.endsWith("site"))
  println(s"GitHub repos: ${githubRepos.size}");
  val mapped = githubRepos.map(r => (r,
    committees.filter (p => r.name.startsWith(p.id) || r.name.startsWith(s"incubator-${p.id}")).headOption)
  )
  println(s"GitHub repos starting with a project name: ${mapped.filter(_._2.isDefined).size}");
  // Infrastructure, security, some other leftovers
  //println(s"GitHub repos not starting with a project name: ${mapped.filterNot(_._2.isDefined).size}");
  val columns = Seq(
    "Binary-Artifacts", "Branch-Protection", "CI-Tests", "CII-Best-Practices", "Code-Review", "Contributors", "Dangerous-Workflow", "Dependency-Update-Tool", "Fuzzing", "License", "Maintained", "Packaging", "Pinned-Dependencies", "SAST", "Security-Policy", "Signed-Releases", "Token-Permissions", "Vulnerabilities"
  )
  print("committee\trepo\toverall\t" + columns.mkString("\t") + "\n")
  committees.foreach(c =>
    //println(s"${c.name}:")
    val populated = githubRepos.filter(_.name.startsWith(c.id)).filter(r => Scorecards.has(r.name))
    if (populated.nonEmpty) {
      populated.foreach { r =>
        print(s"${c.name}\t${r.name}\t")
        val s = Scorecards.get(r.name)
        columns.map { column =>
          print(s.score + "\t" + s.checks.filter(_.name == column).headOption.map(_.score).getOrElse("-1"))
          print("\t")
        }
        print("\n")
      }
    }
  )
