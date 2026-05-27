import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { candidate, isTestFile } from "./utils.js";

export const mediaStoreImageWrite: MatcherPlugin = {
  slug: "media-store-image-write",
  description:
    "MediaStore image export paths that should handle IS_PENDING cleanup and failed writes",
  noiseTier: "normal",
  filePatterns: ["app/src/main/java/**/*.kt"],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];
    if (!content.includes("MediaStore.Images.Media.EXTERNAL_CONTENT_URI")) return [];

    const hasPending = content.includes("MediaStore.Images.Media.IS_PENDING");
    const hasDeleteCleanup = /\bdelete\s*\(|\bdeleteFailedExport\b/.test(content);
    if (hasPending && hasDeleteCleanup) return [];

    return [
      candidate(
        "media-store-image-write",
        content,
        content.indexOf("MediaStore.Images.Media.EXTERNAL_CONTENT_URI"),
        hasPending
          ? "MediaStore image export without failed-write cleanup"
          : "MediaStore image export without IS_PENDING lifecycle",
      ),
    ];
  },
};
