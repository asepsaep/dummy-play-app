package utils

import org.apache.spark.{ SparkConf, SparkContext }

object SparkUtil {

  val sparkConf = new SparkConf().setMaster("local[*]").setAppName("dev-play-app")
  val sparkContext = SparkContext.getOrCreate(sparkConf)

}