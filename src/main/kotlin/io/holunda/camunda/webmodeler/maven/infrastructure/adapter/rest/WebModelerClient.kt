package io.holunda.camunda.webmodeler.maven.infrastructure.adapter.rest

import io.holunda.camunda.webmodeler.maven.core.domain.Model
import io.holunda.camunda.webmodeler.maven.core.port.out.WebModelerOutPort
import io.holunda.webmodeler.rest.CamundaWebModelerClientBuilder
import io.holunda.webmodeler.rest.impl.CamundaWebModelerClient
import org.apache.maven.plugin.logging.Log
import org.openapitools.client.model.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.createDirectories

class WebModelerClient(
    clientId: String,
    clientSecret: String,
    private val log: Log
) : WebModelerOutPort {

    private val client: CamundaWebModelerClient = CamundaWebModelerClientBuilder.builder()
        .clientId(clientId)
        .clientSecret(clientSecret)
        .build()

    override fun download(model: Model, targetFolder: Path) {
        val projectId = getProjectId(model.project)
        val fileId: String? = getFileId(model.name, projectId)
        if (fileId != null) {
            val downloadFile = if (model.milestone != null) {
                getMilestoneFile(fileId, model.milestone)
            } else {
                getFile(fileId)
            }
            saveFile(model, downloadFile, targetFolder)
        } else {
            log.warn("Could not find file for $model")
        }

    }

    private fun getProjectId(projectName: String?): String? {
        return projectName?.let {
            val projectSearchDto = PubSearchDtoProjectMetadataDto().apply {
                this.filter = ProjectMetadataDto().apply {
                    this.name = projectName
                }
            }

            val searchResult = client.searchProjects(projectSearchDto)
            require(searchResult.total == 1) { "project search for $projectSearchDto returned ${searchResult.total} results, should be one" }
            return searchResult.items.first().id
        }
    }

    private fun getFileId(fileName: String, projectId: String?): String? {
        val fileMetadataSearch = FileMetadataDto()
        fileMetadataSearch.name = fileName
        fileMetadataSearch.projectId = projectId
        val fileSearchDto = PubSearchDtoFileMetadataDto()
        fileSearchDto.filter = fileMetadataSearch

        val fileSearchResponse: PubSearchResultDtoFileMetadataDto = client.searchFiles(fileSearchDto)
        require(fileSearchResponse.total == 1) { "file search for $fileMetadataSearch returned ${fileSearchResponse.total} results, should be one" }
        return fileSearchResponse.items.first().id
    }

    private fun getMilestoneFile(fileId: String, milestoneName: String): DownloadFile {
        val mileStoneSearch = MilestoneMetadataDto()
        mileStoneSearch.name = milestoneName
        mileStoneSearch.fileId = fileId

        val pubSearchDtoMilestoneMetadataDto = PubSearchDtoMilestoneMetadataDto()
        pubSearchDtoMilestoneMetadataDto.filter = mileStoneSearch

        val mileStoneId: String = client.searchMilestones(pubSearchDtoMilestoneMetadataDto).items.first().id
        val milestone: MilestoneDto = client.getMilestone(UUID.fromString(mileStoneId))
        val dto: FileDto = client.getFile(UUID.fromString(fileId))
        requireNotNull(dto.metadata) { "metadata of file '$fileId' and milestone '$milestoneName' is null" }
        return DownloadFile(
            dto.metadata.simplePath,
            milestone.content
        )
    }

    private fun getFile(fileId: String): DownloadFile {
        val dto: FileDto = client.getFile(UUID.fromString(fileId))
        requireNotNull(dto.metadata) { "metadata of file '$fileId' is null" }
        return DownloadFile(dto.metadata.simplePath, dto.content)
    }

    private fun saveFile(model: Model, file: DownloadFile, targetFolder: Path) {
        val filename = model.targetPath ?: file.name
        val targetPath = targetFolder.resolve(filename)
        targetPath.parent.createDirectories()
        Files.writeString(targetPath, file.content)
        log.info("Saved '$filename'")
    }

}