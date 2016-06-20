package models.daos

import javax.inject.Inject

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import models.PasswordInfoTable
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.driver.JdbcProfile
import slick.lifted.TableQuery
import modules.MyPostgresDriver.api._

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

class PasswordInfoDAO @Inject() (accountDAO: AccountDAO, protected val dbConfigProvider: DatabaseConfigProvider) extends DelegableAuthInfoDAO[PasswordInfo] with HasDatabaseConfigProvider[JdbcProfile] {

  import models.DBPasswordInfo._

  private val PasswordInfos = TableQuery[PasswordInfoTable]

  override def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] = {
    accountDAO.findByLoginInfo(loginInfo).flatMap {
      case None => Future.successful(None)
      case Some(account) => db.run(PasswordInfos.filter(_.accountUsername === account.username).result.headOption).flatMap {
        case None               => Future.successful(None)
        case Some(passwordInfo) => Future.successful(Some(db2PasswordInfo(passwordInfo)))
      }
    }
  }

  override def update(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    accountDAO.findByLoginInfo(loginInfo).flatMap {
      case None => throw new IllegalStateException("There must exist a user before password info can be added")
      case Some(account) => db.run(PasswordInfos.filter(_.accountUsername === account.username).update(passwordInfo2db(account.username, authInfo))).flatMap {
        _ => Future.successful(authInfo)
      }
    }
  }

  override def remove(loginInfo: LoginInfo): Future[Unit] = {
    accountDAO.findByLoginInfo(loginInfo).flatMap {
      case None => throw new IllegalStateException("There must exist a user before password info can be added")
      case Some(account) => db.run(PasswordInfos.filter(_.accountUsername === account.username).delete).flatMap {
        _ => Future.successful({})
      }
    }
  }

  override def save(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    accountDAO.findByLoginInfo(loginInfo).flatMap {
      case None => throw new IllegalStateException("There must exist a user before password info can be added")
      case Some(account) => db.run(PasswordInfos.filter(_.accountUsername === account.username).result.headOption).flatMap {
        case Some(passwordInfo) => update(loginInfo, authInfo)
        case None               => add(loginInfo, authInfo)
      }
    }
  }

  override def add(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    accountDAO.findByLoginInfo(loginInfo).flatMap {
      case None => throw new IllegalStateException("There must exist a user before password info can be added")
      case Some(account) => db.run(PasswordInfos += passwordInfo2db(account.username, authInfo)).flatMap {
        _ => Future.successful(authInfo)
      }
    }
  }

}
