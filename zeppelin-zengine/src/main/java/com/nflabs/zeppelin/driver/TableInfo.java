package com.nflabs.zeppelin.driver;

public class TableInfo {
	String database;
	String owner;
	String location;
	boolean exists;
	
	public TableInfo(){
	}

	public String getDatabase() {
		return database;
	}

	public void setDatabase(String database) {
		this.database = database;
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public boolean isExists() {
		return exists;
	}

	public void setExists(boolean exists) {
		this.exists = exists;
	}
	
	
}
