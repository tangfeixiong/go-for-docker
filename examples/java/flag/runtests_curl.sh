#!/bin/bash -e

# curl http://172.17.4.50:8082/checkLogin -X POST -d '{"username": "admin", "passwd": "123456"}' -jkSL
DATETIME=$(date +%Y-%m-%dT%H:%M:%S -d "+15 seconds")
case $1 in
    create)
	    curl http://172.17.4.50:8082/v1/refresh-creation \
		    -H "Content-Type: application/json" -X POST -iv -d \
"{
  \"id\": 1,
  \"image_id\": 1,
  \"battlefield_id\": 1,
  \"name\": \"test\",
  \"periodic\": 15,
  \"refreshing_rfc3339\": \"${DATETIME}\",
  \"rounds\": 10,
  \"count\": 0,
  \"data_store\": \"test1/\",
  \"config\":
    {
      \"id\": 1,
      \"common\": \"20170826\",
      \"count\": 10,
      \"environment_count\": 1
    },
  \"refreshing_info\":
    {
      \"test\":
        {
          \"container_id\": 1,
          \"refresh_config_id\": 1,
          \"team_id\": 1,
          \"name\": \"flag\",
          \"sub_path\": \"demo1/\",
          \"flag\":
            {
              \"id\": 1,
              \"env\": 1,
              \"teamNo\": 1
            }
        }
    }
}"
        ;;
    signin)
	    curl http://172.17.4.50:8082/checkLogin \
		    -H "Content-Type: application/json" -X POST -iv -d \
'{
  "username": "admin",
  "passwd": "123456"    
}'
    ;;
    open)
	    curl http://172.17.4.50:8082/v1/open \
		    -H "Content-Type: application/json" -X GET -iv
    ;;
    *)
        echo "Valid test: create, reap, update, delete"
        ;;
	
esac


