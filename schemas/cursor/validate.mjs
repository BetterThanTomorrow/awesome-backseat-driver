#!/usr/bin/env node
import Ajv from "ajv";
import addFormats from "ajv-formats";
import { readFileSync } from "node:fs";
import { resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));

function usage() {
  console.error("Usage: node validate.mjs -s <schema.json> -d <data.json>");
  process.exit(2);
}

function parseArgs(argv) {
  const args = { schema: null, data: null };
  for (let i = 2; i < argv.length; i++) {
    if (argv[i] === "-s") args.schema = argv[++i];
    else if (argv[i] === "-d") args.data = argv[++i];
  }
  if (!args.schema || !args.data) usage();
  return args;
}

export function validateJson(schemaPath, dataPath) {
  const ajv = new Ajv({ allErrors: true, strict: false });
  addFormats(ajv);
  const schema = JSON.parse(readFileSync(resolve(schemaPath), "utf8"));
  const data = JSON.parse(readFileSync(resolve(dataPath), "utf8"));
  const validate = ajv.compile(schema);
  if (validate(data)) return [];
  return validate.errors ?? [{ message: "unknown validation error" }];
}

if (process.argv[1] && resolve(process.argv[1]) === resolve(fileURLToPath(import.meta.url))) {
  const { schema, data } = parseArgs(process.argv);
  const errors = validateJson(schema, data);
  if (errors.length === 0) {
    console.log("OK");
    process.exit(0);
  }
  for (const e of errors) {
    console.error(`${e.instancePath || "/"}: ${e.message}`);
  }
  process.exit(1);
}
