# requirement 

Feat 0905 退回說明

## Background

- 檢視側溝時，基本資料頁面中，新增一個紅色圖框說明“退回原因”

## UI
### UI 說明

- 檢視側溝 > 基本資料頁面 > 側溝座標編號上方，新增一個多行文字區域，底色為"#FCF1F0"，紅框；
- 標題為"退回原因"，紅字粗體；
- 內容為"[revokeComment]"(API 回傳內容)。

### UI 參考
[紅框參考](./截圖%202026-09-05%20下午2.00.00.png)

## API 
- "/v1/ditch/ditchDetails?"
- 參數
    - revokeComment
- Response 200
    ```JSON
    {
        "success": true,
        "message": "查詢成功",
        "data": {
            "ditch_id": 1306,
            "SPI_NUM": "3202-ZZ-00979",
            "SPI_STATE": "2",
            "XY_NUM": {
                "起點": "A0624pt46",
                "節點": [
                    "A0624pt45",
                    "A0624pt44"
                ],
                "終點": "A0624pt43"
            },
            "SPI_TYP": "1",
            "STR_X": "268081.743",
            "STR_Y": "2766981.756",
            "END_X": "267985.071",
            "END_Y": "2767042.770",
            "STR_LE": "",
            "END_LE": "",
            "NODE_XY": "268051.061,2766980.608_268023.674,2767006.712",
            "STR_DEP": 90,
            "END_DEP": 63,
            "STR_WID": 59,
            "END_WID": 62,
            "LENG": "121.36",
            "SLOP": "0.00000",
            "NOTE": "",
            "is_curve": 0,
            "curve_point": [],
            "is_pendingDeploy": "0",
            "revokeComment": "終點，若現場淤泥、土石或雜物垃圾厚度未達溝體淨深一半，僅生長大量雜草或樹枝堆積，機關希望歸類為輕度淤積，請協助調整",
            "nodes": [
                {
                    "node_id": 4507,
                    "NODE_ATT": "1",
                    "NODE_NUM": null,
                    "latitude": 25.010764,
                    "longitude": 121.179149,
                    "is_pendingDeploy": "0",
                    "is_virtual": "0",
                    "url": [
                        {
                            "url": "https://demo.srgeo.com.tw/TY_RSGDBIP_Folder/node_images/4507/579a6992-addb-4087-b28a-f376124f9ae1.jpg",
                            "node_id": "4507",
                            "fileCategory": "1",
                            "id": 13893,
                            "created_at": "2026-06-24T03:58:42.810000Z"
                        },
                        {
                            "url": "https://demo.srgeo.com.tw/TY_RSGDBIP_Folder/node_images/4507/bc1da2f9-39ea-4fe7-a9f3-ea6a8e95f848.jpg",
                            "node_id": "4507",
                            "fileCategory": "2",
                            "id": 13891,
                            "created_at": "2026-06-24T03:58:42.490000Z"
                        },
                        {
                            "url": "https://demo.srgeo.com.tw/TY_RSGDBIP_Folder/node_images/4507/45f7459b-b9a6-48bd-b85b-a80ad3295ba0.jpg",
                            "node_id": "4507",
                            "fileCategory": "3",
                            "id": 13892,
                            "created_at": "2026-06-24T03:58:42.797000Z"
                        }
                    ]
                },
                {
                    "node_id": 4508,
                    "NODE_ATT": "2",
                    "NODE_NUM": "1",
                    "latitude": 25.010754,
                    "longitude": 121.178845,
                    "is_pendingDeploy": "0",
                    "is_virtual": "0",
                    "url": [
                        {
                            "url": "https://demo.srgeo.com.tw/TY_RSGDBIP_Folder/node_images/4508/f4cf22ed-eb4a-48ff-a467-ffa6cd153e82.jpg",
                            "node_id": "4508",
                            "fileCategory": "1",
                            "id": 13894,
                            "created_at": "2026-06-24T03:58:44.140000Z"
                        },
                        {
                            "url": "https://demo.srgeo.com.tw/TY_RSGDBIP_Folder/node_images/4508/4361a7ce-285a-48fa-a339-62887f3be7f4.jpg",
                            "node_id": "4508",
                            "fileCategory": "2",
                            "id": 13895,
                            "created_at": "2026-06-24T03:58:44.393000Z"
                        },
                        {
                            "url": "https://demo.srgeo.com.tw/TY_RSGDBIP_Folder/node_images/4508/e23312d6-51f5-4907-88df-ec41ba73756c.jpg",
                            "node_id": "4508",
                            "fileCategory": "3",
                            "id": 13896,
                            "created_at": "2026-06-24T03:58:44.837000Z"
                        }
                    ]
                },
                {
                    "node_id": 4509,
                    "NODE_ATT": "2",
                    "NODE_NUM": "2",
                    "latitude": 25.01099,
                    "longitude": 121.178574,
                    "is_pendingDeploy": "0",
                    "is_virtual": "0",
                    "url": [
                        {
                            "url": "https://demo.srgeo.com.tw/TY_RSGDBIP_Folder/node_images/4509/2a1c9301-8287-40c1-a16e-0b90c20f389a.jpg",
                            "node_id": "4509",
                            "fileCategory": "1",
                            "id": 13897,
                            "created_at": "2026-06-24T03:58:45.743000Z"
                        },
                        {
                            "url": "https://demo.srgeo.com.tw/TY_RSGDBIP_Folder/node_images/4509/f25fbc40-6cd2-47e1-97ad-728b4d4ba5f9.jpg",
                            "node_id": "4509",
                            "fileCategory": "2",
                            "id": 13898,
                            "created_at": "2026-06-24T03:58:45.950000Z"
                        },
                        {
                            "url": "https://demo.srgeo.com.tw/TY_RSGDBIP_Folder/node_images/4509/ba77cb85-13af-4688-bcc6-c8215b934786.jpg",
                            "node_id": "4509",
                            "fileCategory": "3",
                            "id": 13899,
                            "created_at": "2026-06-24T03:58:46.957000Z"
                        }
                    ]
                },
                {
                    "node_id": 4510,
                    "NODE_ATT": "3",
                    "NODE_NUM": null,
                    "latitude": 25.011316,
                    "longitude": 121.178192,
                    "is_pendingDeploy": "0",
                    "is_virtual": "0",
                    "url": [
                        {
                            "url": "https://demo.srgeo.com.tw/TY_RSGDBIP_Folder/node_images/4510/beafb89e-5fec-4f6d-8fa2-a02f2b4c56d1.jpg",
                            "node_id": "4510",
                            "fileCategory": "1",
                            "id": 13900,
                            "created_at": "2026-06-24T03:58:47.340000Z"
                        },
                        {
                            "url": "https://demo.srgeo.com.tw/TY_RSGDBIP_Folder/node_images/4510/fa8c438d-13f7-4f98-807b-23fde2cfc5c9.jpg",
                            "node_id": "4510",
                            "fileCategory": "2",
                            "id": 13901,
                            "created_at": "2026-06-24T03:58:47.443000Z"
                        },
                        {
                            "url": "https://demo.srgeo.com.tw/TY_RSGDBIP_Folder/node_images/4510/77690dff-4030-429b-b044-30d1cd9f578f.jpg",
                            "node_id": "4510",
                            "fileCategory": "3",
                            "id": 13902,
                            "created_at": "2026-06-24T03:58:49.147000Z"
                        }
                    ]
                }
            ]
        }
    }
    ```