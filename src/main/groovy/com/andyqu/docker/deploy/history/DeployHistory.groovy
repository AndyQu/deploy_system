package com.andyqu.docker.deploy.history

class DeployHistory {
	int startTimeStamp;
	String startTime;
	int endTimeStamp;
	String endTime;
	
	String containerName;
	String containerId;
	String hostName;
	String hostIp;
	
	def deployConfig;
}
