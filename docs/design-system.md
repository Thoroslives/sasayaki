# Sasayaki design system

Sasayaki means *whisper*. The interface should feel like one: quiet until it matters, then
clear and immediate. Everything here maps to code in `app/src/main/java/com/sasayaki/ui/theme`.

## Principles

1. **Quiet by default, loud when it matters.** Calm surfaces and neutral text; color and
   motion are spent on state changes (recording, success, failure) and the one primary action.
2. **One primary action per screen.** If two buttons compete, one of them is a text button.
3. **Everything saves itself.** No Save buttons on settings or profiles. Validation is inline.
4. **Feedback within a frame.** Every tap answers with a ripple, a spring, or a haptic tick.
   Never block the UI on the network; show progress where the result will appear.
5. **Explain once, briefly.** One line of supporting text at most. Prefer a good label to a
   paragraph.

## Color

Built from three brand palettes plus a neutral. Tones are perceptual lightness (CIE L*), so
`Pine40` and `Clay40` have the same contrast against any surface.

| Palette | Hue | Role |
| --- | --- | --- |
| Pine | teal green | `primary`: main actions, selection, active state |
| Sage | muted green | `secondary`: tonal buttons, chips, selection indicators |
| Clay | terracotta | `tertiary`: live recording, attention that is not an error |
| Ember | red | `error`: failures only |
| Ink / Mist | green-tinted greys | surfaces, text, outlines |

Rules:

- Use roles (`MaterialTheme.colorScheme.*`), never palette tokens, in screens. Tokens exist
  only to build the schemes.
- Layer surfaces with `surfaceContainerLow` → `surfaceContainerHighest`, not elevation shadows.
- Text on containers uses the matching `on*Container` role.
- Wallpaper colors (Android 12+) are opt-in from Settings; the brand scheme is the default.

## Typography

| Use | Style | Family |
| --- | --- | --- |
| Hero numbers, onboarding titles | `display*` | Serif |
| Screen titles (top app bars) | `headline*`, `titleLarge` | Serif |
| Card and list headlines | `titleMedium`, `bodyLarge` | Sans |
| Body and supporting text | `body*` | Sans |
| Buttons, chips, metadata | `label*` | Sans |

Use the `*Emphasized` variants for a value that is the point of the element (a word count, the
selected option), never for whole paragraphs. Sentence case everywhere.

## Shape

| Token | Radius | Typical use |
| --- | --- | --- |
| `extraSmall` | 6 dp | text fields, tooltips |
| `small` | 10 dp | chips, small badges |
| `medium` | 16 dp | cards |
| `large` | 20 dp | list groups, sheets' inner content |
| `largeIncreased` | 24 dp | hero cards |
| `extraLarge` | 28 dp | dialogs, bottom sheets |
| `extraExtraLarge` | 48 dp | brand moments |

Buttons are fully rounded. `MaterialShapes` (cookie, clover, sunny…) are reserved for brand
moments: the logo container, empty states, avatars, and loading indicators.

## Spacing

4 dp grid, tokens in `Spacing`.

- Screen edge: `Spacing.screenHorizontal` (16 dp).
- Between sections: `Spacing.xl` (24 dp). Section label to its group: `Spacing.s`.
- Inside cards: `Spacing.l`–`Spacing.xl`.
- Segmented list items are separated by the component's own 2 dp gap.

## Motion

`MotionScheme.expressive()` drives every component. In custom animations use the scheme's
specs instead of fixed durations:

- `MaterialTheme.motionScheme.fastSpatialSpec()` for small movements (toggles, chips).
- `defaultSpatialSpec()` for layout and size changes.
- `*EffectsSpec()` for color and alpha.

Springs may overshoot on position and size, never on color or opacity.

## Iconography

Material Symbols Rounded, weight 400, 24 dp, inlined in `SasayakiIcons`. Filled variants
(`*Filled`) only mark the selected navigation destination and the dictation controls. Every
actionable icon has a `contentDescription`; decorative icons next to a text label pass `null`.

## Surfaces

Screens sit on `surfaceContainer`; grouped rows and cards sit on `surfaceBright`, which reads
as raised in both themes. The floating bubble is the exception: it always uses the dark scheme
so it looks the same over any app.

## Components

All in `ui/components`. Reach for these before building a one-off.

| Component | Use it for |
| --- | --- |
| `SettingsPage` | Any list screen with a large collapsing title (settings, providers, profiles). |
| `SegmentedGroup` + `NavigationItem` / `SwitchItem` | Grouped rows. Summaries show the current value; `needsAttention` turns it red. |
| `ConnectedChoice` | Two to four mutually exclusive options shown inline (theme, writing style). |
| `ModelPickerSheet` | Choosing a model from a provider's list, with search and manual entry. |
| `SetupGroup` | Permission and setup rows, including the restricted-settings walkthrough. |
| `EmptyState` | Empty lists: a Material shape, one line of title, one of body, optional action. |
| `Pill` | Small metadata and status labels. |

Rules:

- Settings save as they change. Only creating something new (a provider, a rule) has a button.
- Lists of things the user owns use a FAB to add; the FAB hides while the list is empty and the
  empty state offers the same action instead.
- Destructive actions confirm in a dialog unless they can be undone from a snackbar.

## Floating bubble

The bubble is the product; everything else is configuration.

| State | Shape | Content |
| --- | --- | --- |
| Ready | 52 dp circle, `primaryContainer` | The mark. Flashes a check after text is inserted. |
| Recording | Pill | Discard · live waveform with timer and profile · pause · stop (clay) |
| Processing | Pill | Morphing loading indicator, "Transcribing" or "Cleaning up" (clay), cancel |
| Failed | Pill, `errorContainer` | The reason, retry when the audio was kept, dismiss |

- It docks to the nearest screen edge after a drag and grows away from that edge.
- Dragging shows a remove target at the top (overlays draw below the keyboard); dropping on it
  turns the bubble off.
- Long-press the bubble, or tap the waveform while recording, to switch profile.
- Shape changes use the spatial spring, so the bubble visibly morphs instead of jumping.

## Haptics

| Moment | Pattern |
| --- | --- |
| Recording starts | `recordStart` |
| Recording stops / processing cancelled | `recordStop` |
| Text inserted | `complete` |
| Failure | `error` |
| Menu opens, profile switched | `tick` |

## Voice

- Name things by what they do for the user: "Speech to text", "Cleanup", not "ASR" or "LLM".
- Buttons are verbs: "Turn on", "Test connection", "Try again".
- Errors say what happened and what to do next, in that order.

## Logo

The mark is a brush-stroke S that swells and thins like a breath, with two sound arcs fading
to its right: a whisper being spoken. It is cream on pine.

- Source of truth: `SasayakiMark` in `ui/theme/SasayakiLogo.kt`. The launcher foreground,
  monochrome (themed icon) layer and notification icon are generated from the same paths.
- In the app, use `SasayakiLogo` (mark on a pine circle). Keep at least a quarter of its size
  as clear space and never recolor the arcs separately from the S.
- The monochrome layer is what Android 13+ tints for themed icons; keep it in sync.
