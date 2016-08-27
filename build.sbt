name := """play-rest-validation"""

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  cache,
  ws,
  "com.gu"                 %% "scanamo"            % "0.7.0",
  "net.codingwell"         %% "scala-guice"        % "4.0.1",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test
)

// Set up ScalaStyle to be a compile task
lazy val compileScalastyle = taskKey[Unit]("compileScalastyle")

compileScalastyle := org.scalastyle.sbt.ScalastylePlugin.scalastyle.in(Compile).toTask("").value

(compile in Compile) <<= (compile in Compile) dependsOn compileScalastyle

// DynamoDB Local
dynamoDBLocalDownloadDir := file("dynamodb-local")
dynamoDBLocalPort := 8000
dynamoDBLocalSharedDB := true
startDynamoDBLocal <<= startDynamoDBLocal.dependsOn(compile in Test)
test in Test <<= (test in Test).dependsOn(startDynamoDBLocal)
testOptions in Test <+= dynamoDBLocalTestCleanup