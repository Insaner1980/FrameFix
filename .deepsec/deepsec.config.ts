import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { type DeepsecPlugin, defineConfig } from "deepsec/config";
import { androidExportedComponent } from "./matchers/android-exported-component.js";
import { androidUriShareWithoutClipData } from "./matchers/android-uri-share-without-clipdata.js";
import { fileproviderBroadPath } from "./matchers/fileprovider-broad-path.js";
import { mediaStoreImageWrite } from "./matchers/media-store-image-write.js";
import { sensitiveAndroidLog } from "./matchers/sensitive-android-log.js";

const here = path.dirname(fileURLToPath(import.meta.url));

function frameFixPlugin(): DeepsecPlugin {
  return {
    name: "framefix-android",
    matchers: [
      androidExportedComponent,
      fileproviderBroadPath,
      androidUriShareWithoutClipData,
      mediaStoreImageWrite,
      sensitiveAndroidLog,
    ],
  };
}

export default defineConfig({
  projects: [
    {
      id: "framefix",
      root: "..",
      infoMarkdown: fs.readFileSync(path.join(here, "data", "framefix", "INFO.md"), "utf-8"),
    },
  ],
  plugins: [frameFixPlugin()],
});
