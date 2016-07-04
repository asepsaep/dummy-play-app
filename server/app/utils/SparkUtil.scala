package utils

import org.apache.spark.{ SparkConf, SparkContext }

object SparkUtil {

  val sparkContext = new SparkContext(new SparkConf().setMaster("local[*]").setAppName("dev-play-app"))

}
