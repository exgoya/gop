import java.sql.*;

import com.google.gson.GsonBuilder;

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
			
			while (rs.next()) {
			    String sysTimestamp = new GsonBuilder()
	               .setDateFormat("yyyy-MM-dd hh:mm:ss")
	               .create()
	               .toJson(new Timestamp(System.currentTimeMillis()));
				resultArr[i] = new ResultCommon(sConfig.common[i].name,rs.getInt(1),sConfig.common[i].tag,sysTimestamp);
			}
			
		}
		return resultArr;
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