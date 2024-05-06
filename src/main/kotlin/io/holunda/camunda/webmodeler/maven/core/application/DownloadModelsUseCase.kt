package io.holunda.camunda.webmodeler.maven.core.application

import io.holunda.camunda.webmodeler.maven.core.domain.Model
import io.holunda.camunda.webmodeler.maven.core.port.`in`.DownloadModelsInPort
import io.holunda.camunda.webmodeler.maven.core.port.out.WebModelerOutPort
import java.nio.file.Path

class DownloadModelsUseCase(
    val webModelerOutPort: WebModelerOutPort
): DownloadModelsInPort {

    override fun downloadModel(model: Model, targetFolder: Path) {
        webModelerOutPort.download(model, targetFolder)
    }

}