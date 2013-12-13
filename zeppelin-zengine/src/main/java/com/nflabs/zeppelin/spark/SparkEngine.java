package com.nflabs.zeppelin.spark;

import java.util.List;

import org.apache.spark.SparkContext;
import org.apache.spark.scheduler.SparkListener;
import org.apache.spark.scheduler.SparkListenerJobEnd;
import org.apache.spark.scheduler.SparkListenerJobStart;
import org.apache.spark.scheduler.SparkListenerStageSubmitted;
import org.apache.spark.scheduler.SparkListenerTaskEnd;
import org.apache.spark.scheduler.SparkListenerTaskStart;
import org.apache.spark.scheduler.StageCompleted;

import scala.collection.mutable.Buffer;
import scala.tools.nsc.Settings;
import scala.tools.nsc.interpreter.IMain;

import com.nflabs.zeppelin.conf.ZeppelinConfiguration;
import com.nflabs.zeppelin.conf.ZeppelinConfiguration.ConfVars;

public class SparkEngine implements SparkListener {

	private ZeppelinConfiguration conf;
	private SparkContext sparkContext;
	private IMain interpreter;

	public SparkEngine(ZeppelinConfiguration conf, String name, List<String> jars){
		this.conf = conf;
		init(name, jars);
	}
	
	public void init(String name, List<String> jars){
		String master = conf.getString(ConfVars.SPARK_MASTER);
		String sparkHome = conf.getString(ConfVars.SPARK_HOME);
		Buffer<String> jarSeq = null;
		if(jars!=null){
			scala.collection.JavaConversions.asScalaBuffer(jars);
		}
		SparkContext sc = new SparkContext(master, name, sparkHome, jarSeq, scala.collection.JavaConversions.mapAsScalaMap(System.getenv()), null);
		sc.addSparkListener(this);
		this.sparkContext = sc;
		
		Settings settings = new Settings();
		settings.usejavacp().tryToSetFromPropertyValue("true");
		//settings.stopAfter().tryToSetColon(Nil.$colon$colon("dce"));		
		interpreter = new IMain(settings);
		
	}
	
	public void bind(String name, Object o){
		interpreter.bindValue(name, o);
	}
	
	public void interpret(String line){
		interpreter.interpret(line);
	}
	
	public void stop(){
		sparkContext.stop();
		interpreter.close();
	}

	public SparkContext sc(){
		return sparkContext;
	}
	
	@Override
	public void onJobEnd(SparkListenerJobEnd arg0) {
		
	}

	@Override
	public void onJobStart(SparkListenerJobStart arg0) {
		
	}

	@Override
	public void onStageCompleted(StageCompleted arg0) {
		
	}

	@Override
	public void onStageSubmitted(SparkListenerStageSubmitted arg0) {
		
	}

	@Override
	public void onTaskEnd(SparkListenerTaskEnd arg0) {
		
	}

	@Override
	public void onTaskStart(SparkListenerTaskStart arg0) {
		
	}
	
}
