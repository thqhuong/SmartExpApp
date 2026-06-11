const fs = require('fs');
const path = require('path');
const readline = require('readline');

const logFile = 'C:\\Users\\ADMIN\\.gemini\\antigravity\\brain\\ca592191-c485-46f3-b7d7-6a0014fc8211\\.system_generated\\logs\\transcript.jsonl';
const outputDir = 'C:\\Users\\ADMIN\\.gemini\\antigravity\\brain\\37f53f17-4ced-4f71-8130-841b9f68b566\\scratch\\extracted_files';

if (!fs.existsSync(outputDir)) {
  fs.mkdirSync(outputDir, { recursive: true });
}

async function run() {
  const fileStream = fs.createReadStream(logFile);
  const rl = readline.createInterface({
    input: fileStream,
    crlfDelay: Infinity
  });

  const files = {};

  for await (const line of rl) {
    const data = JSON.parse(line);
    if (data.tool_calls) {
      for (const call of data.tool_calls) {
        if (call.name === 'write_to_file') {
          const args = typeof call.args === 'string' ? JSON.parse(call.args) : call.args;
          let target = args.TargetFile;
          let content = args.CodeContent;
          if (target && content) {
            target = target.replace(/["']/g, ''); // Clean quotes
            
            // Unescape if double escaped
            if (typeof content === 'string') {
              if (content.startsWith('"') && content.endsWith('"')) {
                try {
                  content = JSON.parse(content);
                } catch (e) {}
              } else {
                // If it's string, sometimes it contains literal \n and \"
                // We can parse it by wrapping in quotes and parsing if it looks escaped
                if (content.includes('\\n') || content.includes('\\"')) {
                  try {
                    content = JSON.parse(`"${content.replace(/"/g, '\\"')}"`);
                  } catch (e) {
                    // fall back to string replacement
                    content = content.replace(/\\n/g, '\n').replace(/\\"/g, '"').replace(/\\t/g, '\t');
                  }
                }
              }
            }
            
            const base = path.basename(target);
            files[base] = content;
          }
        }
      }
    }
  }

  for (const [name, content] of Object.entries(files)) {
    fs.writeFileSync(path.join(outputDir, name), content);
    console.log(`Extracted: ${name} (${content.length} bytes)`);
  }
}

run();
