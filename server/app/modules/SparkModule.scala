package modules

import com.google.inject.AbstractModule
import org.apache.spark.SparkContext
import utils.SparkUtil

class SparkModule extends AbstractModule {

  override def configure() = {

    bind(classOf[SparkContext]).toInstance(SparkUtil.sparkContext)

  }

}
