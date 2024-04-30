package io.holunda.camunda.webmodeler.maven.core.port.`in`

import io.holunda.camunda.webmodeler.maven.core.domain.Model
import java.nio.file.Path

interface DownloadModelsInPort {

    fun downloadModel(model: Model, targetFolder: Path)

}