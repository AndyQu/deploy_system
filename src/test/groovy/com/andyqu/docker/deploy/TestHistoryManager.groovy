package com.andyqu.docker.deploy
import org.testng.annotations.BeforeTest
import org.testng.annotations.Test

import com.andyqu.docker.deploy.history.DeployHistory
import com.andyqu.docker.deploy.history.HistoryManager
import com.mongodb.DBCollection
import groovy.json.JsonSlurper

import org.slf4j.Logger;
import org.testng.annotations.AfterTest

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TestHistoryManager {
	def static final Logger logger = LoggerFactory.getLogger(TestHistoryManager)
	
	HistoryManager manager
	Tool tool = new Tool()
	
	@BeforeTest
	def void setup(){
		manager = new HistoryManager()
		manager.setMongoConfig("/mongodb.json")
		
		int time = System.currentTimeSeconds().intValue()
		[
			new DeployHistory(
				startTimeStamp:time,
				endTimeStamp:time+100,
				projectName:"webhivesql",
				containerName:"annoy-master-webhivesql",
				containerId:"e12eba6ee03b",
				hostName:"andyqu-dev",
				hostIp:"172.26.19.70"
			),
			new DeployHistory(
				startTimeStamp:time+2000,
				endTimeStamp:time+3000,
				projectName:"crm",
				containerName:"andy-dev-webhivesql",
				containerId:"e12eba6ee03b",
				hostName:"andyqu-dev",
				hostIp:"172.26.19.70"
			),
		].each {
			manager.save(it)
		}
	}
	
	@Test
	def void cat(){
		manager.fetchHistories("crm")
	}
	
	@Test
	def void dog(){
		manager.fetchHistories("crm",[containerId:"e12eba6ee03b"])
	}
	
	@AfterTest
	def void cleanup(){
		[
			'webhivesql',
			'crm'
		].each {
		name->
			manager.db[name].find().each {
				manager.db[name].remove it
				logger.info "removed_record={}", it 
			}
		}
	}
}
