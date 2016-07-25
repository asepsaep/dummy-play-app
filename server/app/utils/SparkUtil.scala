package utils

import org.apache.spark.{ SparkConf, SparkContext }

object SparkUtil {

  val sparkConf = new SparkConf().setMaster("local[*]").setAppName("dev-play-app").set("spark.driver.memory", "4g")
  val sparkContext = SparkContext.getOrCreate(sparkConf)

}