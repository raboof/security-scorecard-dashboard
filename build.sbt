scalaVersion := "3.4.2"

val PekkoVersion = "1.1.0-M1"
val PekkoHttpVersion = "1.1.0-M1"

//resolvers += "Apache Pekko Nightlies" at "https://nightlies.apache.org/pekko/snapshots"
//resolvers += "Apache Pekko Snapshots" at "https://repository.apache.org/content/groups/snapshots"

libraryDependencies ++= Seq(
  "org.apache.pekko" %% "pekko-actor-typed" % PekkoVersion,
  "org.apache.pekko" %% "pekko-stream" % PekkoVersion,
  "org.apache.pekko" %% "pekko-actor-testkit-typed" % PekkoVersion % Test,
  "org.apache.pekko" %% "pekko-http" % PekkoHttpVersion,
  "com.lihaoyi" %% "scalatags" % "0.13.1",
  "io.spray" %% "spray-json" % "1.3.6",
).map(_.cross(CrossVersion.for3Use2_13)) ++ Seq(
  "commons-codec" % "commons-codec" % "1.15",
  "commons-io" % "commons-io" % "2.11.0",
)
