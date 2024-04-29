
import io.holunda.webmodeler.rest.CamundaWebModelerClientBuilder
import io.holunda.webmodeler.rest.impl.CamundaWebModelerClient
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject
import org.openapitools.client.model.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.createDirectories

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


        log.info("Starting to download files")
        if (path == null) {
            path = project.basedir.toPath().resolve("/src/main/resources").toString()
        }
        log.info("Files will be stored under $path")


        documents.forEach { it ->
            val fileId: Optional<String> = getFileId(it.name)
            fileId.ifPresentOrElse(
                { id ->
                    val downloadFile: DownloadFile =
                        Optional.ofNullable(it.mileStone).map { milestone -> getMilestoneFile(id, milestone) }
                            .orElseGet {
                                val dto: FileDto = client!!.getFile(UUID.fromString(id))
                                Objects.requireNonNull(dto.metadata, "metadata of file '$id' is null")
                                DownloadFile(dto.metadata.simplePath, dto.content)
                            }
                    downloadFile(downloadFile)
                },
                { log.warn("Could not find file for " + it.name) }
            )
        }
    }

    private fun getFileId(fileName: String): Optional<String> {
        val fileMetadataSearch = FileMetadataDto()
        fileMetadataSearch.name = fileName
        val fileSearchDto = PubSearchDtoFileMetadataDto()
        fileSearchDto.filter = fileMetadataSearch

        val fileSearchResponse: PubSearchResultDtoFileMetadataDto = client!!.searchFiles(fileSearchDto)
        if (fileSearchResponse.total != 1) {
            throw IllegalStateException("file search for $fileMetadataSearch returned ${fileSearchResponse.total} results, should be one")
        }
        return Optional.ofNullable(fileSearchResponse.items)
            .map { items -> items.stream().findFirst().map(FileMetadataDto::getId) }
            .flatMap { file -> file }
    }

    private fun getMilestoneFile(fileId: String, milestoneName: String): DownloadFile {
        val mileStoneSearch = MilestoneMetadataDto()
        mileStoneSearch.name = milestoneName
        mileStoneSearch.fileId = fileId

        val pubSearchDtoMilestoneMetadataDto = PubSearchDtoMilestoneMetadataDto()
        pubSearchDtoMilestoneMetadataDto.filter = mileStoneSearch

        val mileStoneId: String = Optional.ofNullable(
            client!!.searchMilestones(
                pubSearchDtoMilestoneMetadataDto
            )
                .items
        ).map { res -> res.stream().findFirst() }.flatMap { ms -> ms }.map(
            MilestoneMetadataDto::getId
        ).orElseThrow()

        val milestone: MilestoneDto = client!!.getMilestone(UUID.fromString(mileStoneId))
        val dto: FileDto = client!!.getFile(UUID.fromString(fileId))
        Objects.requireNonNull(dto.metadata, "metadata of file '$fileId' and milestone '$milestoneName' is null")
        return DownloadFile(
            dto.metadata.simplePath,
            milestone.content
        )
    }

    private fun downloadFile(file: DownloadFile) {
        path?.let {
            Path.of(it).createDirectories()
            Files.writeString(Path.of(it, file.name), file.content)
            log.info("Created file " + file.name)
        } ?: throw IllegalArgumentException("path not set in plugin configuration")
    }
}


