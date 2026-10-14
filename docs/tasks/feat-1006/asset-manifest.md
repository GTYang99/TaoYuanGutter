# FEAT-1006 Offline Map Data Manifest

## Confirmed Inputs for the Amended Plan

| App asset (planned) | Actual format | Table | CRS | Geometry | Features | Source size | SHA-256 |
|---|---|---|---|---|---:|---:|---|
| `20260824_水務局既有側溝更新v3_線轉面.gpkg` | OGC GeoPackage (SQLite) | `line_buffer_polygon` | EPSG:3826 | Polygon | 36,970 | 19,615,744 bytes | `4b0df3e90dab556b3de0bd859511dee5a3af5d171255e4157e32b8d3f6e1ecaf` |
| `20260601_水務局未調查側溝_v5.gpkg` | OGC GeoPackage (SQLite) | `refactored` | EPSG:3826 | MultiPolygon | 61,435 | 60,661,760 bytes | `de68967c0fd607b1921a8bd3856d2a6d70e754d2bbb966beca89c82f6cc16177` |
| `桃園市行政區.gpkg` | OGC GeoPackage (SQLite) | `桃園市行政區` | SRS id 100000 (`GCS_TWD97[2020]`) | MultiPolygon | 13 | 1,081,344 bytes | `aeed88ae15bc53956bbea3e7ebd9a3faf1bf29fc45a6bfaa4188b5333458c1bc` |

The source files were read and verified during Planning. Combined raw input size is 81,358,848 bytes (about 77.7 MiB). This table describes the confirmed inputs; implementation must still copy/package them and verify the resulting APK entries.

## Historical Packaging Evidence — Superseded Source Pair

The earlier implementation packaged `20260824_水務局既有側溝更新v3_線轉面.gpkg` and `20261006_0601版可能側溝位置.geojson`. Its recorded release artifact was `app/build/outputs/apk/release/app-release-unsigned.apk`, 46,745,127 bytes (44.6 MiB); the old GPKG and GeoJSON APK entries were DEFLATE-compressed to 7,668,975 and 25,342,525 bytes and matched the then-current source hashes.

That artifact and its measurements are historical only. They do not verify packaging of the newly confirmed possible-gutter GPKG and must not be used as AC-001 evidence for the amended FEAT-1006 scope. Rebuild and inspect the APK after implementation.
