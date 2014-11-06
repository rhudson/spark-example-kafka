# spark-example-kafka

This is a self-contained example project to illustrate using Spark Streaming with Kafka.

Firstly, build the JAR:

    sbt assembly

Next, create your Kafka topic:

    kafka-topics.sh --zookeeper localhost:2181 --create --topic wordcount --partitions 1 --replication-factor 1

Run the KafkaWordCountProducer to produce 10 messages per second with 100 words per message:

    bin/KafkaWordCountProducer localhost:9092 wordcount 10 100

Last but not least, run the KafkaWordCount example:

    bin/KafkaWordCount localhost:2181 KafkaWordCount wordcount 1

Check ```build.sbt``` to see how the dependencies are setup and workarounds for classpath collisions while building the assembly.

## Run the KafkaWordCount example as a NON-fat jar.

Build the driver program and push to the server from which it will be run.

	sbt clean package
	scp ./target/scala-2.10/spark-example-kafka_2.10-1.0.jar prodspark01.ies:/work

Bundle the dependency jars and push to every node of the Spark cluster.

	sbt pack
	zip -j libs.zip target/pack/lib/*.jar -x "*scala-*"
	scp libs.zip prodspark01.ies:/work
	scp libs.zip prodspark02.ies:/work
	scp libs.zip prodspark03.ies:/work

Put the jar files in a consistent location on all nodes of the Spark cluster.

	rm -rf /work/libs; unzip -d /work/libs /work/libs.zip

	# Bounce the master
	pkill -f java; sleep 5; /work/programs/spark-1.0.0-bin-hadoop2/sbin/start-master.sh

	# Bounce the slaves
	pkill -f java; sleep 5; /work/programs/spark-1.0.0-bin-hadoop2/sbin/start-slave.sh 1 spark://172.31.16.164:7077

The class path for the executor must be specified in the conf/spark-defaults.conf file.

	$ egrep -v "^#|^$" /work/programs/spark-1.0.0-bin-hadoop2/conf/spark-defaults.conf
	spark.executor.extraClassPath=/work/libs/*

The spark-submit program supports the specification of the class path for the driver program (this may be different from the class path of the executor).  e.g. - 

	/work/programs/spark-1.0.0-bin-hadoop2/bin/spark-submit --class spark.example.kafka.KafkaWordCount --master spark://172.31.16.164:7077 --driver-class-path /work/libs/\* /work/spark-example-kafka_2.10-1.0.jar devkafka01.ies:2181 kafkaSparkTestGroup wordcount 1


## Run the KafkaWordCount example as dockerized application.

Build the Docker image and push to the registry.

	sbt clean package pack
	rm target/pack/lib/scala-*.jar
	docker build -t docker.ies:5000/spark-kafka-example .
	docker push docker.ies:5000/spark-kafka-example

Ensure that the target cluster is running from the correct base image.  Otherwise the dependencies may not be on the classpath.
	
	weave run 10.0.1.1/24 -d -v /var/log/spark:/var/log/spark -e RUN_AS_MASTER=1 -p 8080:8080 docker.ies:5000/ies-analytics-libs:3.1.0
	weave run 10.0.1.11/24 -d -v /var/log/spark:/var/log/spark -e SPARK_MASTER_IP=10.0.1.1 docker.ies:5000/ies-analytics-libs:3.1.0
	weave run 10.0.1.12/24 -d -v /var/log/spark:/var/log/spark -e SPARK_MASTER_IP=10.0.1.1 docker.ies:5000/ies-analytics-libs:3.1.0

Start up a container from which to launch the driver program.

	C=$(weave run 10.0.1.103/24 -t -i -p 4040:4040 docker.ies:5000/spark-kafka-example /bin/bash)
	docker attach $C

Run the driver program from within the container.

	$SPARK_HOME/bin/spark-submit --class spark.example.kafka.KafkaWordCount --master spark://10.0.1.1:7077 --driver-class-path /app/libs/\* /app/spark-example-kafka_2.10-1.0.jar devkafka01.ies:2181 kafkaSparkTestGroup wordcount 1


Kick off a producer locally.

	bin/KafkaWordCountProducer devkafka01.ies:9092 wordcount 10 100

