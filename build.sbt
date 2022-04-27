lazy val root = project
  .in(file("."))
  .settings(
    scalaVersion := "2.13.8",
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio-test"     % "2.0.0-RC5" % Test,
      "dev.zio" %% "zio-test-sbt" % "2.0.0-RC5" % Test,
      "io.d11"  %% "zhttp"        % "2.0.0-RC7",
    ),
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    scalacOptions += "-Xsource:3",
  )
