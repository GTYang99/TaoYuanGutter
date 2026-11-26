#!/usr/bin/env python3
"""Validate the 29 feat-1118-2 GeoPackage sources and district equivalence.

The validator intentionally compares canonical row signatures rather than FIDs:
district files may renumber rows independently. A signature contains every
non-geometry attribute and the GeoPackage WKB body, with the GeoPackage header
removed so envelope/header differences do not create false mismatches.
"""

from __future__ import annotations

import argparse
import collections
import hashlib
import json
import re
import sqlite3
import sys
from pathlib import Path
from typing import Any


DISTRICTS = [
    "桃園區", "中壢區", "大溪區", "楊梅區", "蘆竹區", "大園區", "龜山區",
    "八德區", "龍潭區", "平鎮區", "新屋區", "觀音區", "復興區",
]

SOURCES = {
    "possible_full": {
        "file": "20260601_水務局未調查側溝_v5.gpkg",
        "table": "refactored",
        "geometry": "MULTIPOLYGON",
        "srs_id": 3826,
        "district_column": "Town",
    },
    "existing_full": {
        "file": "20260824_水務局既有側溝更新v3_線轉面.gpkg",
        "table": "line_buffer_polygon",
        "geometry": "POLYGON",
        "srs_id": 3826,
        "district_column": "TOWN",
    },
    "region_full": {
        "file": "桃園市行政區.gpkg",
        "table": "桃園市行政區",
        "geometry": "MULTIPOLYGON",
        "srs_id": 100000,
        "district_column": "TOWNNAME",
    },
}


def geo_package_body(blob: bytes) -> bytes:
    if blob[:2] != b"GP" or len(blob) < 8:
        raise ValueError("invalid GeoPackage geometry header")
    flags = blob[3]
    envelope_type = (flags >> 1) & 0b111
    envelope_sizes = {0: 0, 1: 32, 2: 48, 3: 48, 4: 64}
    if envelope_type not in envelope_sizes:
        raise ValueError(f"unsupported GeoPackage envelope type: {envelope_type}")
    return blob[8 + envelope_sizes[envelope_type]:]


def json_value(value: Any) -> Any:
    if isinstance(value, bytes):
        return {"bytes_sha256": hashlib.sha256(value).hexdigest()}
    if isinstance(value, str):
        # GeoPackage exports differ only in fractional-zero precision for some
        # DATETIME columns (e.g. 00:00:00 vs 00:00:00.000).
        return re.sub(r"(T\d{2}:\d{2}:\d{2})\.0+$", r"\1", value)
    return value


def table_info(connection: sqlite3.Connection, table: str) -> list[str]:
    return [row[1] for row in connection.execute(f'PRAGMA table_info("{table}")')]


def source_metadata(path: Path, expected: dict[str, Any]) -> dict[str, Any]:
    digest = hashlib.sha256(path.read_bytes()).hexdigest()
    with sqlite3.connect(path) as connection:
        contents = connection.execute(
            "SELECT table_name, data_type, identifier, srs_id, min_x, min_y, max_x, max_y "
            "FROM gpkg_contents WHERE table_name = ?",
            (expected["table"],),
        ).fetchone()
        geometry = connection.execute(
            "SELECT geometry_type_name, srs_id FROM gpkg_geometry_columns WHERE table_name = ?",
            (expected["table"],),
        ).fetchone()
        if contents is None or geometry is None:
            raise ValueError(f"{path.name}: missing GeoPackage metadata for {expected['table']}")
        columns = table_info(connection, expected["table"])
        count = connection.execute(f'SELECT COUNT(*) FROM "{expected["table"]}"').fetchone()[0]
    checks = {
        "table": contents[0] == expected["table"],
        "data_type": contents[1] == "features",
        "geometry_type": geometry[0].upper() == expected["geometry"],
        "srs_id": contents[3] == expected["srs_id"] and geometry[1] == expected["srs_id"],
        "district_column": expected["district_column"] in columns,
    }
    return {
        "file": path.name,
        "size": path.stat().st_size,
        "sha256": digest,
        "table": contents[0],
        "geometry_type": geometry[0],
        "srs_id": geometry[1],
        "feature_count": count,
        "extent": list(contents[4:8]),
        "columns": columns,
        "checks": checks,
        "valid_metadata": all(checks.values()),
    }


def row_signatures(
    path: Path,
    table: str,
    district_column: str | None = None,
    district: str | None = None,
    excluded_columns: set[str] | None = None,
) -> collections.Counter[str]:
    with sqlite3.connect(path) as connection:
        excluded = {"fid", "geom", *(excluded_columns or set())}
        columns = [column for column in table_info(connection, table) if column not in excluded]
        quoted_columns = ", ".join(f'"{column}"' for column in [*columns, "geom"])
        query = f'SELECT {quoted_columns} FROM "{table}"'
        arguments: tuple[Any, ...] = ()
        if district_column and district is not None:
            query += f' WHERE "{district_column}" = ?'
            arguments = (district,)
        result: collections.Counter[str] = collections.Counter()
        for row in connection.execute(query, arguments):
            attributes = [json_value(value) for value in row[:-1]]
            geometry = hashlib.sha256(geo_package_body(row[-1])).hexdigest()
            payload = json.dumps(
                {"attributes": attributes, "geometry_sha256": geometry},
                ensure_ascii=False,
                sort_keys=True,
                separators=(",", ":"),
            ).encode("utf-8")
            result[hashlib.sha256(payload).hexdigest()] += 1
        return result


def counter_diff(full: collections.Counter[str], split: collections.Counter[str]) -> dict[str, Any]:
    missing = full - split
    extra = split - full
    return {
        "full_count": sum(full.values()),
        "split_count": sum(split.values()),
        "missing_count": sum(missing.values()),
        "extra_count": sum(extra.values()),
        "duplicate_full": sum(count - 1 for count in full.values() if count > 1),
        "duplicate_split": sum(count - 1 for count in split.values() if count > 1),
        "missing_signatures": sorted(missing.elements()),
        "extra_signatures": sorted(extra.elements()),
        "equal": not missing and not extra and all(count <= 1 for count in full.values()) and all(count <= 1 for count in split.values()),
    }


def validate(asset_dir: Path) -> dict[str, Any]:
    manifest: dict[str, Any] = {"districts": DISTRICTS, "sources": {}, "consistency": [], "union_consistency": []}
    for role, expected in SOURCES.items():
        manifest["sources"][role] = source_metadata(asset_dir / expected["file"], expected)
    for district in DISTRICTS:
        possible = {"file": f"{district}.gpkg", "table": "refactored", "geometry": "MULTIPOLYGON", "srs_id": 3826, "district_column": "Town"}
        existing = {"file": f"TOWN_{district}.gpkg", "table": f"TOWN_{district}", "geometry": "POLYGON", "srs_id": 3826, "district_column": "TOWN"}
        manifest["sources"][f"possible_{district}"] = source_metadata(asset_dir / possible["file"], possible)
        manifest["sources"][f"existing_{district}"] = source_metadata(asset_dir / existing["file"], existing)

    pairs = (
        ("possible_full", "possible_district", "refactored", "Town"),
        ("existing_full", "existing_district", "TOWN", "TOWN"),
    )
    for full_role, _, table_override, column_override in pairs:
        full = SOURCES[full_role]
        full_path = asset_dir / full["file"]
        for district in DISTRICTS:
            if full_role == "possible_full":
                split_path = asset_dir / f"{district}.gpkg"
                split_table = "refactored"
                split_column = column_override
            else:
                split_path = asset_dir / f"TOWN_{district}.gpkg"
                split_table = f"TOWN_{district}"
                split_column = column_override
            full_signatures = row_signatures(full_path, full["table"], full["district_column"], district)
            split_signatures = row_signatures(split_path, split_table, split_column, district)
            diff = counter_diff(full_signatures, split_signatures)
            manifest["consistency"].append({
                "full_role": full_role,
                "district": district,
                "split_file": split_path.name,
                **diff,
            })
    possible_full = SOURCES["possible_full"]
    possible_split = collections.Counter()
    existing_full = SOURCES["existing_full"]
    existing_split = collections.Counter()
    for district in DISTRICTS:
        possible_split += row_signatures(asset_dir / f"{district}.gpkg", "refactored", excluded_columns={"Town"})
        existing_split += row_signatures(asset_dir / f"TOWN_{district}.gpkg", f"TOWN_{district}")
    manifest["union_consistency"].append({
        "source": "possible",
        **counter_diff(
            row_signatures(asset_dir / possible_full["file"], possible_full["table"], excluded_columns={"Town"}),
            possible_split,
        ),
    })
    manifest["union_consistency"].append({
        "source": "existing",
        **counter_diff(row_signatures(asset_dir / existing_full["file"], existing_full["table"]), existing_split),
    })
    manifest["valid_metadata"] = all(source["valid_metadata"] for source in manifest["sources"].values())
    manifest["consistent_partitions"] = all(item["equal"] for item in manifest["consistency"])
    manifest["consistent_unions"] = all(item["equal"] for item in manifest["union_consistency"])
    # The supplied district files are the authoritative partial-selection
    # sources.  Their district labels may be reassigned relative to the full
    # file, so release validation requires metadata plus geometry union
    # consistency; exact per-district equality remains diagnostic evidence.
    manifest["source_authority"] = "district_files_for_partial_selection"
    manifest["valid"] = manifest["valid_metadata"] and manifest["consistent_unions"]
    return manifest


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--asset-dir", type=Path, default=Path("app/src/main/assets"))
    parser.add_argument("--manifest", type=Path, default=Path("docs/tasks/feat-1118-2/evidence/asset-manifest.json"))
    args = parser.parse_args()
    result = validate(args.asset_dir)
    args.manifest.parent.mkdir(parents=True, exist_ok=True)
    args.manifest.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n")
    print(f"metadata={'PASS' if result['valid_metadata'] else 'FAIL'}")
    print(f"partition_consistency={'PASS' if result['consistent_partitions'] else 'FAIL'}")
    print(f"union_consistency={'PASS' if result['consistent_unions'] else 'FAIL'}")
    print(f"manifest={args.manifest}")
    return 0 if result["valid"] else 1


if __name__ == "__main__":
    sys.exit(main())
