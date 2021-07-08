import slick.jdbc.H2Profile.api._

import scala.concurrent.Await
import scala.concurrent.duration._

import scala.concurrent.ExecutionContext.Implicits.global
import slick.lifted.AbstractTable
import slick.dbio

object Example {

  final case class Message(sender: String, content: Option[String], id: Long = 0L)

  def freshTestData = Seq(
    Message("Dave", Some("Hello, HAL. Do you read me, HAL?")),
    Message("HAL", Some("Affirmative, Dave. I read you.")),
    Message("Dave", Some("Open the pod bay doors, HAL.")),
    Message("HAL", Some("I'm sorry, Dave. I'm afraid I can't do that.")),
    Message("Dave", None)
  )

  final class MessageTable(tag: Tag) extends Table[Message](tag, "message") {

    def id      = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def sender  = column[String]("sender")
    def content = column[Option[String]]("content")

    def * = (sender, content, id).mapTo[Message]
  }

  lazy val messages = TableQuery[MessageTable]

  val q = messages

  object QueryLabelling {
    // TODO: sanitise the label text to avoid breaking out of the comment?
    private def formatComment(label: String): String =
      s"/* $label */"

    private def labelResult[E <: AbstractTable[_]](
      q:     TableQuery[E],
      label: String
    ): DBIOAction[Seq[E#TableElementType], NoStream, dbio.Effect.Read] = {
      val sql         = q.result.statements.mkString
      val labelledSql = sql + formatComment(label)
      println(labelledSql)
      q.result.overrideStatements(Seq(labelledSql))
    }

    implicit class QueryLabellingOps[E <: AbstractTable[_]](q: TableQuery[E]) {

      /** Never pass end-user supplied text in the label: it's a SQL injection route */
      def labelledResult(nonUserSuppliedText: String) = QueryLabelling.labelResult(q, nonUserSuppliedText)
    }
  }

  import QueryLabelling._

  def main(args: Array[String]): Unit = {
    val program = for {
      _       <- messages.schema.create
      _       <- messages ++= freshTestData
      results <- q.labelledResult("This is query 1")
    } yield results

    val db = Database.forConfig("example")
    try Await.result(db.run(program), 2.seconds).foreach(println)
    finally db.close
  }
}
