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

