# gop
 goldilocks console local monitoring

# program argument : 
sample : <config> <demon | clinet> <time|name|tag|all> <timerange|query name|tag name>  

sample use  >>
- resource/config.json demon
- resource/config.json client time '2022-09-05 03:14:40' '2022-09-05 03:15:00'
- resource/config.json client name execute
- resource/config.json clinen tag tag1
- resource/config.json client all

# alert policy :
- 0 : not use ( default )
- 1 : is greater then alertValue ( query result > alertValue )
- 2 : is less then alertValue ( query result < alertValue )
- 3 : equal to alertValue ( query result = alertValue )