package models.daos

import java.util.UUID
import javax.inject.Inject

import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import modules.MyPostgresDriver.api._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._
import models._
import play.api.Logger

trait MilestoneDAO {
  def find(ticketId: Long): Future[Seq[Milestone]]
  def findById(id: Long): Future[Option[Milestone]]
  def findFrom(reporter: String): Future[Seq[Milestone]]
  def all: Future[Seq[Milestone]]
  def all(page: Int, pageSize: Int): Future[Seq[Milestone]]
  def save(milestone: Milestone): Future[Milestone]
  def count: Future[Int]
  def delete(id: Long): Future[Unit]
}

class MilestoneDAOImpl @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends MilestoneDAO with HasDatabaseConfigProvider[JdbcProfile] {

  private val Milestones = TableQuery[MilestoneTable]

  override def find(ticketId: Long): Future[Seq[Milestone]] = {
    db.run(Milestones.filter(_.ticketId === ticketId).sortBy(_.datetime.desc).result)
  }

  override def findFrom(reporter: String): Future[Seq[Milestone]] = {
    db.run(Milestones.filter(_.milestoneReporter === reporter).sortBy(_.datetime.desc).result)
  }

  override def findById(id: Long): Future[Option[Milestone]] = {
    db.run(Milestones.filter(_.id === id).result.headOption)
  }

  override def count: Future[Int] = {
    db.run(Milestones.length.result)
  }

  override def all: Future[Seq[Milestone]] = {
    db.run(Milestones.sortBy(_.ticketId).result)
  }

  override def all(page: Int, pageSize: Int): Future[Seq[Milestone]] = {
    db.run(Milestones.sortBy(_.ticketId).drop(page * pageSize).take(pageSize).result)
  }

  override def delete(id: Long): Future[Unit] = {
    db.run(Milestones.filter(_.id === id).delete).map(_ ⇒ {})
  }

  override def save(milestone: Milestone): Future[Milestone] = {
    milestone.id match {
      case None ⇒ db.run(
        for {
          i ← Milestones.returning(Milestones.map(_.id)).into((item, id) ⇒ item.copy(id = Some(id))) += milestone
        } yield i
      )
      case Some(id) ⇒ findById(id).flatMap {
        case None ⇒ db.run(
          for {
            i ← Milestones.returning(Milestones.map(_.id)).into((item, id) ⇒ item.copy(id = Some(id))) += milestone
          } yield i
        )
        case Some(_) ⇒ db.run(
          for {
            u ← Milestones.filter(_.id === id).update(milestone)
          } yield milestone
        )
      }
    }
  }

}
