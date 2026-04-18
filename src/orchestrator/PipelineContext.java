package orchestrator;

import config.PipelineConfig;

import java.util.*;

public class PipelineContext {

    private final PipelineConfig config;

    private String fileChecksum;
    private double videoDuration;
    private int videoWidth;
    private int videoHeight;
    private String videoCodec;
    private double introEndTimestamp;
    private double creditsStartTimestamp;

    private List<Map<String, Object>> sceneSegments = new ArrayList<>();

    private double averageComplexity;
    private String complexityProfile;

    private final List<String> generatedAssets = new ArrayList<>();

    private final List<String> errors = new ArrayList<>();

    public PipelineContext(PipelineConfig config) {
        this.config = config;
    }

    public PipelineConfig getConfig() {
        return config;
    }

    public void addAsset(String relativePath) {
        generatedAssets.add(relativePath);
    }

    public List<String> getGeneratedAssets() {
        return Collections.unmodifiableList(generatedAssets);
    }

    public void addError(String error) {
        errors.add(error);
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public String getFileChecksum() {
        return fileChecksum;
    }

    public void setFileChecksum(String fileChecksum) {
        this.fileChecksum = fileChecksum;
    }

    public double getVideoDuration() {
        return videoDuration;
    }

    public void setVideoDuration(double videoDuration) {
        this.videoDuration = videoDuration;
    }

    public int getVideoWidth() {
        return videoWidth;
    }

    public void setVideoWidth(int videoWidth) {
        this.videoWidth = videoWidth;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    public void setVideoHeight(int videoHeight) {
        this.videoHeight = videoHeight;
    }

    public String getVideoCodec() {
        return videoCodec;
    }

    public void setVideoCodec(String videoCodec) {
        this.videoCodec = videoCodec;
    }

    public double getIntroEndTimestamp() {
        return introEndTimestamp;
    }

    public void setIntroEndTimestamp(double introEndTimestamp) {
        this.introEndTimestamp = introEndTimestamp;
    }

    public double getCreditsStartTimestamp() {
        return creditsStartTimestamp;
    }

    public void setCreditsStartTimestamp(double creditsStartTimestamp) {
        this.creditsStartTimestamp = creditsStartTimestamp;
    }

    public List<Map<String, Object>> getSceneSegments() {
        return sceneSegments;
    }

    public void setSceneSegments(List<Map<String, Object>> sceneSegments) {
        this.sceneSegments = sceneSegments;
    }

    public void setAverageComplexity(double averageComplexity) {
        this.averageComplexity = averageComplexity;
    }

    public String getComplexityProfile() {
        return complexityProfile;
    }

    public void setComplexityProfile(String complexityProfile) {
        this.complexityProfile = complexityProfile;
    }
}
