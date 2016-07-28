import sbt._
import Keys._
import sbt.Project.projectToRef
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform._
import scalariform.formatter.preferences._

lazy val appV = "0.0.1"
lazy val scalaV = "2.11.8"
lazy val clients = Seq(client)

lazy val versions = new {

  val silhouette = "4.0.0-RC1"
  val spark = "2.0.0"
  val hadoop = "2.7.0"
  val akka = "2.4.7"
  val slick = "3.1.1"
  val playSlick = "2.0.0"
  val psqlJdbc = "9.4-1206-jdbc41"
  val slickPostgres = "0.14.1"
  val playMailer = "5.0.0-M1"
  val playRecaptcha = "2.0"
  val guice = "4.0.1"
  val ficus = "1.2.6"
  val webjars = "2.5.0-2"
  val jackson = "2.7.4"
  val playBootstrap = "1.0-P25-B3"
  val playScalaJS = "0.5.0"
  val playScalatest = "1.5.1"
  val mockito = "2.0.2-beta"

  val dom = "0.9.0"
  val jquery = "0.9.0"

}

lazy val commonSettings = Seq(
  version := appV,
  scalaVersion := scalaV,
  scalacOptions ++= scalaCompilerOptions,
  resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  resolvers += Resolver.jcenterRepo,
  ScalariformKeys.preferences := ScalariformKeys.preferences.value
    .setPreference(FormatXml, false)
    .setPreference(DoubleIndentClassDeclaration, false)
    .setPreference(DanglingCloseParenthesis, Preserve)
    .setPreference(AlignParameters, false)
    .setPreference(CompactStringConcatenation, false)
    .setPreference(IndentPackageBlocks, true)
    .setPreference(PreserveSpaceBeforeArguments, false)
    .setPreference(RewriteArrowSymbols, true)
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 40)
    .setPreference(SpaceBeforeColon, false)
    .setPreference(SpaceInsideBrackets, false)
    .setPreference(SpaceInsideParentheses, false)
    .setPreference(IndentSpaces, 2)
    .setPreference(IndentLocalDefs, false)
    .setPreference(SpacesWithinPatternBinders, true)
    .setPreference(SpacesAroundMultiImports, true)
)

lazy val server = (project in file("server"))
  .settings(SbtScalariform.defaultScalariformSettings: _*)
  .settings(commonSettings: _*)
  .settings(
    name := "server",
    fork in run := true,
    connectInput in run := true,
    routesGenerator := InjectedRoutesGenerator,
    scalaJSProjects := clients,
    javaOptions ++= jvmOptions,
    pipelineStages := Seq(scalaJSProd, gzip),
    libraryDependencies ++= Seq(
      cache,
      filters,
      "com.mohiva" %% "play-silhouette" % versions.silhouette,
      "com.mohiva" %% "play-silhouette-password-bcrypt" % versions.silhouette,
      "com.mohiva" %% "play-silhouette-persistence" % versions.silhouette,
      "com.mohiva" %% "play-silhouette-crypto-jca" % versions.silhouette,
      "org.apache.spark" %% "spark-core" % versions.spark,
      "org.apache.spark" %% "spark-sql" % versions.spark,
      "org.apache.spark" %% "spark-mllib" % versions.spark,
      "org.apache.spark" %% "spark-streaming" % versions.spark,
//      "org.apache.spark" %% "spark-streaming-twitter" % versions.spark,
      "org.apache.hadoop" % "hadoop-client" % versions.hadoop,
      "com.fasterxml.jackson.core" % "jackson-databind" % versions.jackson,
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % versions.jackson,
      "com.typesafe.akka" % "akka-actor_2.11" % versions.akka,
      "com.typesafe.play" %% "play-slick" % versions.playSlick,
      "com.typesafe.play" %% "play-slick-evolutions" % versions.playSlick,
      "com.typesafe.slick" % "slick-hikaricp_2.11" % versions.slick,
      "com.github.tminglei" %% "slick-pg" % versions.slickPostgres,
      "com.github.tminglei" %% "slick-pg_date2" % versions.slickPostgres,
      "org.postgresql" % "postgresql" % versions.psqlJdbc,
      "com.typesafe.play" %% "play-mailer" % versions.playMailer,
      "com.nappin" %% "play-recaptcha" % versions.playRecaptcha,
      "org.webjars" %% "webjars-play" % versions.webjars,
      "net.codingwell" %% "scala-guice" % versions.guice,
      "com.iheart" %% "ficus" % versions.ficus,
      "com.adrianhurt" %% "play-bootstrap" % versions.playBootstrap,
      "com.vmunier" %% "play-scalajs-scripts" % versions.playScalaJS,
      "com.mohiva" %% "play-silhouette-testkit" % versions.silhouette % "test",
      "org.scalatestplus.play" %% "scalatestplus-play" % versions.playScalatest % "test",
      "org.mockito" % "mockito-all" % versions.mockito % "test"
    )
  ).enablePlugins(PlayScala)
    .disablePlugins(SbtScalariform)
    .aggregate(clients.map(projectToRef): _*)
    .dependsOn(sharedJvm)


lazy val client = (project in file("client"))
  .settings(SbtScalariform.defaultScalariformSettings: _*)
  .settings(commonSettings: _*).settings(
    name := "client",
    scalaJSUseRhino in Global := false,
    persistLauncher := true,
    persistLauncher in Test := false,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % versions.dom,
      "be.doeraene" %%% "scalajs-jquery" % versions.jquery
    )
  ).enablePlugins(ScalaJSPlugin, ScalaJSPlay)
    .disablePlugins(SbtScalariform)
    .dependsOn(sharedJs)

lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared"))
  .settings(scalaVersion := scalaV)
  .jsConfigure(_ enablePlugins ScalaJSPlay)

lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

lazy val scalaCompilerOptions = Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
  "-Xlint", // Enable recommended additional warnings.
  "-Ywarn-adapted-args", // Warn if an argument list is modified to match the receiver.
  "-Ywarn-dead-code", // Warn when dead code is identified.
  "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
  "-Ywarn-nullary-override", // Warn when non-nullary overrides nullary, e.g. def foo() over def foo.
  "-Ywarn-numeric-widen" // Warn when numerics are widened.
)

lazy val jvmOptions = Seq(
  "-Xms256M",
  "-Xmx2G",
  "-XX:MaxPermSize=2048M",
  "-XX:+UseConcMarkSweepGC"
)

// onLoad in Global := (Command.process("project server", _: State)) compose (onLoad in Global).value
