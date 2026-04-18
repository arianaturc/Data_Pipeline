package orchestrator;

public interface PipelineStage {
    String getName();
    void execute(PipelineContext context) throws PipelineException;
}
