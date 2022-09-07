package service;
import java.sql.*;

import com.google.gson.GsonBuilder;

import model.Common;
import model.Config;
import model.ResultCommon;

public class Db {
	Config sConfig;
	
	public Db(Config config) {
		sConfig = config;
	}
	
	public ResultCommon[] getCommonQuery(Connection con) throws SQLException {
		
		ResultCommon[] resultArr = new ResultCommon[sConfig.common.length];
		
		Statement stmt = con.createStatement();
		for( int i = 0 ; i < sConfig.common.length; i ++ ) {
			
			ResultSet rs = stmt.executeQuery(sConfig.common[i].sql);
			ResultSetMetaData rsMeta = rs.getMetaData();
			if(rsMeta.getColumnCount()!=1) {
				System.out.println("not support multiple column query _ Query name : " + sConfig.common[i].name);
			}
		
			boolean alert = false;
			int queryValue = 0;
			while (rs.next()) {
			    String sysTimestamp = new GsonBuilder()
	               .setDateFormat("yyyy-MM-dd hh:mm:ss")
	               .create()
	               .toJson(new Timestamp(System.currentTimeMillis()));
			    queryValue = rs.getInt(1);
			    alert = alertCheck(sConfig.common[i],queryValue);
			    
				resultArr[i] = new ResultCommon(sConfig.common[i].name,queryValue,sConfig.common[i].tag,sysTimestamp,alert);
			}
			
		}
		return resultArr;
	}
	
	private boolean alertCheck(Common common, int queryValue) {
		switch (common.alertPolicy) {
		case 1:
			if (common.alertValue < queryValue) {
				return true;
			}
			break;
		case 2:
			if (common.alertValue > queryValue) {
				return true;
			}
			break;
		case 3:
			if (common.alertValue == queryValue) {
				return true;
			}
			break;
		default:
			return false;
		}
		
		return false;
	}

	public Connection createConnection() throws SQLException {
        String sClass = "sunje.goldilocks.jdbc.GoldilocksDriver";
        String sUrl = "jdbc:goldilocks://" + sConfig.host.ip + ":" + sConfig.host.port + "/test";
            try {
				Class.forName(sClass);
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
        return DriverManager.getConnection(sUrl, sConfig.host.user, sConfig.host.password);
	}
}