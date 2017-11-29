addSbtPlugin("com.lightbend.sbt" % "sbt-proguard" % "0.3.0")

addSbtPlugin("com.lucidchart" % "sbt-scalafmt" % "1.14")

libraryDependencies ++= Seq(
  "org.apache.httpcomponents" % "httpclient" % "4.5.2",
  "org.apache.httpcomponents" % "httpmime" % "4.5.2",
  "com.jsuereth" %% "scala-arm" % "2.0"
)

scalacOptions ++= Seq("-deprecation", "-feature")
