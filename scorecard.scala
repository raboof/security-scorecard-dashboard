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

case class Scorecard(repo: Repo, score: Double, checks: Seq[Check])
object Scorecard:
  given JsonReader[Scorecard] = jsonFormat3(Scorecard.apply)

object Scorecards extends Cached[String](identity):
  override def fetchUncached(repo: String): Array[Byte] =
    var p = Promise[java.io.InputStream]
    //var e = Promise[java.io.InputStream]
    var seq = Seq("scorecard", s"--repo=$repo", "--format=json", "--show-details")
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
    //val re = Await.result(e.future, 60.seconds)
    if process.exitValue() != 0 then
        throw new IllegalStateException(s"Failed to get scorecard for $repo")
    //val errors = IOUtils.toByteArray(re)
    //println("Errors: " + new String(errors))
    val result = IOUtils.toByteArray(ro)
    //println("Output: " + new String(result))
    result
