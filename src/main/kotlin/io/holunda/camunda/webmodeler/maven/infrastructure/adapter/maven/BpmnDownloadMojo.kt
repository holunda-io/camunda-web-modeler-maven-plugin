package io.holunda.camunda.webmodeler.maven.infrastructure.adapter.maven

import io.holunda.camunda.webmodeler.maven.core.application.DownloadModelsUseCase
import io.holunda.camunda.webmodeler.maven.core.domain.Model
import io.holunda.camunda.webmodeler.maven.core.port.`in`.DownloadModelsInPort
import io.holunda.camunda.webmodeler.maven.core.port.out.WebModelerOutPort
import io.holunda.camunda.webmodeler.maven.infrastructure.adapter.rest.WebModelerClient
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import java.nio.file.Path

@Mojo(name = "bpmn-download", defaultPhase = LifecyclePhase.COMPILE)
class BpmnDownloadMojo: AbstractMojo() {
    @Parameter(defaultValue = "\${project}", required = true, readonly = true)
    lateinit var project: MavenProject

    @Parameter(property = "client-id", readonly = true, required = true)
    lateinit var clientId: String

    @Parameter(property = "client-secret", readonly = true, required = true)
    lateinit var clientSecret: String

    @Parameter(property = "documents", readonly = true, required = true)
    lateinit var documents: List<ModelerDocument>

    @Parameter(defaultValue = "\${project.basedir}/src/main/resources")
    lateinit var path: String

    override fun execute() {
        val webModelerOutPort: WebModelerOutPort = WebModelerClient(clientId, clientSecret, log)
        val downloadModelsInPort: DownloadModelsInPort = DownloadModelsUseCase(webModelerOutPort)
        val targetFolder = Path.of(path)

        log.info("Starting to download files. They will be stored under '$targetFolder'")

        documents.map { Model(it.name, it.mileStone, it.project) }
            .forEach { downloadModelsInPort.downloadModel(it, targetFolder) }
    }

}


