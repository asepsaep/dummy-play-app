package modules

import com.github.tminglei.slickpg._
import models.CatTypes
import slick.driver.JdbcProfile
import slick.profile.Capability

trait MyPostgresDriver extends ExPostgresDriver
  with PgArraySupport
  with PgEnumSupport
  with PgDate2Support
  with PgHStoreSupport {

  def pgjson = "jsonb"

  override protected def computeCapabilities: Set[Capability] = {
    super.computeCapabilities + JdbcProfile.capabilities.insertOrUpdate
  }

  override val api = MyAPI

  object MyAPI extends API
    with ArrayImplicits
    with DateTimeImplicits
    with HStoreImplicits
    with EnumImplicits {

    implicit val strListTypeMapper = new SimpleArrayJdbcType[String]("text").to(_.toList)

  }

  trait EnumImplicits {
    implicit val catTypeMapper = createEnumJdbcType("cat_type", CatTypes)
    implicit val catListTypeMapper = createEnumListJdbcType("cat_type", CatTypes)
    implicit val catColumnExtensionMethodsBuilder = createEnumColumnExtensionMethodsBuilder(CatTypes)
    implicit val catOptionColumnExtensionMethodsBuilder = createEnumOptionColumnExtensionMethodsBuilder(CatTypes)
  }

}

object MyPostgresDriver extends MyPostgresDriver