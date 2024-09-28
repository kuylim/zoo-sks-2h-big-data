# Realtime Data Steaming
As a part of my final project for course [CS523-Big Data Technologies](https://compro.miu.edu/courses/#toggle-id-15)
I built a simple `voting system` to demonstrate real-time data streaming on big data ecosystem.

## Prerequisites
Tools
* JDK 11
* Maven 3
* Docker
* Your favorite IDE

Should have a prior experience with using
* `spring boot` we use it as a source of data [Spring Boot](https://spring.io/projects/spring-boot)
* `Kafka` as a message broker to receive data from `spring boot application`

## Tech Stacks
* `Docker` [learn more @docker.com](https://www.docker.com/)
* `Hadoop` [learn more @Big Data Europe (github.com)](https://github.com/big-data-europe)
* `ZooKeeper`
* `Kafka Bitnami` [bitnami/kafka - Docker Image | Docker Hub](https://hub.docker.com/r/bitnami/kafka)
* `Kafka UI` for monitoring tool [kafka-ui](https://github.com/provectus/kafka-ui)
* `Spark` for real-time data analytic, [learn more @Big Data Europe (github.com)](https://github.com/big-data-europe)
* `HBase` as datastore, [learn more @Big Data Europe (github.com)](https://github.com/big-data-europe)
* `Portainer` as a docker management tool [@Portainer](https://www.portainer.io/)

## High level architecture

![enter image description here](https://raw.githubusercontent.com/kuylim/zoo-sks-2h-big-data/main/docker-infra/arch.jpg)

## Boot Up Big Data Environment
🎬🎬🎬To Start all containers please navigate to directory `docker-infra` before execute command `docker-compose up -d` please update the mount directory in `docker-compose.yml` for `spark-master`
````yaml
volumes:  
  - /run/desktop/mnt/host/c/opt/deps:/opt/deps
````
In my case, I use docker desktop [WSL2 ](https://learn.microsoft.com/en-us/windows/wsl/about), if you use `Mac` or `Linux` please update the host path accordingly.
Above mount path use for importing external libraries while execute `spart-submit`.

You all set!!! Now good to execute `docker-compose up -d`
In the `docker-compose.yml` I am not including `Portainer` in there. In case you need a `Docker UI Management Tool` please use below command.

`docker run -d -p 8000:8000 -p 9443:9443 --name portainer --restart=always -v /var/run/docker.sock:/var/run/docker.sock -v portainer_data:/data portainer/portainer-ce:2.21.2`

At this point we have Big Data environment ready 🎉🎉🎉

## Configuration
The environment is ready, but we need a little more configuration for `spark-master` to let `spark` recognize `external jars` during execute `spark-submit`.

Execute into `spark-master`'s container then navigate to directory `spark/conf`
Copy file `spark-defaults.conf.template` to `spark-defaults.conf` and add below configuration
````
park.driver.extraClassPath /path-to-external-jars/*
spark.executor.extraClassPath /path-to-external-jars/*
```` 
My configuration `/path-to-external-jars` is `/opt/deps/*`
Save the configuration file and restart `spark-master`
>All external jars need for execution also provide under directory `deps` in GitHub project.

## Streaming Data
Start the `spring-boot` project. After project up and running - on every 15 seconds `spring-boot` project will randomly publish `voting` data to `Kafka`. `Spark` will consume the `streaming data` starting to perform a basic calculate and save the result to `HBase`

In order to allow `Spark` consume the streaming data we need to submit our `spark-application`.
The build `jar` could be found under directory `build/spark-application.jar` or you can build by yourself with `maven` `mvn clean install`.

Copy `spark-application.jar` into `spark-master`

Submit the `jar` by navigate to `spark/bin/` the execute
````shell
./spark-submit --class [class-name] [path-to-jar]

Example: ./spark-submit --class edu.miu.btd.App /opt/deps/app.jar
````

You would be able to see the steaming data in console logs and result save `HBase` at the same time.

To view data in `HBase`, execute into HBase's container navigate to `/bin` then `hbase shell`.

## Reference
* [Big Data Europe (github.com)](https://github.com/big-data-europe) - big data environment for docker.
* [Bitnami: Packaged Applications for Any Platform - Cloud, Container, Virtual Machine](https://bitnami.com/) - kafka environment for docker.
* [Kafka-UI](https://docs.kafka-ui.provectus.io/) - a fancy tool for Kafka monitoring
* [Kubernetes and Docker Container Management Software (portainer.io)](https://www.portainer.io/) - Docker Management Tool

## Support
