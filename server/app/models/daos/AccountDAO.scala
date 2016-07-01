package models.daos

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import models._
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import play.api.Logger
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import modules.MyPostgresDriver.api._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

trait AccountDAO {
  def findByEmail(email: String): Future[Option[Account]]
  def save(account: Account): Future[Account]
  def count: Future[Int]
  def find(username: String): Future[Option[Account]]
  def findByLoginInfoAlongWithEmail(username: String, email: String): Future[Option[Account]]
  def findByLoginInfo(loginInfo: LoginInfo): Future[Option[Account]]
  def all: Future[Seq[Account]]
  def all(page: Int, pageSize: Int): Future[Seq[Account]]
  def delete(username: String): Future[Unit]
  def updateUsername(oldUsername: String, updatedAccount: Account): Future[Account]
  def findCredentialsAccount(email: String): Future[Option[Account]]
}

class AccountDAOImpl @Inject() (protected val dbConfigProvider: DatabaseConfigProvider) extends AccountDAO with HasDatabaseConfigProvider[JdbcProfile] {

  private val Accounts = TableQuery[AccountTable]
  private val PasswordInfos = TableQuery[PasswordInfoTable]
  private val OAuth1Infos = TableQuery[OAuth1InfoTable]
  private val OAuth2Infos = TableQuery[OAuth2InfoTable]

  private def findBy(criterion: (AccountTable ⇒ slick.lifted.Rep[Boolean])) = db.run(
    for {
      account ← Accounts.filter(criterion).result.headOption
    } yield account
  ).recover {
      case e ⇒ Logger.error(e.getMessage, e); None
    }

  override def findByEmail(email: String): Future[Option[Account]] = {
    findBy(_.email.toLowerCase === email.toLowerCase)
  }

  override def find(username: String): Future[Option[Account]] = {
    findBy(_.username.toLowerCase === username.toLowerCase)
  }

  override def findCredentialsAccount(email: String): Future[Option[Account]] = {
    findBy(u ⇒ u.providerId === CredentialsProvider.ID && u.email === email)
  }

  override def findByLoginInfoAlongWithEmail(username: String, email: String) = {
    findBy(u ⇒ u.username.toLowerCase === username.toLowerCase || u.email.toLowerCase === email)
  }

  override def findByLoginInfo(loginInfo: LoginInfo): Future[Option[Account]] = {
    findBy(account ⇒ account.providerId === loginInfo.providerID && account.providerKey.toLowerCase === loginInfo.providerKey.toLowerCase)
  }

  override def count: Future[Int] = {
    db.run(Accounts.length.result)
  }

  override def all: Future[Seq[Account]] = {
    db.run(Accounts.sortBy(account ⇒ account.name.asc).result)
  }

  override def all(page: Int, pageSize: Int): Future[Seq[Account]] = {
    db.run(Accounts.sortBy(account ⇒ account.name.asc).drop(page * pageSize).take(pageSize).result)
  }

  override def delete(username: String): Future[Unit] = {
    val deletion = for {
      oAuth2InfoDeletion ← OAuth2Infos.filter(_.accountUsername.toLowerCase === username.toLowerCase).delete
      oAuth1InfoDeletion ← OAuth1Infos.filter(_.accountUsername.toLowerCase === username.toLowerCase).delete
      passwordInfoDeletion ← PasswordInfos.filter(_.accountUsername.toLowerCase === username.toLowerCase).delete
      accountDeletion ← Accounts.filter(_.username.toLowerCase === username.toLowerCase).delete
    } yield (oAuth2InfoDeletion, oAuth1InfoDeletion, passwordInfoDeletion, accountDeletion)
    db.run(deletion).map(_ ⇒ {})
  }

  override def save(account: Account): Future[Account] = {
    val existingUserFuture = find(account.username)
    existingUserFuture.flatMap {
      case None ⇒ db.run(
        for {
          i ← (Accounts += account)
        } yield account
      )
      case Some(_) ⇒ db.run(
        for {
          u ← Accounts.filter(_.username === account.username).update(account)
        } yield account
      )
    }
  }

  override def updateUsername(oldUsername: String, updatedAccount: Account): Future[Account] = {
    db.run(
      for {
        u ← Accounts.filter(_.username === oldUsername).update(updatedAccount)
      } yield updatedAccount
    )
  }

}