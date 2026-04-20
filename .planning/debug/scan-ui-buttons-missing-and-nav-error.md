---
status: awaiting_human_verify
trigger: "scan-ui-buttons-missing-and-nav-error"
created: 2026-04-18T00:00:00Z
updated: 2026-04-18T00:10:00Z
---

## Current Focus

hypothesis: THREE root causes identified — (1) ScanContainerFragment defaults to TAB_YOLO which lacks single/multi buttons, (2) unguarded navigation allows double-tap crash, (3) YOLO pipeline shows no server status so LLM failures appear as "Failed"
test: Fix all three: change default tab to TAB_PHOTO, add navigation guard, verify server connectivity visible
expecting: Buttons visible, no crash, better failure feedback
next_action: Implement fixes in ScanContainerFragment, InventoryFragment, and optionally YoloScanFragment

## Symptoms

expected: Single-item and multi-item scan mode buttons should be visible in the scan UI. Clicking them should trigger the appropriate scan mode. Scan results should show recognized food items.
actual: After fixing AI Vision Scan button navigation, the rectangular scan mode selection buttons (single/multi item) disappeared from the UI. The app defaults to AI Vision Scan mode. When navigating from scan_container, a crash occurs. All scan results show "Failed".
errors: 
- Navigation action/destination com.example.foodexpiryapp:id/action_inventory_to_yolo_scan cannot be found from the current destination Destination(com.example.foodexpiryapp:id/navigation_scan_container)
- At InventoryFragment.setupActionButtons$lambda$4(InventoryFragment.kt:133)
reproduction: 1. Open app, go to Inventory, 2. Click AI Vision Scan button, 3. Camera opens but scan mode buttons missing, 4. Results show "Failed", 5. Clicking scan again while on scan_container crashes
started: After recent UI fix for AI Vision Scan button navigation
timeline: Immediately after recent fix

## Eliminated

## Evidence

- timestamp: 2026-04-18T00:01
  checked: nav_graph.xml
  found: action_inventory_to_yolo_scan navigates from navigation_inventory to navigation_scan_container. navigation_scan_container has action_scan_container_to_confirmation. action_inventory_to_vision_scan exists but unused by InventoryFragment.
  implication: cardVisionScan uses action_inventory_to_yolo_scan which goes to scan_container, not standalone vision_scan

- timestamp: 2026-04-18T00:02
  checked: ScanContainerFragment.kt
  found: Defaults to TAB_YOLO (position 2) via setCurrentItem(ScanPagerAdapter.TAB_YOLO, false). ViewPager2 has 3 tabs: TAB_PHOTO=0 (VisionScanFragment), TAB_BARCODE=1 (ScanFragment), TAB_YOLO=2 (YoloScanFragment).
  implication: Opening scan_container shows YOLO tab, NOT Vision tab that has single/multi buttons

- timestamp: 2026-04-18T00:03
  checked: fragment_yolo_scan.xml
  found: YOLO scan layout has NO toggleScanMode/btnSingleMode/btnMultiMode elements. Only has camera, capture button, gallery, progress overlay.
  implication: YOLO tab cannot show single/multi scan mode buttons — they don't exist in its layout

- timestamp: 2026-04-18T00:04
  checked: fragment_vision_scan.xml
  found: Lines 123-168 contain toggleScanMode LinearLayout with btnSingleMode ("📷 1 item") and btnMultiMode ("🔍 Multi") buttons. Also has btn_settings for server config and checkServerConnection() method.
  implication: Single/multi buttons ONLY exist in VisionScanFragment, which is TAB_PHOTO (position 0) in the ViewPager

- timestamp: 2026-04-18T00:05
  checked: InventoryFragment.kt setupActionButtons()
  found: cardVisionScan (line 131-137) uses action_inventory_to_yolo_scan with try-catch. btnPhotoScan (line 149-155) also uses same action. Multiple buttons navigate to scan_container without checking current destination first.
  implication: Double-tap causes IllegalArgumentException because destination changes between first and second click

- timestamp: 2026-04-18T00:06
  checked: DetectionPipeline.kt
  found: Pipeline requires LLM server (Ollama/LM Studio) for classification. If client.analyzeFood() returns null or throws, status becomes FAILED. No server connection check before running.
  implication: If LLM server unreachable, all detected items get FAILED status. YOLO scan tab doesn't show server connection status or settings button.

- timestamp: 2026-04-18T00:07
  checked: VisionScanFragment.kt
  found: Has checkServerConnection() that warns user. Has btn_settings to configure Ollama server. Has single/multi mode toggle. YoloScanFragment has NONE of these.
  implication: Defaulting to YOLO tab hides server status and settings, making LLM failures invisible

## Resolution

root_cause: Three related issues caused by recent navigation fix: (1) ScanContainerFragment defaults to TAB_YOLO (position 2) but single/multi scan mode buttons only exist in VisionScanFragment layout (TAB_PHOTO, position 0), so the buttons are invisible; (2) InventoryFragment navigation buttons lack current destination guard, allowing double-tap to trigger action_inventory_to_yolo_scan when already on scan_container; (3) YOLO scan tab has no LLM server status or settings, so unreachable server causes all items to silently fail classification with FAILED status.
fix: (1) Changed ScanContainerFragment default tab from TAB_YOLO to TAB_PHOTO so VisionScanFragment (with single/multi buttons and server status) is shown first. (2) Added currentDestination guard to all navigation calls in InventoryFragment to prevent double-tap crash. (3) The server status issue is resolved by fix (1) since the Vision tab checks server connection and shows settings gear.
verification: 
files_changed: [ScanContainerFragment.kt, InventoryFragment.kt]
