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
mkdir: `bin' 디렉토리를 만들 수 없습니다: File exists
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
[centos@goya-db1 gop]$ java -Xmx100M -jar gop.jar resource/config.json demon
time       execute       session          lock    long_query           tbs   global-ager    group-ager    local-ager          stmt     MemAvaimb
"2022-11-07 16:52:49.721"       7516919             1             0             0             1             0             0             0             9          2686
"2022-11-07 16:52:50.831"             9             1             0             0             1             0             0             0             9          2685
"2022-11-07 16:52:51.907"             9             1             0             0             1             0             0             0             9          2684
"2022-11-07 16:52:52.970"             9             1             0             0             1             0             0             0             9          2684
"2022-11-07 16:52:54.032"             9             1             0             0             1             0             0             0             9          2684
"2022-11-07 16:52:55.090"             9             1             0             0             1             0             0             0             9          2683
"2022-11-07 16:52:56.155"             9             1             0             0             1             0             0             0             9          2680
"2022-11-07 16:52:57.215"             9             1             0             0             1             0             0             0             9          2680
~~~


#### run
 
sample : <config> <demon | clinet> <time|name|tag|all> <timerange|query name|tag name>  

sample use  >>
- resource/config.json demon
- resource/config.json client time '2022-09-05 03:14:40' '2022-09-05 03:15:00'
- resource/config.json client name execute
- resource/config.json clinen tag tag1
- resource/config.json client all

ex )
 java -Xmx100M -jar resource/config.json gop.jar demon

## alert policy :
- 0 : not use ( default )
- 1 : is greater then alertValue ( query result > alertValue )
- 2 : is less then alertValue ( query result < alertValue )
- 3 : equal to alertValue ( query result = alertValue )
