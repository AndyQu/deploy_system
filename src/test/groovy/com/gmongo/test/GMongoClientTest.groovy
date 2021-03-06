package com.gmongo.test

import com.andyqu.docker.deploy.Tool;
import com.andyqu.docker.deploy.history.DeployHistory
import com.gmongo.GMongoClient
import com.mongodb.DB
import com.mongodb.DBCursor
import com.mongodb.DBObject;
import com.mongodb.ServerAddress
import com.mongodb.MongoCredential
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

import org.testng.annotations.BeforeTest
import org.testng.annotations.Test
import org.testng.annotations.AfterTest
import org.apache.commons.lang3.builder.ReflectionToStringBuilder
import org.apache.commons.lang3.builder.ToStringStyle
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.apache.commons.beanutils.BeanUtils

class GMongoClientTest {
	def static final Logger logger = LoggerFactory.getLogger(GMongoClientTest)
	def static final JsonSlurper slurper=new JsonSlurper()
	
	//使用美团办公云的mongodb。如果不能直接连，则先用anyconnect连vpn
	def username="unit_testor"
	def password="123456"
	def database="unit_test"
	def port=27017
	
	def credentials
	def GMongoClient client
	def DB db
	
	@BeforeTest
	def void setup(){
		new Tool().extendObject()
		//notice:这里的"admin"指的是用于获取用户数据的database，不是用户拥有权限的database
		//	def host="10.4.242.144" //meituan
//		credentials = MongoCredential.createMongoCRCredential(username,  "admin",  password as char[])
//		client = new GMongoClient(new ServerAddress( host,  port), [credentials])

		def host="127.0.0.1"
		credentials = MongoCredential.createScramSha1Credential(username, database, password as char[])
		client = new GMongoClient(new ServerAddress( "${host}:${port}"), [credentials])
//		client = new GMongoClient(new ServerAddress( host,  port), [credentials])

		db=client.getDB(database)
		logger.info("event_name=setup_mongodb_connection client={}", ReflectionToStringBuilder.toString(client, ToStringStyle.MULTI_LINE_STYLE))
		
		prepareData()
	}
	
	def void prepareData(){
		assert db.webhivesql instanceof com.mongodb.DBCollection
		assert db['webhivesql'] instanceof com.mongodb.DBCollection
//		def a= BeanUtils.describe(["web","hehe"])
		def time=System.currentTimeSeconds()
		[
			new DeployHistory(
				startTimeStamp:time,
				endTimeStamp:time+100,
				projectNames:["srqserver","webhivesql"],
				containerName:"annoy-master-webhivesql",
				containerId:"e12eba6ee03b",
				hostName:"andyqu-dev",
				hostIp:"172.26.19.70"
			),
			new DeployHistory(
				startTimeStamp:time+2000,
				endTimeStamp:time+3000,
				projectNames:["webhivesql","crm"],
				containerName:"andy-dev-webhivesql",
				containerId:"e12eba6ee03b",
				hostName:"andyqu-dev",
				hostIp:"172.26.19.70"
			),
		].each{
			def m = it.toMap()
			logger.info("event_name=insert item={}", m)
			db.webhivesql << m
		}
	}
	
	@Test
	def void dog(){
		db['webhivesql'].find().each {
			logger.info("event_name=show_record record={}",new JsonBuilder(it).toPrettyString())
		}
//		logger.info("event_name=find_andy-dev-webhivesql obj={}", db.webhivesql.findOne(containerName:"andy-dev-webhivesql"))
		logger.info("升序排列")
		def condition=[
				projectNames: 
					[
						$elemMatch:[ $eq: "webhivesql" ]
					]
			] 
		db.webhivesql.find( 
			condition
		).sort(startTimeStamp:1).each { 
			logger.info("event_name=show_record record={}",new JsonBuilder(it).toPrettyString())
		}
		logger.info("降序排列")
		db.webhivesql.find(condition ).sort(startTimeStamp:-1).each {
			assert it instanceof DBObject
			logger.info("event_name=show_record record={}",new JsonBuilder(it).toPrettyString())
		}
		logger.info("找出最新的部署")
		DBCursor cursor = db.webhivesql.find(condition ).sort(startTimeStamp:-1)
		if(cursor.hasNext()){
			logger.info "event_name=show_latest_deployment record={}",cursor.next()
		}else{
			logger.info("event_name=没有任何部署")
		}
	}
	
	@Test
	def void cat(){
		DBCursor cursor = db.webhivesql.find( ).sort(startTimeStamp:-1)
		if(cursor.hasNext()){
			DBObject item=cursor.next()
			logger.info "event_name=show_DBObject_type class={} toString={}",item.getClass(), item.toString()
			logger.info "event_name=show_latest_deployment record={}", item
			def Map jresult = slurper.	parseText( new JsonBuilder(item).toString())
//			DeployHistory history=jresult as DeployHistory
			jresult.remove("_id")
			DeployHistory history = new DeployHistory(jresult)
			println(history.toString())
		}else{
			logger.info("event_name=没有任何部署")
		}
	}
	
	@AfterTest
	def void cleanup(){
		db.webhivesql.find().each {
			db.webhivesql.remove(it)
		}
	}
}
