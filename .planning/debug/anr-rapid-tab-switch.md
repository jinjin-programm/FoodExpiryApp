---
status: awaiting_human_verify
trigger: "ANR Crash on Rapid Tab Switching in FoodExpiryApp"
created: 2026-04-21T00:00:00Z
updated: 2026-04-21T00:00:01Z
---

## Current Focus

hypothesis: Circular update loop between ViewPager2 and BottomNavigationView caused by post{} wrapper creating stale queued callbacks that cascade page changes
test: Read MainActivity.kt lines 71-108, verify the feedback loop mechanism
expecting: post{} queues callbacks that fire after state changes, bypassing the guard check
next_action: implement fix - add isPageChangeInProgress flag and remove post{} wrapper

## Symptoms

expected: Smooth tab switching with no lag or ANR
actual: App crashes with ANR after rapidly switching tabs, CPU 105%, "Input dispatching timed out (Waited 10009ms)"
errors: ANR in com.example.foodexpiryapp - Reason: Input dispatching timed out (Waited 10009ms for FocusEvent(hasFocus=false))
reproduction: Rapidly switch between tabs via BottomNavigationView or ViewPager2 swiping
started: Recurring issue, user reports multiple fix attempts

## Eliminated

## Evidence

- timestamp: 2026-04-21T00:00:00Z
  checked: MainActivity.kt lines 71-108 (ViewPager2/BottomNav sync)
  found: ViewPager2 onPageSelected sets bottomNavigation.selectedItemId (line 84) which triggers setOnItemSelectedListener (line 89) which uses post{} to set viewPager.currentItem (lines 101-105)
  implication: post{} wrapper creates asynchronous callbacks that can bypass the synchronous guard check, causing cascading page changes

- timestamp: 2026-04-21T00:00:00Z
  checked: Guard conditions in both listeners
  found: onPageSelected has guard on line 83 (if selectedItemId != itemId), listener has guard on line 98 (if currentItem != page) AND inside post on line 102
  implication: The post{} inner guard (line 102) checks against stale state - by the time the post runs, viewPager.currentItem may have changed due to other queued posts, causing it to fire unnecessarily

- timestamp: 2026-04-21T00:00:00Z
  checked: Knowledge base for similar patterns
  found: Previous ViewPager2 issue (yolo-scan-camera-black) was about camera lifecycle, not ANR/feedback loops
  implication: No prior pattern match, treating as new issue

## Resolution

root_cause: Circular update loop between ViewPager2 and BottomNavigationView. The post{} wrapper in setOnItemSelectedListener (line 101) queued callbacks asynchronously. During rapid tab switches, multiple stale callbacks accumulated. Each callback changed the page, triggering onPageSelected, which triggered the listener again. The guard check inside the post{} ran against already-changed state, allowing the callback to fire when it shouldn't, creating a cascade of page changes that overwhelmed the main thread.
fix: Added isPageChangeInProgress boolean flag to prevent re-entrant updates. Removed the post{} wrapper in setOnItemSelectedListener, replacing with direct synchronous assignment. Added guard check in both onPageSelected (early return if isPageChangeInProgress) and in the listener (skip if isPageChangeInProgress).
verification: Build compiles successfully (compileDebugKotlin passed). Needs human verification on device.
files_changed: [app/src/main/java/com/example/foodexpiryapp/MainActivity.kt]
