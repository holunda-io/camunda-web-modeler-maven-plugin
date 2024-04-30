package io.holunda.camunda.webmodeler.maven.core.application

import io.holunda.camunda.webmodeler.maven.core.domain.Model
import io.holunda.camunda.webmodeler.maven.core.port.`in`.DownloadModelsInPort
import io.holunda.camunda.webmodeler.maven.core.port.out.WebModelerOutPort
import java.nio.file.Path
import kotlin.io.path.createDirectories

class DownloadModelsUseCase(
    val webModelerOutPort: WebModelerOutPort
): DownloadModelsInPort {

    override fun downloadModel(model: Model, targetFolder: Path) {
        ensureFoldersExist(targetFolder)
        webModelerOutPort.download(model, targetFolder)
    }

    private fun ensureFoldersExist(target: Path) {
        target.createDirectories()
    }

}