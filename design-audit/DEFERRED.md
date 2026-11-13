# Explicit owner decisions and unavailable checks

The owner approved implementing all batches where possible while explicitly leaving logo and scale decisions open. These are not silently resolved by choosing new values.

- `visual-1-002`, `visual-2-002`: replacing the existing primary-button glow with a hard-offset shadow needs approved offset/depth values. Primary fill, foreground, disabled/focus states and sheen are corrected separately; the shadow dimension/style decision remains open.
- `visual-1-005`, `visual-2-005`: current panel/key/theme-card radii vary from named shapes. Choosing canonical role radii is an open scale decision; existing shapes are retained.
- `visual-1-008`, `visual-2-007`: amount-input size ceilings76/60/46 remain existing implementation values until the figure roles/scale are decided. Exact width fitting, Mono family, named keypad/stamp roles and accessibility are addressed separately. Do not claim these broad type-role findings wholly fixed.
- Logo family, clear space/reversals/minimums, global spacing/radius/shadow scales and the canonical icon family remain explicit owner choices from the contract. No new visual identity is invented.
- Physical-device TalkBack/haptic quality, OEM payment delivery/Doze behavior and physical frame-rate claims cannot be established by emulator evidence. The final pass exercises the available emulator/static cases and retains these limitations.

F-Droid preparation is deferred at the owner's request. No license, FOSS flavor or submission is being added in this work.

A post-freeze cosmetic observation is retained for the next audit: the no-circle decorative illustration in CircleScreen.kt still blends accent2. The current routine-control/progress findings targeted shared controls and their listed consumers; no further application change was added during release finalization.
