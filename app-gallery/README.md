# Gallery — Android visual prototype

Phase 1 of the independent artwork-first client. This is a runnable Compose app,
not a replacement for the existing Shaft application.

## Run

Use the repository's JDK 17 / Android SDK configuration:

```sh
./gradlew :app-gallery:assembleDebug
adb install -r app-gallery/build/outputs/apk/debug/app-gallery-debug.apk
adb shell am start -n ceui.pixiv.gallery.preview/ceui.pixiv.gallery.MainActivity
```

Package: `ceui.pixiv.gallery.preview`. Label: `Shaft Gallery · 样板`.
The app has no Internet permission and no connection to the user's Pixiv account.

## Scope

- Adaptive artwork grid, sample category and ID search.
- Local bookmarks persisted on this device, with a bookmark-only view.
- Detail image fitted within the viewport, double-tap and pinch zoom, bounded pan.
- Artwork information sheet and an external link to the original Pixiv work.
- Light/dark appearance, retained list position when returning from detail.
- No fold/unfold transition handling or lifecycle acceptance in scope.

Sample works are selected from `app/src/main/assets/pixiv_prime/prime_index.json`.
Their uncropped master previews are bundled for offline evaluation; source URLs
are recorded in `sample-sources.json`. IDs identify the original works. No author
names or titles are fabricated. Category labels are local fixtures. Artwork rights
remain with their authors; review permissions before distributing these fixtures
in a public release.

The revised gallery uses a compact category bar, original-ratio masonry, 6dp gaps,
per-artwork contextual menus and a compact floating navigation surface. It is a
Pinterest-inspired browsing study, not a pixel-identical Pinterest clone.

## Next milestone

After visual direction is accepted, add a repository boundary for a real
login → recommendations → detail → bookmark path. Extract only its required
Pixiv capabilities. Keep Shaft-owned backend signing separate. Do not depend on
the work-in-progress `core-network` module until its boundary is verified.

Not implemented: OAuth, live pagination/search/bookmarks, multi-image artworks,
animated works, downloads, shared-element transitions, production release.
