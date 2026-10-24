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

Built from four brand palettes plus a neutral. Tones are perceptual lightness (CIE L*), so
`Pine40` and `Clay40` have the same contrast against any surface.

| Palette | Hue | Role |
| --- | --- | --- |
| Pine | teal green | `primary`: main actions, selection, active state |
| Sand | warm beige | `secondary`: chips, filters, supporting containers |
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
