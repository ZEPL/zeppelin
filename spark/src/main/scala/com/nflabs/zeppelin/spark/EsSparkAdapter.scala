package com.nflabs.zeppelin.spark

import org.apache.spark.serializer.KryoSerializer
import org.apache.spark.sql.{SQLContext, SchemaRDD}
import org.apache.spark.{SparkConf, SparkContext}
import org.elasticsearch.hadoop.cfg.ConfigurationOptions._
import org.elasticsearch.spark.sql.EsSparkSQL

object EsSparkAdapter {
  def esRDD(node: String, resource: String): SchemaRDD = {
    esRDD(Map("spark.serializer" -> classOf[KryoSerializer].getName,
      ES_NODES -> node,
      ES_RESOURCE -> resource))
  }

  def esRDD(node: String, resource: String, query: String): SchemaRDD = {
    esRDD(Map("spark.serializer" -> classOf[KryoSerializer].getName,
      ES_NODES -> node,
      ES_RESOURCE -> resource,
      ES_QUERY -> query))
  }

  private def esRDD(params: Map[String, String]): SchemaRDD = {
    EsSparkSQL.esRDD(sqlContext(params))
  }

  private def sqlContext(params: Map[String, String]): SQLContext = {
    val sconf = new SparkConf().setAll(params).setAppName("Test").setMaster("local")
    new SQLContext(new SparkContext(sconf))
  }
}
