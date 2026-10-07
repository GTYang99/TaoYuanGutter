# FEAT-1006 Offline Map Data Manifest

| App asset | Actual format | CRS | Geometry | Features | Source size | SHA-256 |
|---|---|---|---|---:|---:|---|
| `20260824_水務局既有側溝更新v3_線轉面.gpkg` | OGC GeoPackage (SQLite) | EPSG:3826 | Polygon | 36,970 | 19,615,744 bytes | `4b0df3e90dab556b3de0bd859511dee5a3af5d171255e4157e32b8d3f6e1ecaf` |
| `20261006_0601版可能側溝位置.geojson` | GeoJSON FeatureCollection | EPSG:3826 (`urn:ogc:def:crs:EPSG::3826`) | MultiPolygon | 61,435 | 97,531,074 bytes | `2f08586b3de41bd1c87af0558b65be56b18de2dda64ab03cd4dd6e67dad91af8` |

The two explicitly linked files are bundled unchanged. The original requirement's format labels are reversed; parsing follows these actual formats. The layer pairing remains exploratory for this trial.

Release artifact: `app/build/outputs/apk/release/app-release-unsigned.apk`, 46,745,127 bytes (44.6 MiB). Both asset entries are DEFLATE-compressed; the GPKG is 7,668,975 bytes and the GeoJSON is 25,342,525 bytes. The APK entries were read back and both SHA-256 hashes match the source files.

The packaging build used a temporary, non-secret `MAPS_API_KEY=compile-only-placeholder` because this worktree has no local Maps key. The temporary `local.properties` was removed afterward. This APK is package-content evidence only and is not a runtime-ready release artifact.
