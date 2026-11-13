# Glass navigation and pane gestures

Research note: 2026-11-13. This compares the requested Apple interaction cues with the app's native Compose implementation. It does not recommend copying Apple's proprietary Liquid Glass implementation or claiming Android backdrop blur.

## Findings

- Apple describes materials as a way to establish depth and hierarchy between foreground controls and underlying content. Its current guidance treats Liquid Glass as a distinct functional layer for controls and navigation, and says it should be used sparingly with legibility preserved. [Apple HIG: Materials](https://developer.apple.com/design/human-interface-guidelines/materials)
- Apple describes tab bars as top-level navigation that preserves navigation state within sections. It recommends keeping the bar visible, using concise labels, and using only the tabs needed to represent the app's hierarchy. [Apple HIG: Tab bars](https://developer.apple.com/design/human-interface-guidelines/tab-bars)
- Apple motion guidance recommends purposeful, brief feedback that follows a person's gesture, avoids unnecessary frequent animation, and can be cancelled. [Apple HIG: Motion](https://developer.apple.com/design/human-interface-guidelines/motion)
- Compose's `Modifier.blur` blurs the composable's own drawn content (a surface/layer blur); it is not backdrop blur of content behind the composable. [Android Developers: Blurring in Compose](https://developer.android.com/develop/ui/compose/graphics/blur/overview)
- Compose gesture guidance recommends higher-level gesture modifiers where possible. For custom gestures, pointer events pass through Initial, Main, and Final phases; Main-pass handling gives children the opportunity to consume input first. Custom handlers should check whether another handler consumed the event. [Android Developers: Understand gestures](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures)

## Applied design decisions

- Keep the three native destinations (Today, Activity, Circle), their visible text labels, selectable/tab semantics, and persistent tap targets. Refine the bottom bar with a tinted translucent surface, a fine border/highlight, restrained depth, and an animated accent capsule. This is a glass-like treatment; it does not sample or blur the screen behind it. Shadow drawing is clipped outside the rounded glass surface so it cannot darken the translucent interior.
- Keep the bar legible in all four themes, use a vertical icon-and-label arrangement in a 96dp bar at large font scale, preserve 48dp minimum targets, and mirror the selection/gesture direction for RTL.
- Pane drag follows the finger with modest resistance and springs back on cancellation. It commits at a displacement threshold without a velocity fling. Reduced motion removes the spring/indicator travel while leaving tab navigation usable.
- The pane recognizer is child-first, ignores Android system-edge start zones, and is disabled while top-level sheets are open. Activity's chart-only horizontal week gesture and Today’s finish slider retain priority over pane navigation; body swipes outside the chart navigate panes. The visible tab buttons remain the predictable fallback.

## Limits

Apple's HIG describes platform behavior and visual principles; it is not a specification for Android. The design here applies those principles with native Compose drawing and interaction APIs. Earlier emulator theme, small-width and 200% font checks are dated in BUILD-EVIDENCE.md. Latest drawing/gesture refinements still require their final-build checks, including RTL and reduced motion. Real-phone haptics, TalkBack and frame-rate performance are not established.
