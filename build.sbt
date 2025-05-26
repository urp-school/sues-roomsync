import org.openurp.parent.Dependencies.*
import org.openurp.parent.Settings.*

ThisBuild / organization := "net.openurp.sues"
ThisBuild / version := "0.0.1-SNAPSHOT"

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/urp-school/sues-edu-ws"),
    "scm:git@github.com:urp-school/sues-edu-ws.git"
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

ThisBuild / description := "Openurp Sues Edu WebService"
ThisBuild / homepage := Some(url("https://beangle.github.io/urp-school/sues-edu-ws.html"))

lazy val root = (project in file("."))
  .enablePlugins(WarPlugin, TomcatPlugin)
  .settings(
    name := "sues-edu-ws",
    common,
    libraryDependencies ++= Seq(beangle_commons, beangle_ems_app, beangle_webmvc, beangle_serializer),
    libraryDependencies ++= Seq(beangle_model, beangle_cdi),
    libraryDependencies ++= Seq(spring_context, spring_beans)
  )
