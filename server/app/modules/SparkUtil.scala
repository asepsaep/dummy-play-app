package modules

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SparkSession

object SparkUtil {

  val classes: Seq[Class[_]] = Seq(
    getClass, // To get the jar with our own code.
    classOf[org.postgresql.Driver] // To get the connector.
  )

  val jars = classes.map(_.getProtectionDomain.getCodeSource.getLocation.getPath)

  val sparkConf = new SparkConf()
    .setMaster("local[*]")
    .setAppName("dev-play-app")
    .set("spark.logConf", "true")
    .set("spark.driver.host", "localhost")
    .set("spark.driver.port", "8080")

  val sparkContext = SparkContext.getOrCreate(sparkConf)

  val sparkSession = SparkSession.builder.getOrCreate()

}