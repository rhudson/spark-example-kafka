FROM docker.ies:5000/docker-spark-1.0.2

ADD target/scala-2.10/spark-example-kafka_2.10-1.0.jar /app/
ADD target/pack/lib/*.jar /app/libs/


