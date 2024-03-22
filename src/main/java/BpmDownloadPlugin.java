import io.holunda.tasklist.rest.CamundaWebModelerClientBuilder;
import io.holunda.tasklist.rest.impl.CamundaWebModelerClient;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.openapitools.client.model.FileDto;
import org.openapitools.client.model.FileMetadataDto;
import org.openapitools.client.model.MilestoneDto;
import org.openapitools.client.model.MilestoneMetadataDto;
import org.openapitools.client.model.PubSearchDtoFileMetadataDto;
import org.openapitools.client.model.PubSearchDtoMilestoneMetadataDto;
import org.openapitools.client.model.PubSearchResultDtoFileMetadataDto;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Mojo(name = "bpmn-download", defaultPhase = LifecyclePhase.COMPILE)
public class BpmDownloadPlugin extends AbstractMojo {

    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;
    @Parameter
    private String path;

    @Parameter(property = "client-id", readonly = true, required = true)
    private String clientId;
    @Parameter(property = "client-secret", readonly = true, required = true)
    private String clientSecret;

    @Parameter(property = "documents", readonly = true, required = true)
    private List<ModelerDocument> documents;

    private final CamundaWebModelerClient client;

    public BpmDownloadPlugin() {
        this.client = CamundaWebModelerClientBuilder.builder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
        ;
    }


    @Override
    public void execute() {
        getLog().info("Starting to download files");
        if (path == null) {
            path = project.getBasedir() + "/src/main/resources";
        }
        getLog().info("Files will be stored under " + path);


        documents.forEach(it -> {
            Optional<String> fileId = getFileId(it.name);
            fileId.ifPresentOrElse(
                    id -> {
                        DownloadFile downloadFile = Optional.ofNullable(it.mileStone).map(
                                milestone -> getMilestoneFile(id, milestone)
                        ).orElseGet(() -> {
                            FileDto dto = client.getFile(UUID.fromString(id));
                            return new DownloadFile(dto.getMetadata().getSimplePath(), dto.getContent());
                        });
                        downloadFile(downloadFile);
                    },
                    () -> getLog().warn("Could not find file for " + it.name)
            );
        });
    }

    private Optional<String> getFileId(String fileName) {
        PubSearchDtoFileMetadataDto fileSearchDto = new PubSearchDtoFileMetadataDto();
        FileMetadataDto fileMetadataSearch = new FileMetadataDto();
        fileMetadataSearch.setName(fileName);
        fileSearchDto.setFilter(fileMetadataSearch);
        PubSearchResultDtoFileMetadataDto fileSearchResponse = client.searchFiles(fileSearchDto);
        return Optional.ofNullable(fileSearchResponse.getItems()).map(
                items -> items.stream().findFirst().map(FileMetadataDto::getId)).flatMap(file -> file);
    }

    private DownloadFile getMilestoneFile(String fileId, String milestone) {
        PubSearchDtoMilestoneMetadataDto pubSearchDtoMilestoneMetadataDto = new PubSearchDtoMilestoneMetadataDto();
        MilestoneMetadataDto mileStoneSearch = new MilestoneMetadataDto();
        mileStoneSearch.setName(milestone);
        mileStoneSearch.setFileId(fileId);
        pubSearchDtoMilestoneMetadataDto.setFilter(mileStoneSearch);
        String mileStoneId = Optional.ofNullable(
                client.searchMilestones(
                                pubSearchDtoMilestoneMetadataDto)
                        .getItems()).map(res -> res.stream().findFirst()).flatMap(
                ms -> ms).map(
                MilestoneMetadataDto::getId).orElseThrow();

        MilestoneDto milestone1 = client.getMilestone(UUID.fromString(mileStoneId));
        FileDto dto = client.getFile(UUID.fromString(fileId));
        return new DownloadFile(dto.getMetadata().getSimplePath(),
                milestone1.getContent());
    }

    private void downloadFile(DownloadFile file) {
        try {
            Files.writeString(Path.of(path + "/" + file.name), file.content);
            getLog().info("Created file " + file.name);
        } catch (IOException e) {
            getLog().warn("Could not save file for " + file.name);
            throw new RuntimeException(e);
        }
    }
}


