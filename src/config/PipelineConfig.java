package config;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;


public class PipelineConfig {

    private final Path inputFile;
    private final Path outputDir;
    private final String movieId;

    private final List<String> resolutions = Arrays.asList("4k", "1080p", "720p");
    private final List<String> targetLanguages = Arrays.asList("ro");

    private final int thumbnailIntervalSeconds = 10;
    private final int spriteColumns = 5;

    private String expectedChecksum = null;

    public PipelineConfig(Path inputFile, Path outputDir, String movieId) {
        this.inputFile = inputFile;
        this.outputDir = outputDir;
        this.movieId = movieId;
    }

    public Path getBundlePath() {
        return outputDir.resolve(movieId);
    }

    public Path getVideoDir() {
        return getBundlePath().resolve("video");
    }

    public Path getImagesDir() {
        return getBundlePath().resolve("images");
    }

    public Path getThumbnailsDir() {
        return getImagesDir().resolve("thumbnails");
    }

    public Path getTextDir() {
        return getBundlePath().resolve("text");
    }

    public Path getAudioDir() {
        return getBundlePath().resolve("audio");
    }

    public Path getMetadataDir() {
        return getBundlePath().resolve("metadata");
    }

    public Path getInputFile() {
        return inputFile;
    }

    public String getMovieId() {
        return movieId;
    }

    public List<String> getTargetLanguages() {
        return targetLanguages;
    }

    public int getThumbnailIntervalSeconds() {
        return thumbnailIntervalSeconds;
    }

    public int getSpriteColumns() {
        return spriteColumns;
    }

    public String getExpectedChecksum() {
        return expectedChecksum;
    }

}
