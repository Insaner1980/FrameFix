import type { CandidateMatch, MatcherPlugin } from "deepsec/config";
import { isTestFile, regexCandidates } from "./utils.js";

const sensitiveWords =
  "(?:image|bitmap|photo|picture|uri|mediastore|file|path|export|contentresolver|stream|jpeg|framefix)";

export const sensitiveAndroidLog: MatcherPlugin = {
  slug: "sensitive-android-log",
  description:
    "Android log statements that may disclose image URIs, file paths, MediaStore output, or image-processing state",
  noiseTier: "normal",
  filePatterns: ["app/src/main/java/**/*.kt"],
  match(content, filePath): CandidateMatch[] {
    if (isTestFile(filePath)) return [];

    return regexCandidates("sensitive-android-log", content, [
      {
        regex: new RegExp(
          String.raw`\b(?:Log|android\.util\.Log)\.(?:v|d|i|w|e)\s*\([^;\n]*${sensitiveWords}[^;\n]*\)`,
          "i",
        ),
        label: "Sensitive image or file term in Android log call",
      },
    ]);
  },
};
