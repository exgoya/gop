package service;

import java.sql.*;

import com.google.gson.GsonBuilder;

import model.Common;
import model.Config;
import model.Data;
import model.ResultCommon;
import sunje.goldilocks.jdbc.GoldilocksDataSource;

public class Db {
	Config sConfig;

	public Db(Config config) {
		sConfig = config;
	}

	public PreparedStatement[] createPstmt(Connection con) throws SQLException {

		PreparedStatement[] arrPstmt = new PreparedStatement[sConfig.common.length];
		for (int i = 0; i < sConfig.common.length; i++) {
			if (!sConfig.common[i].isOs) {
				arrPstmt[i] = con.prepareStatement(sConfig.common[i].sql);
			}
		}
		return arrPstmt;

	}

	public Data getCommonQuery(PreparedStatement[] arrPstmt) throws SQLException {

		ResultCommon[] resultArr = new ResultCommon[sConfig.common.length];

		String sysTimestamp = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss.SSS").create()
				.toJson(new Timestamp(System.currentTimeMillis()));

		for (int i = 0; i < sConfig.common.length; i++) {

			// Statement stmt = con.createStatement();
			boolean alert = false;
			if (sConfig.common[i].isOs) {
				int queryValue = 0;

				ReadOs oc = new ReadOs();
				queryValue = oc.execute(sConfig.common[i].sql);

				alert = alertCheck(sConfig.common[i], queryValue);

				resultArr[i] = new ResultCommon(sConfig.common[i].name, queryValue, sConfig.common[i].tag, alert);

			} else {
				ResultSet rs = arrPstmt[i].executeQuery();
				// ResultSet rs = stmt.executeQuery(sConfig.common[i].sql);
				ResultSetMetaData rsMeta = rs.getMetaData();
				if (rsMeta.getColumnCount() != 1) {
					System.out.println("not support multiple column query _ Query name : " + sConfig.common[i].name);
				}

				int queryValue = 0;
				while (rs.next()) {
					queryValue = rs.getInt(1);
					alert = alertCheck(sConfig.common[i], queryValue);

					resultArr[i] = new ResultCommon(sConfig.common[i].name, queryValue, sConfig.common[i].tag, alert);
				}
				rs.close();
			}
			
			//alert action script
			if (alert) {
				//run action script
				
			}
			
			
		}
		return new Data(sysTimestamp, resultArr);
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
//        String sClass = "sunje.goldilocks.jdbc.GoldilocksDriver";
//        String sUrl = "jdbc:goldilocks://" + sConfig.host.ip + ":" + sConfig.host.port + "/test";
		GoldilocksDataSource sDataSource = new GoldilocksDataSource();
		sDataSource.setDatabaseName("gop");
		sDataSource.setServerName(sConfig.host.ip);
		sDataSource.setPortNumber(sConfig.host.port);
		sDataSource.setUser(sConfig.host.user);
		sDataSource.setPassword(sConfig.host.password);
		sDataSource.setStatementPoolOn(true);
		sDataSource.setStatementPoolSize(20);

		return sDataSource.getConnection();
	}

}