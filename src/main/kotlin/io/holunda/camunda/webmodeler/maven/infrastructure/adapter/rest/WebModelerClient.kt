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
        val fileId: String? = getFileId(model.name)
        if (fileId != null) {
            val downloadFile =
                model.milestone?.let { milestone -> getMilestoneFile(fileId, milestone) }
                    ?: run {
                        val dto: FileDto = client.getFile(UUID.fromString(fileId))
                        Objects.requireNonNull(dto.metadata, "metadata of file '$fileId' is null")
                        DownloadFile(dto.metadata.simplePath, dto.content)
                    }
            downloadFile(downloadFile, targetFolder)
        } else {
            log.warn("Could not find file for " + model.name)
        }

    }

    private fun getFileId(fileName: String): String? {
        val fileMetadataSearch = FileMetadataDto()
        fileMetadataSearch.name = fileName
        val fileSearchDto = PubSearchDtoFileMetadataDto()
        fileSearchDto.filter = fileMetadataSearch

        val fileSearchResponse: PubSearchResultDtoFileMetadataDto = client.searchFiles(fileSearchDto)
        if (fileSearchResponse.total != 1) {
            throw IllegalStateException("file search for $fileMetadataSearch returned ${fileSearchResponse.total} results, should be one")
        }
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
        Objects.requireNonNull(dto.metadata, "metadata of file '$fileId' and milestone '$milestoneName' is null")
        return DownloadFile(
            dto.metadata.simplePath,
            milestone.content
        )
    }

    private fun downloadFile(file: DownloadFile, targetFolder: Path) {
        Files.writeString(targetFolder.resolve(file.name), file.content)
        log.info("Saved '${file.name}'")
    }

}