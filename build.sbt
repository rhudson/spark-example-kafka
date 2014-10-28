import AssemblyKeys._

assemblySettings

name := "spark-example-kafka"

version := "1.0"

scalaVersion := "2.10.4"

jarName in assembly := "spark-example-kafka_2.10-1.0.jar"

net.virtualvoid.sbt.graph.Plugin.graphSettings

assemblyOption in assembly ~= { _.copy(includeScala = false) }

packAutoSettings


libraryDependencies ++= Seq(
  "org.apache.spark" %% "spark-core" % "1.0.0" % "provided",
  "org.apache.spark" %% "spark-streaming" % "1.0.0" % "provided",
  ("org.apache.spark" %% "spark-streaming-kafka" % "1.0.0").
    exclude("commons-beanutils", "commons-beanutils").
    exclude("commons-collections", "commons-collections").
    // These lines should be added when assembling the uber jar for submission to Spark cluster
    exclude("org.apache.spark", "spark-core_2.10").
    exclude("org.apache.spark", "spark-streaming_2.10").
    exclude("javax.jms", "jms").
    exclude("com.esotericsoftware.minlog", "minlog")
)

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case x if x.startsWith("META-INF/ECLIPSEF.RSA") => MergeStrategy.last
    case x if x.startsWith("META-INF/mailcap") => MergeStrategy.last
    case x if x.startsWith("plugin.properties") => MergeStrategy.last
    case x => old(x)
  }
}
