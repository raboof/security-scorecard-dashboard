package scorecard

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.duration._
import sys.process.Process
import sys.process.ProcessIO

import org.apache.commons.io.IOUtils

import spray.json._
import DefaultJsonProtocol._

import cached.Cached
case class Repo(name: String, commit: String)
object Repo:
  given JsonFormat[Repo] = jsonFormat2(Repo.apply)
  
case class Check(name: String, score: Int, reason: String, details: Option[Seq[String]])
object Check:
  given JsonFormat[Check] = jsonFormat4(Check.apply)

case class Scorecard(repo: Repo, score: Double, checks: Option[Seq[Check]])
object Scorecard:
  given JsonReader[Scorecard] = jsonFormat3(Scorecard.apply)

object Scorecards extends Cached[String](identity):
  def has(repo: String): Boolean =
    has(s"score-$repo", s"https://github.com/apache/$repo")
  def get(repo: String): Scorecard =
    println(s"Getting $repo")
    get(s"score-$repo", s"https://github.com/apache/$repo")
      .parseJson
      .convertTo[Scorecard]
  def get(repo: String, fetchIfNeeded: Boolean): Option[Scorecard] =
    if fetchIfNeeded then
        Some(get(repo))
    else
        Option.when(has(repo))(get(repo))

  override def fetchUncached(repo: String): Array[Byte] =
    var p = Promise[java.io.InputStream]
    //var e = Promise[java.io.InputStream]
    var seq = Seq("scorecard", s"--repo=$repo", "--format=json", "--show-details", "--verbosity=debug")
    println(s"Running ${seq.mkString(" ")}")
    var proc = Process(seq)
    var process = proc.run(new ProcessIO(
      _.close(),
      (stdout: java.io.InputStream) => {
        p.completeWith(Future.successful(stdout))
      },
      (stderr: java.io.InputStream) => {
        stderr.transferTo(System.out)
        //e.completeWith(Future.successful(stderr))
      },
    ))
    
    given ExecutionContext = ExecutionContext.global
    val ro = Await.result(p.future, 60.seconds)
    // Note: reading the output must be done before checking
    // the exit value, to prevent deadlock
    val result = IOUtils.toByteArray(ro)
    println(s"Done awaiting, exit ${process.exitValue}")
    if process.exitValue() != 0 then
        throw new IllegalStateException(s"Failed to get scorecard for $repo")
    //val errors = IOUtils.toByteArray(re)
    //println("Errors: " + new String(errors))
    println("Output: " + new String(result))
    result
