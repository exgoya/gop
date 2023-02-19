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

	public Connection createConnection() {
		// String sClass = "sunje.goldilocks.jdbc.GoldilocksDriver";
		// String sUrl = "jdbc:goldilocks://" + sConfig.host.ip + ":" +
		// sConfig.host.port + "/test";
		GoldilocksDataSource sDataSource = new GoldilocksDataSource();
		sDataSource.setDatabaseName("gop");
		sDataSource.setServerName(sConfig.host.ip);
		sDataSource.setPortNumber(sConfig.host.port);
		sDataSource.setUser(sConfig.host.user);
		sDataSource.setPassword(sConfig.host.password);
		sDataSource.setStatementPoolOn(true);
		sDataSource.setStatementPoolSize(20);

		boolean createConnection = true;
		int i = 0;
		while (createConnection) {
			try {
				return sDataSource.getConnection();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				System.out.println("[SQLSTATE:"+ e.getSQLState() + "] getConnection error! retry con : " + i);

				i++;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;

	}

	public PreparedStatement[] createConAndPstmt(Db db) {
		Connection con = null;
		PreparedStatement[] arrPstmt = null;

		do {
			con = db.createConnection();
			arrPstmt = new PreparedStatement[sConfig.common.length];
			for (int i = 0; i < sConfig.common.length; i++) {
				if (!sConfig.common[i].sqlIsOs) {
					try {
						arrPstmt[i] = con.prepareStatement(sConfig.common[i].sql);
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						// e.printStackTrace();
						System.out.println("con.prepareStatement error");
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							System.out.println("[SQLSTATE:"+ e.getSQLState() + "] MSG : "+e.getMessage());
						}
					}
				}
			}
			if (arrPstmt != null) {
				return arrPstmt;
			}
		} while (arrPstmt == null);
		return arrPstmt;
	}

	public Data getCommonQuery(PreparedStatement[] arrPstmt) {

		ResultCommon[] resultArr = new ResultCommon[sConfig.common.length];

		String sysTimestamp = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss.SSS").create()
				.toJson(new Timestamp(System.currentTimeMillis()));

		for (int i = 0; i < sConfig.common.length; i++) {

			// Statement stmt = con.createStatement();
			boolean alert = false;
			if (sConfig.common[i].sqlIsOs) {
				int queryValue = 0;

				ReadOs oc = new ReadOs();
				queryValue = oc.execute(sConfig.common[i].sql);

				alert = alertCheck(sConfig.common[i], queryValue);

				resultArr[i] = new ResultCommon(sConfig.common[i].name, queryValue, sConfig.common[i].tag, alert);

			} else {
				ResultSet rs;
				try {
					rs = arrPstmt[i].executeQuery();

					// ResultSet rs = stmt.executeQuery(sConfig.common[i].sql);
					ResultSetMetaData rsMeta = rs.getMetaData();
					if (rsMeta.getColumnCount() != 1) {
						System.out
								.println("not support multiple column query _ Query name : " + sConfig.common[i].name);
					}

					int queryValue = 0;
					while (rs.next()) {
						queryValue = rs.getInt(1);
						alert = alertCheck(sConfig.common[i], queryValue);

						resultArr[i] = new ResultCommon(sConfig.common[i].name, queryValue, sConfig.common[i].tag,
								alert);
					}
					rs.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					System.out.println("[SQLSTATE:"+ e.getSQLState() + "] MSG : "+e.getMessage());
					System.out.println(e.getSQLState());
					e.printStackTrace();
					return null;
				}
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

}