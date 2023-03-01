# CES
Code Execution Service

### Description
This service allows to execute a code in secure environment 
and makes sure the results of the execution are available.

More detailed description can be found [here](design/design.md).

### Build
```./gradlew clean build```

### Build without tests
```./gradlew clean build -x test```

### Run
```docker-compose up```

### Test
- REST API is available on `8080` port. See this [postman collection](ces.postman_collection.json) with example requests.
- [RabbitMQ](https://www.rabbitmq.com/) UI is available on `15672` port. It can be used to monitor code execution events.
- [MinIO](https://min.io/) UI is available on `9001` port. It can be used in addition to REST API to observe code execution artifacts.
