package orchestrator;

public enum PipelineState {
    IDLE("Idle"),
    INGESTING("Ingest"),
    ANALYZING("Analysis"),
    PROCESSING_VISUALS("Visuals"),
    PROCESSING_AUDIO_TEXT("Audio/Text"),
    COMPLIANCE_CHECK("Compliance"),
    PACKAGING("Packaging"),
    COMPLETED("Completed"),
    FAILED("Failed");

    private final String displayName;

    PipelineState(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }


    public PipelineState next() {
        return switch (this) {
            case IDLE -> INGESTING;
            case INGESTING -> ANALYZING;
            case ANALYZING -> PROCESSING_VISUALS;
            case PROCESSING_VISUALS -> PROCESSING_AUDIO_TEXT;
            case PROCESSING_AUDIO_TEXT -> COMPLIANCE_CHECK;
            case COMPLIANCE_CHECK -> PACKAGING;
            case PACKAGING -> COMPLETED;
            default -> this;
        };
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }
}
