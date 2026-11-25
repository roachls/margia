# MARGIA Rule Editor & YAML Rules Guide

MARGIA allows you to create, edit, and share custom Musician behaviors without writing Java code. Custom rules are defined as finite-state machines stored in YAML format.

## Rule Storage & Precedence

MARGIA loads rules from three sources in the following order of priority:

1. **User YAML Rules (`~/.margia/rules/*.yaml`)**: Editable rules saved on your computer. If a user rule has the same `name` as a bundled template or built-in Java rule, the user rule takes precedence.
2. **Bundled YAML Templates (`[Template]`)**: Read-only example rules packaged inside MARGIA. You can inspect them in the Rule Editor and click **Save As...** to create your own editable copy.
3. **Built-in Java Rules**: Compiled rules (`random`, `randomFlavor`, `statebased`, `receiver`, etc.).

---

## Using the Rule Editor

Open the Rule Editor from the main window via **Edit -> Rule Editor...** or the **Rules...** toolbar button.

### Sidebar Controls
* **Rules & Templates List**: Displays all custom user rules and read-only `[Template]` rules.
* **New Blank**: Starts a fresh two-state rule template.
* **Delete Selected**: Deletes the selected user rule from `~/.margia/rules/` (disabled for read-only templates).
* **Import YAML...**: Validates and imports an external `.yaml` or `.yml` rule file into your user rules directory.
* **Export YAML...**: Saves the active rule (including any custom State Graph layout coordinates) to a `.yaml` file to share with others.

### Editor Tabs
All three tabs stay synchronized when you switch between them:

1. **Visual Builder**:
   * **Configurable UI Properties**: Define parameters (`INT`, `DOUBLE`, `BOOLEAN`, `STRING`, `STRING_LIST`) that appear in each Musician's Options panel. Reference them in state or action parameters using `$key` (for example, `$sequenceLength`).
   * **States, Actions & Transitions**: Add or remove states, assign actions, and configure transitions in a tabular form.
2. **State Graph**:
   * **Double-click** anywhere on the canvas to create a new state at that location.
   * **Drag** any state oval to reposition it (coordinates `x` and `y` are saved into the YAML file).
   * **Shift + Drag** (or toggle **Draw Transition Mode**) from one oval to another to draw a transition arrow.
   * **Click an arrow's label** to remove that transition.
   * **Click a state oval** to edit its name, state type, parameter, actions, or mark it as the starting state in the bottom inspector bar.
   * **Zoom**: Use `-`, `+`, `100%`, `Fit`, or `Ctrl + Mouse Wheel` to zoom and frame the graph.
3. **YAML Source**:
   * View or hand-edit the raw YAML definition directly.

### Validation & Live Hot-Reload
* **Validate**: Checks that the YAML syntax, state references, `$property` placeholders, and numeric ranges are valid.
* **Save & Apply Live**: Validates the rule, saves it to `~/.margia/rules/<name>.yaml`, updates all Musician rule dropdowns, and **immediately reloads** any active Musicians currently using that rule so you can hear changes while MARGIA is playing.
* **Save As...**: Prompts for a new rule name and saves an editable copy (required when customizing a `[Template]`).

---

## Configurable UI Properties (`properties`)

Each entry in `properties` creates a control in the Musician's Options panel and can be referenced anywhere an integer or string parameter is expected by prefixing its `key` with `$` (e.g., `$holdTicks`).

| Property `type` | Required Fields | Optional Fields | Description |
| :--- | :--- | :--- | :--- |
| `INT` | `key`, `label`, `defaultValue`, `min`, `max` | `step` (default `1`) | Integer spinner/slider control. |
| `DOUBLE` | `key`, `label`, `defaultValue`, `min`, `max` | `step` (default `0.1`) | Floating-point control. |
| `BOOLEAN` | `key`, `label`, `defaultValue` | — | Checkbox control (`true` / `false`). |
| `STRING` | `key`, `label`, `defaultValue` | — | Free-text input field. |
| `STRING_LIST` | `key`, `label`, `defaultValue`, `values` | — | Dropdown selector populated from `values` (e.g., `["MAJOR", "MINOR"]`). |

---

## Reference: State Types (`states`)

Every state in the `states` map has a unique name (its YAML key), a `type`, an optional list of `actions` executed when the state is active, and optional `x` / `y` coordinates for the State Graph.

### 1. `ALWAYS_TRANSITION`
Executes its `actions` on the current tick and immediately transitions to `nextState`.
* `nextState` *(String)*: Name of the target state.
* `actions` *(List, optional)*: Actions to perform on this tick.

### 2. `DELAYED_TRANSITION`
Waits `delay` ticks (while performing its `wrappedState`'s actions on each tick) before transitioning to `wrappedState`.
* `delay` *(int or `$property`)*: Number of ticks to wait.
* `wrappedState` *(String)*: Target state to delegate actions to and transition into after `delay` ticks.

### 3. `COUNTDOWN`
Counts down from `ticks` whenever a message is processed; when the counter reaches `0`, transitions to `nextState` and resets the counter.
* `ticks` *(int or `$property`)*: Number of active ticks before transitioning.
* `nextState` *(String)*: Target state when the countdown finishes.
* `actions` *(List, optional)*: Actions to perform while counting down.

### 4. `PSEUDO_RANDOM`
Computes a deterministic pseudo-random number in `[0, maxRoll)` from the Musician's ID, current tick, and last chord played, then transitions to the matching `NumericRange` target.
* `maxRoll` *(int or `$property`)*: Upper bound (exclusive) of the roll.
* `rangeTransitions` *(List)*: List of `{ min, max, target }` ranges (inclusive). If no range matches, stays in the current state.
* `actions` *(List, optional)*: Actions to perform on this tick.

### 5. `DIE_ROLLING`
Rolls virtual dice using standard RPG dice notation (e.g., `"1d20"`, `"2d6+1"`) and transitions to the matching `NumericRange` target.
* `dice` *(String or `$property`)*: Dice specification passed to `DieRoller`.
* `rangeTransitions` *(List)*: List of `{ min, max, target }` ranges (inclusive).
* `actions` *(List, optional)*: Actions to perform on this tick.

### 6. `NOTE_BASED`
Inspects the next incoming `Chord` on the Musician's queue, normalizes its notes to pitch classes (`0` = C, `1` = C#, ..., `11` = B), and transitions if the pitch-class set matches an entry in `noteTransitions`.
* `noteTransitions` *(List)*: List of `{ notes:, target: "stateName" }`.
* `actions` *(List, optional)*: Actions to perform on this tick.

### 7. `CONDITIONAL`
Evaluates a built-in condition against the Musician and branches to `trueState` or `falseState`.
* `condition` *(String)*: One of:
  * `CHORDS_PLAYED_GTE`: True if `musician.getChordsIvePlayed() >= threshold`.
  * `TICK_MODULO`: True if `threshold > 0` and `currentTick % threshold == 0`.
  * `IS_MUTED`: True if the Musician is currently muted.
  * `HAS_LAST_CHORD`: True if the Musician has played at least one chord (`getMyLastChord() != null`).
  * `QUEUE_EMPTY`: True if the Musician's incoming message queue is empty.
* `threshold` *(int or `$property`, optional)*: Numeric argument used by `CHORDS_PLAYED_GTE` and `TICK_MODULO`.
* `trueState` *(String)*: Target state when the condition is true.
* `falseState` *(String, optional)*: Target state when false (if omitted, stays in the current state).
* `actions` *(List, optional)*: Actions to perform on this tick.

### 8. `NEVER_TRANSITION`
A terminal state that executes its `actions` every tick and never transitions to another state.
* `actions` *(List, optional)*: Actions to perform on each tick.
---

## Reference: Musical Actions (`actions`)

Each entry in a state's `actions` list specifies a `type` and an optional `params` map. Parameter values can be literals or `$property` placeholders.

### Chord Playback & Transformation

| Action `type` | Parameters (`params`) | Behavior |
| :--- | :--- | :--- |
| `PLAY_CHORD` | *(none)* | Plays the incoming `Chord` unchanged. |
| `PLAY_CHORD_UP_INTERVAL` | `interval` *(int)* | Transposes the incoming chord up by `interval` scale degrees in the Musician's current `Key` (`1` = unison, `2` = 2nd, `5` = 5th). |
| `PLAY_CHORD_DOWN_INTERVAL` | `interval` *(int)* | Transposes the incoming chord down by `interval` scale degrees in the Musician's current `Key`. |
| `PLAY_NOTE_UP_OCTAVE` | *(none)* | Transposes all notes in the incoming chord up 12 semitones, wrapping by octaves if needed to stay inside the Musician's note range. |
| `PLAY_CHORD_TWICE_LENGTH` | *(none)* | Plays the incoming chord at double duration (capped at 16 ticks / 1 whole note). |
| `PLAY_CHORD_HALF_LENGTH` | *(none)* | Plays the incoming chord at half duration (minimum 1 tick). |
| `PLAY_CHORD_UP_VELOCITY` | `diff` *(int)* | Plays the incoming chord with `diff` added to its velocity (capped at 127). |
| `PLAY_CHORD_DOWN_VELOCITY` | `diff` *(int)* | Plays the incoming chord with `diff` subtracted from its velocity (minimum 0). |
| `REPEAT_LAST_CHORD` | *(none)* | Re-plays the last chord this Musician played (`getMyLastChord()`). |
| `SET_LAST_CHORD` | *(none)* | Stores the incoming chord as the Musician's last-played chord without playing it. |

### Procedural & Random Generation

| Action `type` | Parameters (`params`) | Behavior |
| :--- | :--- | :--- |
| `PLAY_PSEUDO_RANDOM_CHORD` | `spread` *(int)*, `num` *(int)*, `numNotes` *(int)* | Computes a deterministic value in `[0, spread)`; if `<= num`, plays a random chord of `numNotes` notes in the current key, otherwise rests. |
| `PLAY_PSEUDO_RANDOM_CHORD_FLAVOR` | `spread` *(int)*, `num` *(int)*, `flavor` *(String)* | Computes a deterministic value in `[0, spread)`; if `<= num`, plays a triad of the specified `ChordFlavor` (`MAJOR`, `MINOR`, `DIMINISHED`, `AUGMENTED`), otherwise rests. |
| `PLAY_RANDOM_CHORD` | `dice` *(String)*, `num` *(int)*, `maxNotesDiceDescription` *(String)* | Rolls `dice`; if the result is `<= num`, rolls `maxNotesDiceDescription` for the chord size and plays random notes in the current key. |

### Timing, Queue & Mute Control

| Action `type` | Parameters (`params`) | Behavior |
| :--- | :--- | :--- |
| `REST_ONE_TICK` | *(none)* | Emits a rest for 1 tick. |
| `RESET_PLAYED_CHORDS` | *(none)* | Resets the Musician's `chordsIvePlayed` counter to `0`. |
| `CONSUME_MESSAGE` | *(none)* | Pops and discards the next message from the Musician's incoming queue. |
| `MUTE_MUSICIAN` | `musicianId` *(int, optional)* | Mutes the target Musician (defaults to the current Musician if `musicianId` is omitted). |
| `UNMUTE_MUSICIAN` | `musicianId` *(int, optional)* | Unmutes the target Musician (defaults to the current Musician if `musicianId` is omitted). |

---

## Complete YAML Example

```yaml
name: Crescendo Wave
startingState: buildPhase

properties:
  - type: INT
    key: phraseTicks
    label: "Phrase Length (Ticks):"
    defaultValue: 8
    min: 1
    max: 128
    step: 1
  - type: INT
    key: velocityStep
    label: "Velocity Step:"
    defaultValue: 12
    min: 1
    max: 40
    step: 1

states:
  buildPhase:
    type: DELAYED_TRANSITION
    delay: "\$phraseTicks"
    wrappedState: loudStep
    x: 240.0
    y: 220.0

  loudStep:
    type: ALWAYS_TRANSITION
    actions:
      - type: PLAY_CHORD_UP_VELOCITY
        params:
          diff: "\$velocityStep"
    nextState: fadePhase
    x: 520.0
    y: 140.0

  fadePhase:
    type: DELAYED_TRANSITION
    delay: "\$phraseTicks"
    wrappedState: softStep
    x: 800.0
    y: 220.0

  softStep:
    type: ALWAYS_TRANSITION
    actions:
      - type: PLAY_CHORD_DOWN_VELOCITY
        params:
          diff: "\$velocityStep"
    nextState: buildPhase
    x: 520.0
    y: 320.0
```
