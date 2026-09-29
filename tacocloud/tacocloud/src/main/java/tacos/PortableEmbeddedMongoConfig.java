package tacos;

import java.nio.file.Path;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.flapdoodle.embed.mongo.Command;
import de.flapdoodle.embed.mongo.config.Defaults;
import de.flapdoodle.embed.process.config.RuntimeConfig;
import de.flapdoodle.embed.process.config.store.DownloadConfig;
import de.flapdoodle.embed.process.extract.DirectoryAndExecutableNaming;
import de.flapdoodle.embed.process.extract.NoopTempNaming;
import de.flapdoodle.embed.process.io.directories.FixedPath;
import de.flapdoodle.embed.process.store.ExtractedArtifactStore;

@Configuration
public class PortableEmbeddedMongoConfig {

  @Bean
  RuntimeConfig embeddedMongoRuntimeConfig() {
    Path root = Path.of(System.getProperty("java.io.tmpdir"), "tacocloud", "embedded-mongo");
    DownloadConfig download = Defaults.downloadConfigFor(Command.MongoD)
        .artifactStorePath(new FixedPath(root.resolve("downloads").toString())).build();
    DirectoryAndExecutableNaming extraction = DirectoryAndExecutableNaming.builder()
        .directory(new FixedPath(root.resolve("extracted").toString()))
        .executableNaming(new NoopTempNaming()).build();
    ExtractedArtifactStore store = ExtractedArtifactStore.builder()
        .from(Defaults.extractedArtifactStoreFor(Command.MongoD))
        .downloadConfig(download).extraction(extraction).build();
    return Defaults.runtimeConfigFor(Command.MongoD).artifactStore(store).build();
  }
}
