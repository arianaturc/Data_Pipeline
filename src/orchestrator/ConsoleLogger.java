package orchestrator;

public class ConsoleLogger implements PipelineObserver {
    @Override
    public void onStateChange(PipelineState oldState, PipelineState newState) {
        System.out.printf("[STATE] %s -> %s%n", oldState.getDisplayName(), newState.getDisplayName());
    }

    @Override
    public void onStageStart(String stageName) {
        System.out.printf("[START] %s ...%n", stageName);
    }

    @Override
    public void onStageComplete(String stageName, long elapsedMs) {
        System.out.printf("[ OK ] %s (%.1fs)%n", stageName, elapsedMs / 1000.0);
    }

    @Override
    public void onStageError(String stageName, String errorMessage) {
        System.out.printf("[ERROR] %s: %s%n", stageName, errorMessage);
    }
}
