import sbt.Keys._

resolvers in ThisBuild ++= Seq("Apache Development Snapshot Repository" at "https://repository.apache.org/content/repositories/snapshots/", Resolver.sonatypeRepo("public"))

name := "flink-pagerank"

lazy val commonSettings = Seq(
    organization := "com.datablogger",
    version := "0.1.0-SNAPSHOT"
)

val flinkVersion = "1.1.1"

val flinkDependencies = Seq(
    "org.apache.flink" %% "flink-scala" % flinkVersion % "provided",
    "org.apache.flink" %% "flink-clients" % flinkVersion,
    "org.apache.flink" %% "flink-gelly-scala" % flinkVersion
)

lazy val root = (project in file(".")).
    settings(commonSettings: _*).
    settings(
        libraryDependencies ++= flinkDependencies
    ).
    enablePlugins(AssemblyPlugin)

run in Compile <<= Defaults.runTask(fullClasspath in Compile, mainClass in(Compile, run), runner in(Compile, run))

mainClass in assembly := Some("ch.cern.Experiment")
assemblyOption in assembly := (assemblyOption in assembly).value.copy(includeScala = false)

mergeStrategy in assembly := {
    case PathList("org", "apache", "spark", "unused", "UnusedStubClass.class") => MergeStrategy.first
    case x => (mergeStrategy in assembly).value(x)
}