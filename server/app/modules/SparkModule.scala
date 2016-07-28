package modules

import com.google.inject.{AbstractModule, Inject}
import net.codingwell.scalaguice.ScalaModule
import org.apache.spark.SparkContext
import org.apache.spark.sql.SparkSession

class SparkModule extends AbstractModule with ScalaModule {

  override def configure() = {
    bind[SparkContext].toInstance(SparkUtil.sparkContext)
    bind[SparkSession].toInstance(SparkUtil.sparkSession)
  }

}
