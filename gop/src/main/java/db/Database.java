package db;

import java.sql.*;
import java.util.Properties;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import model.Data;
import model.JdbcProperty;
import model.JdbcSource;
import model.ResultCommon;
import model.Measure;
import io.ReadOs;

public class Database {
	JdbcSource jdbcSource;
	Measure[] measure;
	String source;

	public Database(JdbcSource jdbcSource, Measure[] measure, String source) {
		this.jdbcSource = jdbcSource;
		this.measure = measure;
		this.source = source;
	}

	public Connection createConnection()
    {
        try
        {
            Class.forName(jdbcSource.driverClass);
        }
        catch (ClassNotFoundException sException)
        {
        }
		Properties prop = new Properties();
		JdbcProperty jpArr[] = jdbcSource.jdbcProperties;

		for (JdbcProperty jdbcProperty : jpArr) {
			prop.setProperty(jdbcProperty.name, jdbcProperty.value);
		}
		boolean createConnection = true;
		int i = 0;

		String dbUrl=jdbcSource.url + jdbcSource.dbName;
		while (createConnection) {
			try {	
				return DriverManager.getConnection(dbUrl, prop);
			} catch (SQLException e) {
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

	public PreparedStatement[] createConAndPstmt(Database db) {
		Connection con = null;
		PreparedStatement[] arrPstmt = null;

		do {
			con = db.createConnection();
			arrPstmt = new PreparedStatement[measure.length];
			for (int i = 0; i < measure.length; i++) {
				if (!measure[i].sqlIsOs) {
					try {
						arrPstmt[i] = con.prepareStatement(measure[i].sql);
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

	public static class ConnAndStmt {
		public Connection con;
		public PreparedStatement[] stmts;

		ConnAndStmt(Connection con, PreparedStatement[] stmts) {
			this.con = con;
			this.stmts = stmts;
		}
	}

	public ConnAndStmt createConAndPstmtWithConnection() {
		Connection con;
		PreparedStatement[] arrPstmt;
		do {
			con = createConnection();
			arrPstmt = new PreparedStatement[measure.length];
			for (int i = 0; i < measure.length; i++) {
				if (!measure[i].sqlIsOs) {
					try {
						arrPstmt[i] = con.prepareStatement(measure[i].sql);
					} catch (SQLException e) {
						System.out.println("con.prepareStatement error");
						try {
							Thread.sleep(1000);
						} catch (InterruptedException e1) {
							System.out.println("[SQLSTATE:"+ e.getSQLState() + "] MSG : "+e.getMessage());
						}
					}
				}
			}
			if (arrPstmt != null) {
				return new ConnAndStmt(con, arrPstmt);
			}
		} while (arrPstmt == null);
		return new ConnAndStmt(con, arrPstmt);
	}

	public Data getCommonQuery(PreparedStatement[] arrPstmt) {

		ResultCommon[] resultArr = new ResultCommon[measure.length];

		DateTimeFormatter formatDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
		String sysTimestamp = formatDateTime.format(LocalDateTime.now());

		for (int i = 0; i < measure.length; i++) {

			// Statement stmt = con.createStatement();
			boolean alert = false;
			if (measure[i].sqlIsOs) {
				long queryValue = 0;

				ReadOs oc = new ReadOs();
				queryValue = oc.execute(measure[i].sql);

				alert = alertCheck(measure[i], queryValue);

				resultArr[i] = new ResultCommon(measure[i].name, queryValue, measure[i].tag, alert);

			} else {
				ResultSet rs;
				try {
					rs = arrPstmt[i].executeQuery();

					// ResultSet rs = stmt.executeQuery(sConfig.common[i].sql);
					ResultSetMetaData rsMeta = rs.getMetaData();
					if (rsMeta.getColumnCount() != 1) {
						System.out
								.println("not support multiple column query _ Query name : " + measure[i].name);
					}

					long queryValue = 0;
					while (rs.next()) {
						queryValue = rs.getLong(1);
						alert = alertCheck(measure[i], queryValue);

						resultArr[i] = new ResultCommon(measure[i].name, queryValue, measure[i].tag,
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
		return new Data(sysTimestamp, source, resultArr);
	}

	private boolean alertCheck(Measure common, long queryValue) {
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
