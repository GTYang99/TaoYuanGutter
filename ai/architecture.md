# Project Architecture

## Overview

`TaoYuanGutter` is an Android Kotlin app for gutter inspection, form editing, photo capture, draft saving, and server sync.

## Stack

- Android 9+
- Kotlin
- Google Maps SDK
- CameraX
- Retrofit + OkHttp + Gson
- Room
- ViewBinding + ViewPager2

## Structure

- `MainActivity`: map entry and workflow hub
- `api`: network service, DTOs, `GutterRepository`
- `common`: shared helpers and upload utilities
- `gutter`: form, inspect, camera, waypoint models, flow helpers
- `main`: map-screen UI controllers and `MainViewModel`
- `map`: map rendering, camera, scope, measurement controllers
- `pending`: draft storage, Room, migration, pending draft UI
- `login`: auth entry flow
- `ui/theme`: shared theme files

## Pattern

The app is not strict MVVM. It is mostly:

- UI in activities, fragments, and bottom sheets
- workflow logic in controllers and coordinators
- network and persistence in repositories
- shared utilities in `common`

`MainViewModel` is small and only carries limited UI state.

## Core Flow

1. User selects or opens a gutter on the map
2. `AddGutterBottomSheet` / `GutterFormActivity` edits the session
3. `Waypoint.basicData` and `photoUris` are updated
4. `GutterRepository` submits structured data
5. Photos are uploaded separately
6. Drafts are stored in Room and can be restored later

## Entry Points

- Launcher: `login.LoginActivity`
- Workspace: `MainActivity`
- Form: `gutter.GutterFormActivity`
- Inspect: `gutter.GutterInspectActivity`
- Camera: `gutter.LandscapeCameraActivity`
- Point picker: `gutter.MapPointPickerActivity`
- Import: `gutter.ImportExistingWaypointActivity`

## Notes

- Drafts migrated from SharedPreferences into Room
- Camera direction uses `OrientationEventListener` + CameraX `targetRotation`
- Feature work usually belongs in `gutter`, `map`, `pending`, or `api`
