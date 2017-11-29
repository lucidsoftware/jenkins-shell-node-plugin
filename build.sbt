import java.io.IOException
import org.apache.http.client.methods.{HttpHead, HttpPost}
import org.apache.http.entity.mime.MultipartEntityBuilder
import org.apache.http.impl.client.{DefaultHttpRequestRetryHandler, DefaultServiceUnavailableRetryStrategy, HttpClients}
import org.apache.http.protocol.HttpContext
import resource.managed
import scala.concurrent.duration._
import scala.collection.JavaConverters._

val Hpi = config("hpi").extend(Compile)

val install = taskKey[Unit]("Install")

val jenkinsVersion = settingKey[String]("Jenkins version")

val serverUrl = TaskKey[URI]("server-url")

enablePlugins(SbtProguard)

inConfig(Hpi)(
  Defaults.packageTaskSettings(packageBin, mappings in packageBin) ++
  inTask(packageBin)(Seq(
    artifact := artifact.value.withClassifier(None).withExtension("hpi"),
    packageOptions := {
      val attributes = Seq(
        "Jenkins-Version" -> jenkinsVersion.value,
        "Long-Name" -> "Shell Cloud",
        "Short-Name" -> name.value,
        "Specification-Title" -> (description in Hpi).value,
        //"Plugin-Version" -> version.value
      )
      Seq(
        Package.ManifestAttributes(attributes ++ homepage.value.map("Url" -> _.toString): _*),
        Package.addSpecManifestAttributes(name.value, version.value, organization.value),
        Package.addImplManifestAttributes(name.value, version.value, homepage.value, organization.value, organizationName.value)
      )
    }
  ))
)

compileOrder := CompileOrder.ScalaThenJava

crossPaths := false

install in Hpi := {
  val file = (packageBin in Hpi).value
  val log = streams.value.log
  val baseUrl = (serverUrl in (Hpi, install)).value
  for (client <- managed(HttpClients.createDefault)) {
    val uploadRequest = {
      val request = new HttpPost(s"$baseUrl/pluginManager/uploadPlugin")
      request.setEntity(MultipartEntityBuilder.create.addBinaryBody("name", file).build)
      request
    }
    log.info(s"Uploading $file to ${uploadRequest.getURI}")
    val uploadStatus = managed(client.execute(uploadRequest)).acquireAndGet(_.getStatusLine.getStatusCode)
    require(200 <= uploadStatus && uploadStatus < 400)

    val restartRequest = new HttpPost(s"$baseUrl/updateCenter/safeRestart")
    log.info(s"Restarting Jenkins at ${restartRequest.getURI}")
    Thread.sleep(2.seconds.toMillis)
    val restartStatus = managed(client.execute(restartRequest)).acquireAndGet(_.getStatusLine.getStatusCode)
    require(200 <= restartStatus && restartStatus < 400)
  }

  val retryClientBuilder = HttpClients.custom
    .setRetryHandler(new DefaultHttpRequestRetryHandler(10, true, asJavaCollection(Iterable.empty)) {
      override def retryRequest(exception: IOException, executionCount: Int, context: HttpContext) = {
        val retry = super.retryRequest(exception, executionCount, context)
        if (retry) {
          Thread.sleep(1500)
        }
        retry
      }
    })
    .setServiceUnavailableRetryStrategy(new DefaultServiceUnavailableRetryStrategy(20, 1500))
  Thread.sleep(3000)
  log.info("Waiting for Jenkins to be ready")
  for (client <- managed(retryClientBuilder.build)) {
    val request = new HttpHead(baseUrl)
    val status = managed(client.execute(request)).acquireAndGet(_.getStatusLine.getStatusCode)
    assert(200 <= status && status < 400)
  }
}

description in Hpi := "Custom node provisioning using system commands"

jenkinsVersion := "1.651.2"

libraryDependencies ++= Seq(
  "com.jsuereth" %% "scala-arm" % "2.0",
  "javax.servlet" % "javax.servlet-api" % "3.1.0" % Provided,
  "javax.transaction" % "transaction-api" % "1.1" % Provided,
  "org.jenkins-ci.main" % "jenkins-core" % jenkinsVersion.value % Provided
)

logLevel in (Proguard, proguard) := Level.Debug

if (sys.props.contains("proguard")) {
  mappings in (Hpi, packageBin) := (proguard in Proguard).value.map { file =>
    file -> s"WEB-INF/lib/${file.name}"
  }
} else {
  mappings in (Hpi, packageBin) := (fullClasspathAsJars in Runtime).value.map { file =>
    val artifact = file.get(Keys.artifact.key).get
    file.data -> s"WEB-INF/lib/${artifact.name}.${artifact.extension}"
  }
}

name := "jenkins-shell-cloud-plugin"

name in Hpi := "shell-cloud"

organization := "com.lucidchart"

organizationName := "Lucid Software"

inConfig(Proguard)(Seq(
  javaOptions in proguard := Seq("-Xmx512m", "-Xss100m"),
  proguardBinaryDeps ++= update.value.select(moduleFilter("javax.transaction", "transaction-api")),
  proguardOptions :=
    ProguardOptions.jarOptions("-libraryjars", proguardFilteredLibraries.value) ++
    proguardFilteredInputs.value.zip(proguardFilteredOutputs.value).flatMap { case (input, output) =>
      ProguardOptions.jarOptions("-injars", Seq(input)) ++ ProguardOptions.jarOptions("-outjars", Seq(output))
    },
  proguardOptions ++= IO.readLines(baseDirectory.value / "config.pro"),
  proguardOutputs := proguardFilteredInputs.value.map { input =>
    val outputName = if (input.file == (classDirectory in Compile).value) s"${artifact.value.name}.${artifact.value.extension}" else input.file.name
    proguardDirectory.value / outputName
  }
))

homepage := Some(url("https://github.com/lucidsoftware/jenkins-shell-cloud-plugin"))

resolvers += "Jenkins public repository" at "https://repo.jenkins-ci.org/public/"

scalaVersion := "2.12.4"

scalafmtOnCompile := true

scalacOptions += "-Yresolve-term-conflict:object"

serverUrl in (Hpi, install) := new URI("http://localhost:8080")
