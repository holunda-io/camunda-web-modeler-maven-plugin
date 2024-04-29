import io.holunda.webmodeler.rest.CamundaWebModelerClientBuilder
import io.holunda.webmodeler.rest.impl.CamundaWebModelerClient
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.openapitools.client.model.FileDto
import org.openapitools.client.model.FileMetadataDto
import org.openapitools.client.model.MilestoneDto
import org.openapitools.client.model.MilestoneMetadataDto
import org.openapitools.client.model.PubSearchDtoFileMetadataDto
import org.openapitools.client.model.PubSearchDtoMilestoneMetadataDto
import org.openapitools.client.model.PubSearchResultDtoFileMetadataDto
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Objects
import java.util.Optional
import java.util.UUID

@Mojo(name = "bpmn-download", defaultPhase = LifecyclePhase.COMPILE)
class BpmDownloadPlugin: AbstractMojo() {
    @Parameter(defaultValue = "\${project}", required = true, readonly = true)
    lateinit var project: MavenProject

    @Parameter(property = "client-id", readonly = true, required = true)
    lateinit var clientId: String

    @Parameter(property = "client-secret", readonly = true, required = true)
    lateinit var clientSecret: String

    @Parameter(property = "documents", readonly = true, required = true)
    lateinit var documents: List<ModelerDocument>

    @Parameter
    var path: String? = null

    var client: CamundaWebModelerClient? = null

    override fun execute() {
        client = CamundaWebModelerClientBuilder.builder()
            .clientId(clientId)
            .clientSecret(clientSecret)
            .build()


        getLog().info("Starting to download files")
        if (path == null) {
            path = project.basedir.toPath().resolve("/src/main/resources").toString()
        }
        getLog().info("Files will be stored under $path")


        documents.forEach { it ->
            val fileId: Optional<String> = getFileId(it.name)
            fileId.ifPresentOrElse(
                { id ->
                    val downloadFile: DownloadFile =
                        Optional.ofNullable(it.mileStone).map { milestone -> getMilestoneFile(id, milestone) }
                            .orElseGet {
                                val dto: FileDto = client!!.getFile(UUID.fromString(id))
                                Objects.requireNonNull(dto.getMetadata(), "metadata of file '$id' is null")
                                DownloadFile(dto.getMetadata().getSimplePath(), dto.getContent())
                            }
                    downloadFile(downloadFile)
                },
                { getLog().warn("Could not find file for " + it.name) }
            )
        }
    }

    private fun getFileId(fileName: String): Optional<String> {
        val fileMetadataSearch = FileMetadataDto()
        fileMetadataSearch.setName(fileName)
        val fileSearchDto = PubSearchDtoFileMetadataDto()
        fileSearchDto.setFilter(fileMetadataSearch)

        val fileSearchResponse: PubSearchResultDtoFileMetadataDto = client!!.searchFiles(fileSearchDto)
        if (fileSearchResponse.total != 1) {
            throw IllegalStateException("file search for $fileMetadataSearch returned ${fileSearchResponse.total} results, should be one")
        }
        return Optional.ofNullable(fileSearchResponse.getItems())
            .map { items -> items.stream().findFirst().map(FileMetadataDto::getId) }
            .flatMap { file -> file }
    }

    private fun getMilestoneFile(fileId: String, milestoneName: String): DownloadFile {
        val mileStoneSearch = MilestoneMetadataDto()
        mileStoneSearch.setName(milestoneName)
        mileStoneSearch.setFileId(fileId)

        val pubSearchDtoMilestoneMetadataDto = PubSearchDtoMilestoneMetadataDto()
        pubSearchDtoMilestoneMetadataDto.setFilter(mileStoneSearch)

        val mileStoneId: String = Optional.ofNullable(
            client!!.searchMilestones(
                pubSearchDtoMilestoneMetadataDto
            )
                .getItems()
        ).map { res -> res.stream().findFirst() }.flatMap { ms -> ms }.map(
            MilestoneMetadataDto::getId
        ).orElseThrow()

        val milestone: MilestoneDto = client!!.getMilestone(UUID.fromString(mileStoneId))
        val dto: FileDto = client!!.getFile(UUID.fromString(fileId))
        Objects.requireNonNull(dto.getMetadata(), "metadata of file '$fileId' and milestone '$milestoneName' is null")
        return DownloadFile(
            dto.getMetadata().getSimplePath(),
            milestone.getContent()
        )
    }

    private fun downloadFile(file: DownloadFile) {
        path?.let {
            Files.writeString(Path.of(it, file.name), file.content)
            getLog().info("Created file " + file.name)
        } ?: throw IllegalArgumentException("path not set in plugin configuration")
    }
}


