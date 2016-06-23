import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform._

import scalariform.formatter.preferences._

name := "dev-play-app"

version := "4.0.0-RC1"

scalaVersion := "2.11.8"

lazy val versions = new {
  val silhouette = "4.0.0-RC1"
  val guice = "4.0.1"
  val akka = "2.4.7"
  val slick = "3.1.1"
  val playSlick = "2.0.0"
  val psqlJdbc = "9.4-1206-jdbc41"
  val slickPostgres = "0.14.1"
  val playMailer = "5.0.0-M1"
  val ficus = "1.2.6"
  val webjars = "2.5.0-2"
  val playBootstrap = "1.0-P25-B3"
  val scalatest = "2.2.6"
}

fork in run := true

resolvers += Resolver.jcenterRepo

resolvers += Resolver.bintrayRepo("insign", "play-cms")

libraryDependencies ++= Seq(
  "com.mohiva" %% "play-silhouette" % versions.silhouette,
  "com.mohiva" %% "play-silhouette-password-bcrypt" % versions.silhouette,
  "com.mohiva" %% "play-silhouette-persistence" % versions.silhouette,
  "com.mohiva" %% "play-silhouette-crypto-jca" % versions.silhouette,
  "com.typesafe.akka" % "akka-actor_2.11" % versions.akka,
  "com.typesafe.play" %% "play-slick" % versions.playSlick,
  "com.typesafe.play" %% "play-slick-evolutions" % versions.playSlick,
  "com.typesafe.slick" % "slick-hikaricp_2.11" % versions.slick,
  "com.github.tminglei" %% "slick-pg" % versions.slickPostgres,
  "com.github.tminglei" %% "slick-pg_date2" % versions.slickPostgres,
  "org.postgresql" % "postgresql" % versions.psqlJdbc,
  "com.typesafe.play" %% "play-mailer" % versions.playMailer,
  "org.webjars" %% "webjars-play" % versions.webjars,
  "net.codingwell" %% "scala-guice" % versions.guice,
  "com.iheart" %% "ficus" % versions.ficus,
  "com.adrianhurt" %% "play-bootstrap" % versions.playBootstrap,
  "com.mohiva" %% "play-silhouette-testkit" % versions.silhouette % "test",
  "org.scalatest" %% "scalatest" % versions.scalatest % "test",
  specs2 % Test,
  cache,
  filters
)

lazy val root = (project in file(".")).enablePlugins(PlayScala).disablePlugins(SbtScalariform)

routesGenerator := InjectedRoutesGenerator

scalacOptions ++= Seq(
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

//********************************************************
// Scalariform settings
//********************************************************

SbtScalariform.defaultScalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(FormatXml, false)
  .setPreference(DoubleIndentClassDeclaration, false)
  .setPreference(DanglingCloseParenthesis, Preserve)
  .setPreference(AlignParameters, true)
  .setPreference(CompactStringConcatenation, false)
  .setPreference(IndentPackageBlocks, true)
  .setPreference(PreserveSpaceBeforeArguments, false)
  .setPreference(RewriteArrowSymbols, false)
  .setPreference(AlignSingleLineCaseStatements, true)
  .setPreference(AlignSingleLineCaseStatements.MaxArrowIndent, 40)
  .setPreference(SpaceBeforeColon, false)
  .setPreference(SpaceInsideBrackets, false)
  .setPreference(SpaceInsideParentheses, false)
  .setPreference(PreserveDanglingCloseParenthesis, false)
  .setPreference(IndentSpaces, 2)
  .setPreference(IndentLocalDefs, false)
  .setPreference(SpacesWithinPatternBinders, true)
  .setPreference(SpacesAroundMultiImports, true)