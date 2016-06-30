package models.daos

import javax.inject.Inject

import models.{TokenInfo, TokenInfoTable}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import modules.MyPostgresDriver.api._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

trait TokenInfoDAO {
  def save(tokenInfo: TokenInfo): Future[TokenInfo]
  def find(token: String): Future[Option[TokenInfo]]
  def findByEmail(email: String): Future[Seq[TokenInfo]]
  def delete(token: String): Future[Unit]
}

class TokenInfoDAOImpl @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends TokenInfoDAO with HasDatabaseConfigProvider[JdbcProfile] {

  private val TokenInfos = TableQuery[TokenInfoTable]

  override def save(tokenInfo: TokenInfo): Future[TokenInfo] = {
    db.run(
      for {
        t ← (TokenInfos += tokenInfo)
      } yield tokenInfo
    )
  }

  override def find(token: String): Future[Option[TokenInfo]] = {
    db.run(TokenInfos.filter(_.token === token).result.headOption)
  }

  override def findByEmail(email: String): Future[Seq[TokenInfo]] = {
    db.run(TokenInfos.filter(_.email === email).result)
  }

  override def delete(token: String): Future[Unit] = {
    db.run(TokenInfos.filter(_.token === token).delete).map(_ ⇒ {})
  }

}
