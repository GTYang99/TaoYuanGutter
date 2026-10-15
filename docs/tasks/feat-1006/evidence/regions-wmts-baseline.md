# 桃園行政區 WMTS 視覺基準

- Captured: 2026-10-14 (Asia/Taipei)
- User decision: preserve the existing WMTS appearance when replacing its data source with GPKG.
- Endpoint: `https://taipei.srgeo.com.tw/TY_RSGDBIP_BK/geoserver/gwc/service/wmts`
- Request: `SERVICE=WMTS&VERSION=1.0.0&REQUEST=GetTile&LAYER=regions&STYLE=&FORMAT=image/png8&TILEMATRIXSET=WebMercatorQuad&TILEMATRIX=13&TILEROW=3508&TILECOL=6856`
- Response: HTTP 200, `image/png`, 256 × 256 indexed PNG.
- SHA-256: `fa589fdc27d6e7afd37b6c85c8e7477d496df10e90f695e73a0a83e547b9634c`
- Observed style in this representative boundary tile: transparent fill/background; boundary stroke with opaque core color `#762E77` and anti-aliased edge pixels; no text labels in the captured tile. The source tile has transparent background pixels and 1,259 fully opaque `#762E77` pixels. Along the near-vertical sample segment (rows 5–49, columns 220–255), the opaque stroke core has a median width of 2 px; the antialiased raster footprint has a median width of 4 px at alpha ≥64.
- Existing host overlay options use z-index `-0.5f` and zero transparency. Preserve those values in every host.

The image is the server-provided `regions` WMTS tile before source replacement and is the stable visual reference for the matching local tile at z13/x6856/y3508. Compare transparent fill, stroke color/width/opacity, and labels against this tile. Use the GPKG as the geometry truth; do not require pixel-identical boundary locations where WMTS generalization differs. Compare style metrics along local boundary transects instead.
