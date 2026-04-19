package orchestrator;

import config.PipelineConfig;
import service.*;

import java.util.*;

public class WorkflowOrchestrator {
    private final PipelineConfig config;
    private final PipelineContext context;
    private final List<PipelineObserver> observers = new ArrayList<>();

    private PipelineState currentState = PipelineState.IDLE;

    private final Map<PipelineState, List<PipelineStage>> stageMap = new LinkedHashMap<>();

    public WorkflowOrchestrator(PipelineConfig config) {
        this.config = config;
        this.context = new PipelineContext(config);

        addObserver(new ConsoleLogger());
        registerStages();
    }

    private void registerStages() {

        stageMap.put(PipelineState.INGESTING, Arrays.asList(
                new IngestService.IntegrityCheck(),
                new IngestService.FormatValidator()
        ));

        stageMap.put(PipelineState.ANALYZING, Arrays.asList(
                new AnalysisService.IntroOutroDetector(),
                new AnalysisService.CreditRoller(),
                new AnalysisService.SceneIndexer()
        ));

        stageMap.put(PipelineState.PROCESSING_VISUALS, Arrays.asList(
                new VisualsService.SceneComplexityAnalyzer(),
                new VisualsService.Transcoder(),
                new VisualsService.SpriteGenerator()
        ));

        stageMap.put(PipelineState.PROCESSING_AUDIO_TEXT, Arrays.asList(
                new AudioTextService.SpeechToText(),
                new AudioTextService.Translator(),
                new AudioTextService.AIDubber()
        ));

        stageMap.put(PipelineState.COMPLIANCE_CHECK, Arrays.asList(
                new ComplianceService.SafetyScanner(),
                new ComplianceService.RegionalBranding()
        ));

        stageMap.put(PipelineState.PACKAGING, Arrays.asList(
                new PackagingService.DRMWrapper(),
                new PackagingService.ManifestBuilder()
        ));
    }

    public void addObserver(PipelineObserver observer) {
        observers.add(observer);
    }


    public boolean run() {
        while (!currentState.isTerminal()) {
            PipelineState nextState = currentState.next();
            transitionTo(nextState);

            List<PipelineStage> stages = stageMap.get(currentState);
            if (stages != null) {
                for (PipelineStage stage : stages) {
                    if (!executeStage(stage)) {
                        transitionTo(PipelineState.FAILED);
                        return false;
                    }
                }
            }
        }

        return currentState == PipelineState.COMPLETED;
    }


    private boolean executeStage(PipelineStage stage) {
        notifyStageStart(stage.getName());
        long startTime = System.currentTimeMillis();

        try {
            stage.execute(context);
            long elapsed = System.currentTimeMillis() - startTime;
            notifyStageComplete(stage.getName(), elapsed);
            return true;
        } catch (PipelineException e) {
            context.addError(e.getMessage());
            notifyStageError(stage.getName(), e.getMessage());
            return false;
        } catch (Exception e) {
            String msg = "Unexpected error: " + e.getMessage();
            context.addError(msg);
            notifyStageError(stage.getName(), msg);
            return false;
        }
    }


    private void transitionTo(PipelineState newState) {
        PipelineState old = currentState;
        currentState = newState;
        for (PipelineObserver o : observers) {
            o.onStateChange(old, newState);
        }
    }

    private void notifyStageStart(String name) {
        for (PipelineObserver o : observers) {
            o.onStageStart(name);
        }
    }

    private void notifyStageComplete(String name, long elapsedMs) {
        for (PipelineObserver o : observers) {
            o.onStageComplete(name, elapsedMs);
        }
    }

    private void notifyStageError(String name, String msg) {
        for (PipelineObserver o : observers) {
            o.onStageError(name, msg);
        }
    }

}
