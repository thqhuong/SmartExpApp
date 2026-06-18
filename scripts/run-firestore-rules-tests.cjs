const fs = require("fs");
const path = require("path");
const { spawnSync } = require("child_process");

const env = { ...process.env };
const androidStudioJava = "C:\\Program Files\\Android\\Android Studio\\jbr\\bin";
const pathKey = Object.keys(env).find((key) => key.toLowerCase() === "path") || "PATH";

if (process.platform === "win32" && fs.existsSync(path.join(androidStudioJava, "java.exe"))) {
  env[pathKey] = `${androidStudioJava}${path.delimiter}${env[pathKey] || ""}`;
  env.JAVA_HOME = path.dirname(androidStudioJava);
}

const command = process.platform === "win32"
  ? "npx firebase emulators:exec --only firestore \"mocha test/firestore.rules.test.cjs\""
  : "npx firebase emulators:exec --only firestore 'mocha test/firestore.rules.test.cjs'";
const result = spawnSync(command, {
  stdio: "inherit",
  env,
  shell: true
});

if (result.error) {
  console.error(result.error);
  process.exit(1);
}

process.exit(result.status);
