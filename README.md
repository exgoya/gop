# gop

gop는 Goldilocks Database 모니터링을 위한  Console 도구 입니다.   
gop를 사용하여 아래의 내용으로  Goldilocks database에 대한 상태를 확인 할 수 있습니다.   
   
* Query 추이
* System 자원 사용량
* 임계치 알람 ( 자료 수집  스크립트 발동 )
 
 
## 시작하기: 
소스를 받아서 컴파일 합니다.

~~~
$ git clone https://github.com/exgoya/gop.git
'gop'에 복제합니다...
remote: Enumerating objects: 406, done.
remote: Counting objects: 100% (68/68), done.
remote: Compressing objects: 100% (68/68), done.
remote: Total 406 (delta 33), reused 35 (delta 0), pack-reused 338
오브젝트를 받는 중: 100% (406/406), 805.50 KiB | 15.79 MiB/s, 완료.
델타를 알아내는 중: 100% (232/232), 완료.

$ cd gop

$ sh resource/comp.sh
Manifest를 추가함
추가하는 중: Gop.class(입력 = 8121) (출력 = 4177)(48%를 감소함)
추가하는 중: TestOs.class(입력 = 2728) (출력 = 1539)(43%를 감소함)
추가하는 중: model/Common.class(입력 = 878) (출력 = 523)(40%를 감소함)
추가하는 중: model/Config.class(입력 = 668) (출력 = 404)(39%를 감소함)
추가하는 중: model/Data.class(입력 = 1504) (출력 = 771)(48%를 감소함)
추가하는 중: model/Host.class(입력 = 1218) (출력 = 632)(48%를 감소함)
추가하는 중: model/ResultCommon.class(입력 = 1297) (출력 = 632)(51%를 감소함)
추가하는 중: service/Db.class(입력 = 3586) (출력 = 1958)(45%를 감소함)
추가하는 중: service/ReadLog.class(입력 = 4449) (출력 = 2248)(49%를 감소함)
추가하는 중: service/ReadOs.class(입력 = 2685) (출력 = 1517)(43%를 감소함)
~~~

config 사용하여 jar를 실행하고 goldilocks database trace를 시작합니다.

~~~

[goya@tech10 gop]$ java -Xmx100M -jar gop.jar -config resource/config.json -demon
    NAME : g1n1   HOST : 127.0.0.1   PORT : 30009
                     time       execute       session          lock       long_Tx           tbs   global-ager    group-ager    local-ager          stmt    disk-/home     MemAvaimb
"2022-11-25 18:24:17.255"        527353             3             1             2             1             0             0             1            10            71         26044
"2022-11-25 18:24:18.332"             9             3             1             2             1             0             0             1            10            71         26040
"2022-11-25 18:24:19.351"             9             3             1             2             1             0             0             1            10            71         26040
"2022-11-25 18:24:20.370"             9             3             1             2             1             0             0             1            10            71         26038
"2022-11-25 18:24:21.388"             9             3             1             2             1             0             0             1            10            71         26035
"2022-11-25 18:24:22.407"             9             3             1             2             1             0             0             1            10            71         26034
"2022-11-25 18:24:23.426"             9             3             1             2             1             0             0             1            10            71         26032
"2022-11-25 18:24:24.448"             9             3             1             2             1             0             0             1            10            71         26033
"2022-11-25 18:24:25.466"             9             3             1             2             1             0             0             1            10            71         26028
"2022-11-25 18:24:26.484"             9             3             1             2             1             0             0             1            10            71         26028

~~~


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

~~~
{
	"host": {
		"name": "g1n1",
		"ip": "192.168.0.119",
		"port": "30009",
		"user": "test",
		"password": "test",
		"timeInterval": 1000,
		"logPath": "resource/",
		"print": "true",
		"pagesize":"10"
	},
	"common": [
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
	
~~~

## alert policy :
- 0 : not use ( default )
- 1 : is greater then alertValue ( query result > alertValue )
- 2 : is less then alertValue ( query result < alertValue )
- 3 : equal to alertValue ( query result = alertValue )
