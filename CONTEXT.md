# Loud Vibrate — Session Context

## Done
- Gradle project set up (settings/build files, gradle.properties, local.properties)
- **SSIDRule** data model: `id`, `ssid`, `trigger` (IN_RANGE/OUT_OF_RANGE), `ringerMode` (RINGER/VIBRATE/SILENT), `enabled` (Boolean, default `true`); JSON via `org.json`, SharedPreferences via `SSIDRuleStore`
- Backward-compat JSON deserialization (old `inRangeMode` key, old `ringerMode` key; `enabled` defaults `true`)
- **RingerModeManager**: wraps `AudioManager.setRingerMode()`
- **SSIDRingerService** (foreground, `dataSync` type): 30s periodic WiFi scan, transition-based rule matching (OUT_OF_RANGE only fires when no IN_RANGE rules are active), filters disabled rules, logs everything to logcat tag `SSIDRinger`
- **MainActivity**: rule list (RecyclerView + drag-and-drop), service toggle (SwitchMaterial), permission request chain (nearby devices / fine+coarse+bg location / notifications / notification policy), startup permission check, empty state, FAB to add rules; service state persisted in SharedPreferences; monitoring label driven by actual service callbacks
- **AddEditRuleActivity**: SSID input, trigger radio group, ringer mode radio group, save/delete; new rules get UUID; duplicate SSID+trigger shows toast
- **SSIDRuleAdapter** (RecyclerView.Adapter): drag handle (⠿), rule number, enable SwitchMaterial, Edit button (`@color/secondary`), red "✕" delete, grayed-out when disabled
- **item_ssid_rule.xml**: card with drag handle, rule number, SSID/trigger/mode text, enable switch, Edit button (amber `@color/secondary`), red "✕" delete
- **Adaptive icon**: white WiFi arcs + bell on teal (#00897B) background
- **Dark mode**: values-night/colors.xml (lighter on_surface, darker bg/surface)
- **APK** builds, deploys, and launches on device
- **AGENTS.md** restored, **README.md** written, **CONTEXT.md** created
- **Color scheme**: teal primary (#00897B) — no indigo anywhere
- **Monitoring label fix**: removed `onStart`/`onStop` auto-bind/unbind (caused double-unbind exception that left `serviceBound` permanently stale); `onResume` re-binds if toggle is ON and not bound; `startService()`/`stopService()` set label directly instead of relying on async `onServiceConnected` callback; `stopService()` always resets `serviceBound = false` even if `unbindService` throws

## Bug fixes along the way
1. UUID generation for new rules (was `""` → overwrote first saved rule)
2. Location permission missing on API 36 → `SecurityException` in startScan
3. `ACCESS_BACKGROUND_LOCATION` added for "all the time" option
4. `textColor="?attr/colorOnSurfaceVariant"` replaced with `@color/on_surface` (attribute didn't resolve)
5. Monitoring label now driven by actual callbacks, not optimistic toggle state
6. Duplicate `registerReceiver` handled via unregister-first pattern
7. Service state persisted in prefs for recovery after process death
8. Same SSID+same trigger → toast warning instead of silent overwrite
9. Monitoring label stuck "Active" permanently due to swallowed `IllegalArgumentException` in `stopService()` when unbinding already-torn-down connection; fixed by always resetting `serviceBound = false` and setting label directly in `startService()`/`stopService()`

## Relevant files
- `app/build.gradle.kts`, `AndroidManifest.xml`
- `SSIDRule.kt`, `SSIDRuleStore.kt`, `RingerModeManager.kt`
- `SSIDRingerService.kt`, `MainActivity.kt`, `AddEditRuleActivity.kt`, `SSIDRuleAdapter.kt`
- Layouts: `activity_main.xml`, `activity_add_edit_rule.xml`, `item_ssid_rule.xml`
- Resources: `colors.xml`, `values-night/colors.xml`, `ic_launcher_*.xml`
