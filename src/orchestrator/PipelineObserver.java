package orchestrator;

public interface PipelineObserver {
    void onStateChange(PipelineState oldState, PipelineState newState);
    void onStageStart(String stageName);
    void onStageComplete(String stageName, long elapsedMs);
    void onStageError(String stageName, String errorMessage);
}
