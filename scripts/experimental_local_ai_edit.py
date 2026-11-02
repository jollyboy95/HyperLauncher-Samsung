#!/usr/bin/env python3
"""Conservative local-model coding experiment. One allowlisted file per run."""
import json
import os
import pathlib
import re
import subprocess
import sys
import urllib.request

def git(*args, check=True):
    return subprocess.run(["git", *args], text=True, capture_output=True, check=check)

def main():
    task = os.environ["AI_TASK"].strip()
    path = os.environ["AI_TARGET_FILE"].strip()
    if not task or len(task) > 2500:
        sys.exit("Task must contain 1-2500 characters.")
    if not path or path.startswith(("/", ".")) or ".." in pathlib.PurePosixPath(path).parts:
        sys.exit("Invalid target path.")
    if not path.startswith("compat/src/main/java/au/steve/hyperlauncher/compat/") or not re.search(r"(Aurora|Lock|Clock|Notification|Wallpaper|Depth|Keyguard)", pathlib.PurePosixPath(path).name, re.IGNORECASE):
        sys.exit("Only named lock-screen Java source files under compat/src/main are allowed.")
    target = pathlib.Path(path)
    if not target.is_file() or target.is_symlink():
        sys.exit("Target must be an existing regular file.")
    if target.suffix not in (".kt", ".java", ".xml", ".md", ".txt", ".json"):
        sys.exit("Target type is not allowlisted.")
    if any(part in (".github", "gradle", "build") for part in target.parts):
        sys.exit("Workflow/build configuration files are not allowed.")
    original = target.read_text(encoding="utf-8")
    if len(original) > 12000:
        sys.exit("File too large for the experimental model.")
    prompt = (
        "You are a careful Android lock-screen developer. Work ONLY on lock-screen behavior, not launcher, home screen, app drawer or widgets. Return ONLY the full updated contents of the "
        "single file below. Do not use markdown fences or commentary. Preserve all unrelated "
        "content. Do not add secrets, network calls or destructive commands. "
        "If you cannot make the change safely, return the original file unchanged.\n\n"
        f"Task: {task}\nFile: {path}\n\nOriginal contents:\n{original}"
    )
    payload = json.dumps({
        "model": "qwen2.5-coder:1.5b",
        "prompt": prompt,
        "stream": False,
        "options": {"num_ctx": 8192, "num_predict": 2400, "temperature": 0.1},
    }).encode()
    request = urllib.request.Request(
        "http://127.0.0.1:11434/api/generate", data=payload,
        headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(request, timeout=600) as response:
        result = json.load(response)
    answer = result.get("response", "").strip()
    answer = re.sub(r"^\x60\x60\x60[^\n]*\n", "", answer)
    answer = re.sub(r"\n\x60\x60\x60\s*$", "", answer)
    if not answer or answer == original.strip():
        sys.exit("Model produced no change.")
    if len(answer) > max(20000, len(original) * 2):
        sys.exit("Model output unexpectedly large.")
    target.write_text(answer + "\n", encoding="utf-8")
    changed = git("diff", "--name-only").stdout.splitlines()
    if changed != [path]:
        git("restore", "--", path)
        sys.exit("Unexpected files changed.")
    diff = git("diff", "--check", check=False)
    if diff.returncode:
        git("restore", "--", path)
        sys.exit("Generated change has whitespace errors: " + diff.stdout)
    if not git("diff", "--quiet", "--", path, check=False).returncode:
        print("Validated candidate edit:", path)
    else:
        sys.exit("No changes.")
    with open(os.environ["GITHUB_STEP_SUMMARY"], "a") as f:
        f.write("## AI proposed change\n\n")
        f.write(f"Target: \x60{path}\x60\n\n")
        f.write("Review the generated diff carefully. A successful run does not imply correct code.\n")

if __name__ == "__main__":
    main()
