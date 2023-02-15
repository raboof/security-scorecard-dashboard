scalaVersion := "3.2.2"

//val PekkoVersion = "0.0.0+26576-fd932032+20230215-0056-SNAPSHOT"
val PekkoVersion = "0.0.0+26576-fd932032-SNAPSHOT"
val PekkoHttpVersion = "0.0.0+4276-38e1f22f-SNAPSHOT"

//resolvers += "Apache Pekko Nightlies" at "https://nightlies.apache.org/pekko/snapshots"
resolvers += "Apache Pekko Snapshots" at "https://repository.apache.org/content/groups/snapshots"

libraryDependencies ++= Seq(
  "org.apache.pekko" %% "pekko-actor-typed" % PekkoVersion,
  "org.apache.pekko" %% "pekko-stream" % PekkoVersion,
  "org.apache.pekko" %% "pekko-actor-testkit-typed" % PekkoVersion % Test,
  "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
  "io.spray" %% "spray-json" % "1.3.6",
).map(_.cross(CrossVersion.for3Use2_13)) ++ Seq(
  "commons-codec" % "commons-codec" % "1.15",
)
