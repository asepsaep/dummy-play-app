package models.daos

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.OpenIDInfo
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO

import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits._

class OpenIDInfoDAO extends DelegableAuthInfoDAO[OpenIDInfo] {

  override def find(loginInfo: LoginInfo): Future[Option[OpenIDInfo]] = ???

  override def update(loginInfo: LoginInfo, authInfo: OpenIDInfo): Future[OpenIDInfo] = ???

  override def remove(loginInfo: LoginInfo): Future[Unit] = ???

  override def save(loginInfo: LoginInfo, authInfo: OpenIDInfo): Future[OpenIDInfo] = ???

  override def add(loginInfo: LoginInfo, authInfo: OpenIDInfo): Future[OpenIDInfo] = ???

}
