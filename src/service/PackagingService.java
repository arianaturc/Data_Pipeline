package service;

import orchestrator.PipelineContext;
import orchestrator.PipelineException;
import orchestrator.PipelineStage;
import util.FileUtils;
import util.JsonBuilder;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PackagingService {
    public static class DRMWrapper implements PipelineStage {

        @Override
        public String getName() {
            return "DRM Wrapper";
        }

        @Override
        public void execute(PipelineContext context) throws PipelineException {
            Path metadataDir = context.getConfig().getMetadataDir();

            try {
                FileUtils.ensureDirectory(metadataDir);

                int encryptedCount = 0;
                List<Object> encryptedAssets = JsonBuilder.array();

                for (String asset : context.getGeneratedAssets()) {
                    if (asset.startsWith("video/") || asset.startsWith("audio/")) {
                        Map<String, Object> entry = JsonBuilder.object();
                        entry.put("asset", asset);
                        entry.put("drm_system", "Widevine-STUB");
                        entry.put("encryption", "AES-128-CTR");
                        entry.put("key_id", UUID.randomUUID().toString());
                        encryptedAssets.add(entry);
                        encryptedCount++;
                    }
                }

                Map<String, Object> drmReport = JsonBuilder.object();
                drmReport.put("drm_version", "1.0.0-stub");
                drmReport.put("encryption_applied", false);
                drmReport.put("note", "Stub implementation - no actual encryption performed");
                drmReport.put("assets_targeted", encryptedCount);
                drmReport.put("assets", encryptedAssets);

                Path drmFile = metadataDir.resolve("drm_report.json");
                FileUtils.writeString(drmFile, JsonBuilder.toJson(drmReport));
                context.addAsset("metadata/drm_report.json");

                System.out.printf("    DRM stub: %d assets targeted for encryption%n",
                        encryptedCount);

            } catch (IOException e) {
                throw new PipelineException(getName(),
                        "DRM wrapping failed: " + e.getMessage(), e);
            }
        }
    }


    public static class ManifestBuilder implements PipelineStage {

        @Override
        public String getName() {
            return "Manifest Builder";
        }

        @Override
        public void execute(PipelineContext context) throws PipelineException {
            Path metadataDir = context.getConfig().getMetadataDir();

            try {
                FileUtils.ensureDirectory(metadataDir);

                Map<String, Object> manifest = JsonBuilder.object();


                manifest.put("manifest_version", "1.0.0");
                manifest.put("movie_id", context.getConfig().getMovieId());
                manifest.put("generated_at", Instant.now().toString());
                manifest.put("pipeline_status", context.hasErrors() ? "completed_with_errors" : "success");


                Map<String, Object> source = JsonBuilder.object();
                source.put("checksum_sha256", context.getFileChecksum());
                source.put("duration_seconds", context.getVideoDuration());
                source.put("resolution", context.getVideoWidth() + "x" + context.getVideoHeight());
                source.put("codec", context.getVideoCodec());
                manifest.put("source", source);


                Map<String, Object> analysis = JsonBuilder.object();
                analysis.put("intro_end_seconds", context.getIntroEndTimestamp());
                analysis.put("credits_start_seconds", context.getCreditsStartTimestamp());
                analysis.put("scene_count", context.getSceneSegments().size());
                analysis.put("complexity_profile", context.getComplexityProfile());
                manifest.put("analysis", analysis);


                List<Object> videoAssets = JsonBuilder.array();
                for (String asset : context.getGeneratedAssets()) {
                    if (asset.startsWith("video/")) {
                        videoAssets.add(asset);
                    }
                }
                manifest.put("video_assets", videoAssets);

                List<Object> audioAssets = JsonBuilder.array();
                for (String asset : context.getGeneratedAssets()) {
                    if (asset.startsWith("audio/")) {
                        audioAssets.add(asset);
                    }
                }
                manifest.put("audio_assets", audioAssets);

                List<Object> textAssets = JsonBuilder.array();
                for (String asset : context.getGeneratedAssets()) {
                    if (asset.startsWith("text/")) {
                        textAssets.add(asset);
                    }
                }
                manifest.put("text_assets", textAssets);

                List<Object> imageAssets = JsonBuilder.array();
                for (String asset : context.getGeneratedAssets()) {
                    if (asset.startsWith("images/")) {
                        imageAssets.add(asset);
                    }
                }
                manifest.put("image_assets", imageAssets);

                List<Object> metaAssets = JsonBuilder.array();
                for (String asset : context.getGeneratedAssets()) {
                    if (asset.startsWith("metadata/")) {
                        metaAssets.add(asset);
                    }
                }
                manifest.put("metadata_assets", metaAssets);


                manifest.put("total_assets", context.getGeneratedAssets().size());


                if (context.hasErrors()) {
                    List<Object> errors = new ArrayList<>(context.getErrors());
                    manifest.put("errors", errors);
                }

                Path manifestFile = metadataDir.resolve("manifest.json");
                FileUtils.writeString(manifestFile, JsonBuilder.toJson(manifest));
                context.addAsset("metadata/manifest.json");

                System.out.printf("    Manifest written: %d total assets%n",
                        context.getGeneratedAssets().size());

            } catch (IOException e) {
                throw new PipelineException(getName(),
                        "Manifest generation failed: " + e.getMessage(), e);
            }
        }
    }
}
