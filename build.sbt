import org.openurp.parent.Dependencies.*
import org.openurp.parent.Settings.*

ThisBuild / organization := "net.openurp.sues"
ThisBuild / version := "0.0.1-SNAPSHOT"

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/urp-school/sues-room"),
    "scm:git@github.com:urp-school/sues-room.git"
  )
)

ThisBuild / developers := List(
  Developer(
    id = "chaostone",
    name = "Tihua Duan",
    email = "duantihua@gmail.com",
    url = url("http://github.com/duantihua")
  )
)

ThisBuild / description := "The Sues Room Sync Utility"
ThisBuild / homepage := Some(url("https://beangle.github.io/urp-school/sues-room.html"))

val apiVer = "0.41.9"
val starterVer = "0.3.43"
val baseVer = "0.4.41"

lazy val root = (project in file("."))
  .enablePlugins(WarPlugin, TomcatPlugin)
  .settings(
    name := "sues-edu-roomsync",
    common,
    libraryDependencies ++= Seq(beangle_commons, beangle_ems_app, beangle_webmvc, beangle_serializer),
    libraryDependencies ++= Seq(beangle_model, beangle_cdi),
    libraryDependencies ++= Seq(spring_context, spring_beans)
  )
