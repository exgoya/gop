# gop

gop는 Database 모니터링을 위한  Console 도구 입니다.   
gop를 사용하여 아래의 내용으로 database에 대한 상태를 확인 할 수 있습니다.   
   
* Query 추이
* System 자원 사용량
* 임계치 알람 ( 자료 수집  스크립트 발동 )
 
 
## 시작하기: 
소스를 받아서 컴파일 합니다.

```
$ git clone https://github.com/exgoya/gop17.git
'gop'에 복제합니다...
remote: Enumerating objects: 406, done.
remote: Counting objects: 100% (68/68), done.
remote: Compressing objects: 100% (68/68), done.
remote: Total 406 (delta 33), reused 35 (delta 0), pack-reused 338
오브젝트를 받는 중: 100% (406/406), 805.50 KiB | 15.79 MiB/s, 완료.
델타를 알아내는 중: 100% (232/232), 완료.

$ cd gop17

$ ls
README.md       app             data            gradle          gradlew         gradlew.bat     settings.gradle
$ ./gradlew build

BUILD SUCCESSFUL in 4s
7 actionable tasks: 7 executed
$ cd app/build/distributions 
$ ls
app.tar app.zip
$ unzip app.zip
Archive:  app.zip
   creating: app/
   creating: app/lib/
  inflating: app/lib/app.jar         
  inflating: app/lib/goldilocks8.jar  
  inflating: app/lib/guava-30.1.1-jre.jar  
  inflating: app/lib/httpclient-4.5.14.jar  
  inflating: app/lib/gson-2.10.1.jar  
  inflating: app/lib/failureaccess-1.0.1.jar  
  inflating: app/lib/listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar  
  inflating: app/lib/jsr305-3.0.2.jar  
  inflating: app/lib/checker-qual-3.8.0.jar  
  inflating: app/lib/error_prone_annotations-2.5.1.jar  
  inflating: app/lib/j2objc-annotations-1.3.jar  
  inflating: app/lib/httpcore-4.4.16.jar  
  inflating: app/lib/commons-logging-1.2.jar  
  inflating: app/lib/commons-codec-1.11.jar  
   creating: app/bin/
  inflating: app/bin/app             
  inflating: app/bin/app.bat         
$ ls -alh
total 20360
drwxr-xr-x@  5 exgoya  staff   160B  3  2 13:22 .
drwxr-xr-x@ 10 exgoya  staff   320B  3  2 13:22 ..
drwxr-xr-x@  4 exgoya  staff   128B  3  2 13:22 app
-rw-r--r--@  1 exgoya  staff   5.3M  3  2 13:22 app.tar
-rw-r--r--@  1 exgoya  staff   4.7M  3  2 13:22 app.zip
$ cd app
$ mkdir data
$ cp ~/git/gop17/data/config.json ./data
$ ls
bin     data    lib

```
  
## start 
  
```
$ ./bin/app -config data/config.json -demon
Current dir : /Users/exgoya/git/gop17/app/build/distributions/app
Source jdbc url : jdbc:goldilocks://192.168.0.120:30009/
Write file : true
Write file path : data/
Write stacker : true
Write stacker : http://192.168.0.120:5108/dbs/
Write stacker db name: gop
post err : http://192.168.0.120:5108/dbs/gop

                     time          exec       optdata       session     g-session          peer          lock       long_Tx
"2023-03-02 13:24:01.217"         78655           797             1             0             0             0             0
post err : http://192.168.0.120:5108/dbs/gop
"2023-03-02 13:24:02.822"             7             5             1             0             0             0             0
post err : http://192.168.0.120:5108/dbs/gop
"2023-03-02 13:24:03.846"             7             0             1             0             0             0             0
```

#### Help
 
~~~
$ java -jar gop.jar -help
 ---
 -config <config file path> [ -demon | -client -log <log file path> <option> ]

 client option:
   -log <log file path> [ -time 'yyyy-mm-dd hh24:mi:ss.fff' 'yyyy-mm-dd hh24:mi:ss.fff' | -name <column name> | -tag <tag name> ]
			[ -head | -tail <print count> ]

 ---
 sample use
 java -Xmx100M -jar gop.jar -config resource/config.json -demon
 java -jar gop.jar -config resource/config.json -client -log resource/log_20221201.json -time '2022-12-01 03:14:40.000' '2022-12-01 03:15:00.000'
 java -jar gop.jar -config resource/config.json -client -log resource/log_20221201.json -name execute -tail 10
 java -jar gop.jar -config resource/config.json -client -log resource/log_20221201.json -tag tag1 -head 10
 java -jar gop.jar -config resource/config.json -client -log resource/log_20221201.json
~~~


## Config

config 명세  

~~~

host : 모니터링 서버 환경 정보  
	name : String - DB 이름을 명시한다 ( ex. g1n1 , g2n1 )  
	ip : String - DB IP  
	port : int - DB PORT  
	user : String - DB USERNAME  
	password : String - DB password  
	timeInterval : int - monitoring time interval  
	print : boolean - print monitoring log  
	pagesize : int - print column header between row  
	
common  
	name : String - monitoring column name  
	tag : String - monitoring tag name  
	diff : boolean - diff before values  
	alertValue : int  
	alertPolicy : int  
	sql : String - monitoring query  
	sqlIsOs : boolean - sql command is os command  
	alertScript : String   
	alertScriptIsOs : boolean  

~~~

config sample

```
{
	"setting": {
		"jdbcSource" : {
			"url":"jdbc:goldilocks://192.168.0.120:30009/",
			"dbName": "gop",
			"driverClass":"sunje.goldilocks.jdbc.GoldilocksDriver",
			"jdbcProperties":[
				{ "name":"user", "value":"test" },
				{ "name":"password", "value":"test" },
				{ "name":"statement_pool_on", "value":"true" },
				{ "name":"statement_pool_size", "value":"20" },
				{ "name":"login_timeout", "value":"3" }
			]
		},
		"timeInterval": 1000,
		"consolePrint": true,
		"pageSize": "10",
		"printType":"ansi",
		"fileLog" : {
			"enable":true,
			"logPath": "data/"
		},
		"stacker" : {
			"enable":true,
			"baseUrl": "http://192.168.0.120:5108/dbs/",
			"dbName":"gop"
		}
	},
	"measure": [
		{
			"name": "exec",
			"diff": true,
			"member" : "g1n1",
			"sql": "select stat_value from v$system_sql_stat where stat_name = 'CALL_EXECUTE'",
			"tag": "sql1"
		},
		{
			"name": "optdata",
			"diff": true,
			"sql": "select stat_value from v$system_sql_stat where stat_name = 'CALL_OPTDATA'",
			"tag": "sql1"
		},
		{
			"name": "session",
			"alertValue": 80,
			"alertPolicy": 1,
			"sql": "select count(*) from x$session@local where top_layer = 13",
			"tag": "sql1"
		},
		{
			"name": "g-session",
			"alertValue": 80,
			"alertPolicy": 1,
			"sql": "select count(*) from x$session@local where top_layer = 13 and global_connection is true",
			"tag": "sql1"
		},
		{
			"name": "peer",
			"alertValue": 80,
			"alertPolicy": 1,
			"sql": "select count(*) from x$session@local where program = 'cluster peer'",
			"tag": "sql1"
		},
		{
			"name": "lock",
			"alertValue": 0,
			"alertPolicy": 1,
			"alertSql": "select * from tech_lockwait",
			"sql": "select count(*) from v$lock_wait",
			"tag": "sql1",
			"alertScript": "echo \"select * from tech_lockwait;\"|gsqlnet sys gliese --no-prompt",
			"alertScriptIsOs": true
		},
		{
			"name": "long_Tx",
			"tag": "sql1",
			"alertValue": 0,
			"alertPolicy": 3,
			"sql": "select count(*) from x$transaction@local where datediff(SECOND,BEGIN_TIME,systimestamp) > 10",
			"alertScript":"free -wh",
			"alertScriptIsOs": true
		},
		{
			"name": "MemAvaimb",
			"tag": "sql1",
			"diff": false,
			"alertValue": 80,
			"alertPolicy": 1,
			"sql": "cat /proc/meminfo |grep MemAvailable|awk {'print int($2/1024)'}",
			"sqlIsOs": true,
			"alertScript":"select * from x$cluster_member",
			"alertScriptIsOs": false
		}
	]
}

```

## alert policy :
- 0 : not use ( default )
- 1 : is greater then alertValue ( query result > alertValue )
- 2 : is less then alertValue ( query result < alertValue )
- 3 : equal to alertValue ( query result = alertValue )
