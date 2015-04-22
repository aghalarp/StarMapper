name := "starmapper"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  "org.seleniumhq.selenium" % "selenium-java" % "2.38.0" % "test",
  "mysql" % "mysql-connector-java" % "5.1.21",
  "com.amazonaws" % "aws-java-sdk" % "1.9.14"
)     

play.Project.playJavaSettings
