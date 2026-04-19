package service;

import orchestrator.PipelineContext;
import orchestrator.PipelineException;
import orchestrator.PipelineStage;
import util.FileUtils;
import util.JsonBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ComplianceService {
    public static class SafetyScanner implements PipelineStage {

        @Override
        public String getName() {
            return "Safety Scanner";
        }

        @Override
        public void execute(PipelineContext context) throws PipelineException {
            Path metadataDir = context.getConfig().getMetadataDir();

            try {
                FileUtils.ensureDirectory(metadataDir);

                Map<String, Object> report = JsonBuilder.object();
                report.put("scan_status", "completed");
                report.put("scanner_version", "1.0.0-stub");
                report.put("content_rating", "PG-13");

                List<Object> flags = JsonBuilder.array();

                Map<String, Object> flag1 = JsonBuilder.object();
                flag1.put("region", "DE");
                flag1.put("type", "violence_index");
                flag1.put("severity", "low");
                flag1.put("action", "none_required");
                flag1.put("timestamp_start", 0.0);
                flag1.put("timestamp_end", 0.0);
                flags.add(flag1);

                report.put("flags", flags);
                report.put("blur_required", false);
                report.put("approved_regions", Arrays.asList("US", "EU", "RO", "UK", "JP"));

                Path reportFile = metadataDir.resolve("compliance_report.json");
                FileUtils.writeString(reportFile, JsonBuilder.toJson(report));
                context.addAsset("metadata/compliance_report.json");

                System.out.println("    Content rating: PG-13 (stub)");
                System.out.println("    No content flags requiring action");

            } catch (IOException e) {
                throw new PipelineException(getName(),
                        "Safety scan failed: " + e.getMessage(), e);
            }
        }
    }


    public static class RegionalBranding implements PipelineStage {

        @Override
        public String getName() {
            return "Regional Branding";
        }

        @Override
        public void execute(PipelineContext context) throws PipelineException {
            Path metadataDir = context.getConfig().getMetadataDir();

            try {
                FileUtils.ensureDirectory(metadataDir);

                Map<String, Object> branding = JsonBuilder.object();
                branding.put("branding_version", "1.0.0");

                List<Object> overlays = JsonBuilder.array();

                Map<String, Object> overlay1 = JsonBuilder.object();
                overlay1.put("region", "US");
                overlay1.put("logo", "studio_original_us.png");
                overlay1.put("position", "top-left");
                overlay1.put("display_duration_seconds", 5);
                overlays.add(overlay1);

                Map<String, Object> overlay2 = JsonBuilder.object();
                overlay2.put("region", "RO");
                overlay2.put("logo", "studio_original_ro.png");
                overlay2.put("position", "top-left");
                overlay2.put("display_duration_seconds", 5);
                overlays.add(overlay2);

                branding.put("overlays", overlays);

                Path brandingFile = metadataDir.resolve("branding.json");
                FileUtils.writeString(brandingFile, JsonBuilder.toJson(branding));
                context.addAsset("metadata/branding.json");

                System.out.printf("    Regional branding configured for %d regions%n",
                        overlays.size());

            } catch (IOException e) {
                throw new PipelineException(getName(),
                        "Regional branding failed: " + e.getMessage(), e);
            }
        }
    }
}
