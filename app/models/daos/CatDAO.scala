package models.daos

import scala.concurrent.Future

import javax.inject.Inject
import models.Cat
import models.CatTypes._
import play.api.db.slick.DatabaseConfigProvider
import play.api.db.slick.HasDatabaseConfigProvider
import play.api.libs.concurrent.Execution.Implicits._
import slick.driver.JdbcProfile

class CatDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends HasDatabaseConfigProvider[JdbcProfile] {

  import modules.MyPostgresDriver.api._

  private val Cats = TableQuery[CatsTable]

  def all(): Future[Seq[Cat]] = db.run(Cats.result)

  def insert(): Future[Unit] = db.run {
    Cats ++= Seq(Cat(10, List("Bacok", "Clurit", "Silet"), Garong), Cat(1, List("Kucing", "Meow", "Empus"), Anggora))
  }.map(_ => ())

  private class CatsTable(tag: Tag) extends Table[Cat](tag, "cat") {

    def id = column[Long]("id", O.AutoInc)
    def name = column[List[String]]("name")
    def catType = column[CatType]("types")

    def * = (id, name, catType) <> (Cat.tupled, Cat.unapply _)
  }

}