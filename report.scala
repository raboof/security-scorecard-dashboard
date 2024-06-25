package report

import scorecard._
import scalatags.Text.all._

def renderScore(score: Double, yellowThreshold: Int = 5, greenThreshold: Int = 7) =
  val color =
    if score < 0 then "white"
    else if score < yellowThreshold then "red"
    else if score < greenThreshold then "yellow"
    else "green"
  td(style:=s"background: $color")(score)

def renderCard(card: Scorecard) =
  tr(
    td(card.repo.name),
    renderScore(card.score),
    Scorecard.checks.map(c => {
      val score = card.checks.get.find(_.name == c).get.score
      c match
        case "Branch-Protection" => renderScore(score, 3, 8)
        case _ => renderScore(score)
    })
  )

def write(cards: Seq[Scorecard], to: String) =
  val report = html(
    body(
      table(
        tr(
          th("Repo"),
          th("Overall score".replaceAll(" ", 0x00a0.toChar.toString)),
          Scorecard.checks.map(s => th(s.replaceAll("-", 8209.toChar.toString)))
        ),
        cards.map(renderCard)
      )
    )
  )

  import java.io._
  val pw = new PrintWriter(new File(to))
  pw.write("<!DOCTYPE html>")
  pw.write(report.render)
  pw.close
