# FEAT-1006 Offline Map Data Manifest

## Confirmed Inputs for the Amended Plan

| App asset (source verified) | Actual format | Table | CRS | Geometry | Features | Source size | SHA-256 |
|---|---|---|---|---|---:|---:|---|
| `20260824_水務局既有側溝更新v3_線轉面.gpkg` | OGC GeoPackage (SQLite) | `line_buffer_polygon` | EPSG:3826 | Polygon | 36,970 | 19,615,744 bytes | `4b0df3e90dab556b3de0bd859511dee5a3af5d171255e4157e32b8d3f6e1ecaf` |
| `20260601_水務局未調查側溝_v5.gpkg` | OGC GeoPackage (SQLite) | `refactored` | EPSG:3826 | MultiPolygon | 61,435 | 60,661,760 bytes | `de68967c0fd607b1921a8bd3856d2a6d70e754d2bbb966beca89c82f6cc16177` |

The source files were read and verified during Planning, then copied to the FEAT-1006 worktree with SHA-256 confirmation. Combined raw input size is 80,277,504 bytes (about 76.6 MiB).

## Current Release Packaging Evidence

The rebuilt `app/build/outputs/apk/release/app-release-unsigned.apk` is 43,752,030 bytes. ZIP readback confirmed both exact file lengths and SHA-256 values:

| APK asset | Source bytes | APK bytes | SHA-256 readback |
|---|---:|---:|---|
| `20260824_水務局既有側溝更新v3_線轉面.gpkg` | 19,615,744 | 7,668,975 | PASS |
| `20260601_水務局未調查側溝_v5.gpkg` | 60,661,760 | 22,349,403 | PASS |

The obsolete `20261006_0601版可能側溝位置.geojson` APK entry is absent. This verifies package contents only; test-device runtime loading remains NOT VERIFIED.

## Historical Packaging Evidence — Superseded Source Pair

The earlier implementation packaged `20260824_水務局既有側溝更新v3_線轉面.gpkg` and `20261006_0601版可能側溝位置.geojson`. Its recorded release artifact was `app/build/outputs/apk/release/app-release-unsigned.apk`, 46,745,127 bytes (44.6 MiB); the old GPKG and GeoJSON APK entries were DEFLATE-compressed to 7,668,975 and 25,342,525 bytes and matched the then-current source hashes.

That artifact and its measurements are historical only. They do not verify packaging of the newly confirmed possible-gutter GPKG and must not be used as AC-001 evidence for the amended FEAT-1006 scope. Rebuild and inspect the APK after implementation.
