package io.holunda.camunda.webmodeler.maven.core.port.out

import io.holunda.camunda.webmodeler.maven.core.domain.Model
import java.nio.file.Path

interface WebModelerOutPort {

    fun download(model: Model, targetFolder: Path)

}
