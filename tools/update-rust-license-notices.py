#!/usr/bin/env python3

import json
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MANIFESTS = [ROOT / "crates/gallery-ffi/Cargo.toml"]
TEMPLATE = ROOT / "tools/rust-dependency-licenses.hbs"
OUTPUT = ROOT / "kotlin/shared-assets/licenses/RUST-DEPENDENCY-LICENSES.txt"
NOTICE_NAMES = {
    "notice",
    "notice.md",
    "notice.markdown",
    "notice.rst",
    "notice.txt",
    "copyright",
    "copyright.md",
    "copyright.txt",
}


def shipped_dependency_ids(metadata: dict) -> set[str]:
    root_package_ids = {
        package["id"]
        for package in metadata["packages"]
        if package["name"] == "gallery-ffi"
        and any(
            Path(package["manifest_path"]).resolve() == manifest.resolve()
            for manifest in MANIFESTS
        )
    }
    nodes = {node["id"]: node for node in metadata["resolve"]["nodes"]}
    reachable = set(root_package_ids)
    pending = list(root_package_ids)

    while pending:
        node = nodes[pending.pop()]
        for dependency in node["deps"]:
            if any(kind["kind"] is None for kind in dependency["dep_kinds"]):
                package_id = dependency["pkg"]
                if package_id not in reachable:
                    reachable.add(package_id)
                    pending.append(package_id)

    return {
        package_id
        for package_id in reachable
        if next(
            package for package in metadata["packages"] if package["id"] == package_id
        )["source"]
        is not None
    }


def append_package_notices(report: str, metadata: dict) -> str:
    package_by_id = {package["id"]: package for package in metadata["packages"]}
    notices = []

    for package_id in sorted(shipped_dependency_ids(metadata)):
        package = package_by_id[package_id]
        package_root = Path(package["manifest_path"]).resolve().parent
        for path in package_root.iterdir():
            if path.is_file() and path.name.casefold() in NOTICE_NAMES:
                notices.append((package["name"], package["version"], path))

    lines = [
        report.rstrip(),
        "",
        "Third-party NOTICE and copyright files",
        "=======================================",
        "",
    ]
    if not notices:
        lines.append("No separate NOTICE or copyright files were found.")
    else:
        for name, version, path in sorted(notices):
            text = path.read_text(encoding="utf-8")
            lines.extend(
                [
                    f"----- {name} {version}: {path.name} -----",
                    text.rstrip(),
                    "",
                ]
            )
    return "\n".join(lines) + "\n"


def main() -> None:
    generated = "\n\n".join(
        subprocess.run(
            [
                "cargo",
                "about",
                "generate",
                "--manifest-path",
                str(manifest),
                str(TEMPLATE),
            ],
            cwd=ROOT,
            check=True,
            text=True,
            stdout=subprocess.PIPE,
        ).stdout.rstrip()
        for manifest in MANIFESTS
    )
    metadata_json = subprocess.run(
        [
            "cargo",
            "metadata",
            "--format-version",
            "1",
            "--locked",
            "--manifest-path",
            str(ROOT / "Cargo.toml"),
        ],
        cwd=ROOT,
        check=True,
        text=True,
        stdout=subprocess.PIPE,
    ).stdout
    report = append_package_notices(generated, json.loads(metadata_json))
    report = "\n".join(line.rstrip() for line in report.splitlines()).rstrip() + "\n"
    temporary = OUTPUT.with_suffix(".tmp")
    try:
        temporary.write_text(report, encoding="utf-8")
        temporary.replace(OUTPUT)
    finally:
        temporary.unlink(missing_ok=True)
    print(f"Updated {OUTPUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
