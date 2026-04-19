import config.PipelineConfig;
import orchestrator.WorkflowOrchestrator;

import java.nio.file.Path;
import java.nio.file.Paths;


public class Main {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Usage: java com.pipeline.Main <input_video> [output_directory]");
            System.exit(1);
        }

        Path inputFile = Paths.get(args[0]);
        Path outputDir = args.length >= 2 ? Paths.get(args[1]) : Paths.get("output");

        PipelineConfig config = new PipelineConfig(inputFile, outputDir, "movie_101");

        System.out.println("==============================================");
        System.out.println("   Video Processing Pipeline");
        System.out.println("==============================================");
        System.out.println("Input:  " + config.getInputFile());
        System.out.println("Output: " + config.getBundlePath());
        System.out.println();

        WorkflowOrchestrator orchestrator = new WorkflowOrchestrator(config);
        boolean success = orchestrator.run();

        System.out.println();
        if (success) {
            System.out.println("[DONE] Pipeline completed successfully.");
            System.out.println("Bundle location: " + config.getBundlePath());
        } else {
            System.err.println("[FAIL] Pipeline failed. Check logs above.");
            System.exit(1);
        }
    }
}
