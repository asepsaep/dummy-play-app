package models.daos

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OAuth2Info
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import models.OAuth2InfoTable
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import modules.MyPostgresDriver.api._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

class OAuth2InfoDAO @Inject() (accountDAO: AccountDAO, protected val dbConfigProvider: DatabaseConfigProvider) extends DelegableAuthInfoDAO[OAuth2Info] with HasDatabaseConfigProvider[JdbcProfile] {

  import models.DBOAuth2Info._

  private val OAuth2Infos = TableQuery[OAuth2InfoTable]

  override def find(loginInfo: LoginInfo): Future[Option[OAuth2Info]] = {
    accountDAO.findByLoginInfo(loginInfo).flatMap {
      case None ⇒ Future.successful(None)
      case Some(account) ⇒ db.run(OAuth2Infos.filter(_.accountUsername === account.username).result.headOption).flatMap {
        case None             ⇒ Future.successful(None)
        case Some(oAuth1Info) ⇒ Future.successful(Some(db2OAuth2Info(oAuth1Info)))
      }
    }
  }

  override def update(loginInfo: LoginInfo, authInfo: OAuth2Info): Future[OAuth2Info] = {
    accountDAO.findByLoginInfo(loginInfo).flatMap {
      case None ⇒ throw new IllegalStateException("There must exist a user before password info can be added")
      case Some(account) ⇒ db.run(OAuth2Infos.filter(_.accountUsername === account.username).update(oAuth2Info2db(account.username, authInfo))).flatMap {
        _ ⇒ Future.successful(authInfo)
      }
    }
  }

  override def remove(loginInfo: LoginInfo): Future[Unit] = {
    accountDAO.findByLoginInfo(loginInfo).flatMap {
      case None ⇒ throw new IllegalStateException("There must exist a user before password info can be added")
      case Some(account) ⇒ db.run(OAuth2Infos.filter(_.accountUsername === account.username).delete).flatMap {
        _ ⇒ Future.successful({})
      }
    }
  }

  override def save(loginInfo: LoginInfo, authInfo: OAuth2Info): Future[OAuth2Info] = {
    accountDAO.findByLoginInfo(loginInfo).flatMap {
      case None ⇒ throw new IllegalStateException("There must exist a user before password info can be added")
      case Some(account) ⇒ db.run(OAuth2Infos.filter(_.accountUsername === account.username).result.headOption).flatMap {
        case Some(oAuth2Info) ⇒ update(loginInfo, authInfo)
        case None             ⇒ add(loginInfo, authInfo)
      }
    }
  }

  override def add(loginInfo: LoginInfo, authInfo: OAuth2Info): Future[OAuth2Info] = {
    accountDAO.findByLoginInfo(loginInfo).flatMap {
      case None ⇒ throw new IllegalStateException("There must exist a user before password info can be added")
      case Some(account) ⇒ db.run(OAuth2Infos += oAuth2Info2db(account.username, authInfo)).flatMap {
        _ ⇒ Future.successful(authInfo)
      }
    }
  }

}
