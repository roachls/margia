# MARGIA (Music and Rhythm Generating Intelligent Agents)

Intelligent agents with limited intelligence that play music through MIDI and sharing information.

## Demo

[![MARGIA Drums Demo](https://img.youtube.com/vi/56HKEjrpDCI/0.jpg)](https://www.youtube.com/watch?v=56HKEjrpDCI "MARGIA Drums Demo")

## About MARGIA

MARGIA takes the ideas of swarming algorithms such as Conway's Game of Life and others, and applies them to music. A run of MARGIA consists of any number of agents or "musicians". Each Musician may be connected to other Musicians around them. An arrow between two Musicians indicates that one can "hear" the other.

Each Musician produces musical notes via the Java MIDI interface. The program will automatically detect any MIDI interfaces that you have and make them available.

The basic unit of time in MARGIA is the tick, which corresponds to a 16th-note in the current tempo. MARGIA is locked in to a 4/4 time signature.

### Musician rules

Each Musician has a pre-defined set of rules that tells it how to react to the notes that it "hears". Currently these rules are written in Java, and creating new ones is not for the faint of heart, but I do have plans to create a graphical editor to simplify the process.

#### Built-in rulesets

Currently the following rulesets are available, although this will change once the graphical editor is available:

<ul>
	<li>"random". The Musician plays random notes. Optional parameters:
		<ul>
			<li><code>maxChordStringLength (int)</code>: the maximum number of chords to play in a row. Default is 5.</li>
			<li><code>chordSize (int)</code>: number of notes to play in a chord. Default is 1.</li>
			<li><code>restsBetweenChordStrings (int)</code>: number of ticks to rest before starting a new chord string. Default is 1.</li>
		</ul>
	</li>
	<li>"randomFlavor". Just like Random except the chordSize is always 3.
		<ul>
			<li><code>flavor (enum)</code>: the "flavor" of chord to play. Default is MAJOR. Other options are MINOR, DIMINISHED, and AUGMENTED.</li>
		</ul>
	</li>
	<li>StateBasedRule: Musicians have a State that determines their behavior, and they switch states according to the value of a pseudo-random number calculated using the Musician's ID, the current tick number, and the last note the musician played.
		<ul>
			<li>States:
				<ul>
					<li>Direct-repeat: repeat the last note heard</li>
					<li>Up-4th: repeat the last note heard, but up a 4th</li>
					<li>Down-4th: repeat the last note heard, but down a 4th</li>
					<li>Increase velocity: repeat the last note heard, but with increased velocity</li>
					<li>Decrease velocity: repeat the last note heard, but with decreased velocity</li>
					<li>Double-speed: repeat the last note heard, but at half the length (no less than one tick)</li>
					<li>Half-speed: repeat the last note heard, but at double the length (no more than 16 ticks)</li>
				</ul>
			</li>
			<li>Options:
				<ul>
					<li><code>initialTickDelay (int)</code>: Wait this many ticks before playing anything. Default is 0.</li>
					<li><code>sequenceLength (int)</code>: Play this many notes before changing states. Default is 1.</li>
				</ul>
			</li>
		</ul>
	</li>
	<li>Receiver rule: special rule that listens for notes from an external MIDI controller (such as a keyboard) and plays them.</li>
</ul>

#### Settings

All settings are stored in YAML format with a ".margia" extension. Some settings, particularly rule-specific options as listed above, are only editable with an external editor such as Notepad. (This will change).

*Global settings*

<ul>
	<li><code>randomSeed (long)</code>: the global seed used for all random-number generators. Used to allow for random-sounding playbacks that are actually deterministic. Default is 101.</li>
	<li>Music options
		<ul>
			<li><code>tempo (int)</code>: the tempo to play in beats-per-minute. Default is 60.</li>
			<li><code>tempoMinimum (int)</code>: the minimum tempo to allow if using an external MIDI controller to change it. Default is 30.</li>
			<li><code>tempoMaximum (int)</code>: the maximum tempo to allow if using an external MIDI controller to change it. Default is 120.</li>
			<li><code>maxQueueSize (int)</code>: the maximum number of MusicianMessages to allow a musician to have in its queue. Once the queue reaches this size, older messages will be replaced by newer messages. Used to prevent excessive memory usage. Default is 12.</li>
		</ul>
	</li>
	<li>MIDI options
		<ul>
			<li><code>usingExternalMidi (boolean)</code>: determine if the program will send to external MIDI devices. Default is false.</li>
			<li><code>sendingMidiTimecode (boolean)</code>: set this to true if you want MARGIA to act as the timing source for DAWs or external sequencers. Do NOT set both this and <code>usingExternalTiming</code> to true. Default is false.</li>
			<li><code>usingExternalTiming (boolean)</code>: set this to true to have MARGIA's timing and playback controlled by an external MIDI controller or DAW. Default is false. Setting this to true will cause all playback controls to be greyed out. <b>Important</b>: This is currently the only setting that requires a restart of MARGIA if you change it.</li>
			<li><code>tempoController (int, 0-127)</code>: set this to non-zero if you want to allow the tempo to be controlled by an external MIDI controller (such as a knob or fader). The number is the MIDI controller-number of the knob or fader to be used. Default is 0.</li>
			<li><code>sendPanMessage (boolean)</code>: set this to true to have MARGIA send MIDI signals based on a Musician's horizontal position on the screen. Default is true.</li>
			<li><code>panController (int, 0-127)</code>: the MIDI controller number of the horizontal-pan signal to be sent (if sendPanMessage = true). Default is 77.</li>
			<li><code>panWithRelativeLocations (boolean)</code>: if true, horizontal pan information of each Musician will be its position relative to all other Musicians. I.e., the left-most Musician will always have a position of 0, and the right-most Musician will always be 127. If false, position signals will be relative to the screen. I.e., an Musician on the far left side of the screen will be 0, a Musician in the center will be 64, and a Musician on the far right will be 127. Default is true.</li>
			<li><code>sendVerticalPanMessage (boolean)</code>: set this to true to have MARGIA send MIDI signals based on a Musician's verticalal position on the screen. Default is true.</li>
			<li><code>verticalPanController (int, 0-127)</code>: the MIDI controller number of the vertical-pan signal to be sent (if sendVerticalPanMessage = true). Default is 78.</li>
			<li><code>verticalPanWithRelativeLocations (boolean)</code>: if true, verticalal pan information of each Musician will be its position relative to all other Musicians. I.e., the top-most Musician will always have a position of 0, and the bottom-most Musician will always be 127. If false, position signals will be relative to the screen. I.e., an Musician at the top of the screen will be 0, a Musician in the center will be 64, and a Musician on the bottom will be 127. Default is true.</li>
		</ul>
	</li>
	<li>Global UI options
		<ul>
			<li><code>showNumbers (boolean)</code>: set this to true to show labels such as Musician IDs, lock icons, and "listening" icons.</li>
			<li><code>gravity (double)</code>: The amount of gravity pulling Musicians towards the center of the screen. Use 0.0 for no gravity, positive values to pull them towards the center, and negative values to push them away from the center. Default is 5.0.</li>
			<li><code>edgeLength (int)</code>: The length, in pixels that an arrow between two Musicians should try to obtain. Note that this may be overridden by things like gravity. Default is 60.</li>
			<li><code>windSpeed (double)</code>: The speed of the "wind" that causes Musicians to rotate around the center. Positive numbers will result in clockwise rotation, while negative numbers will result in counter-clockwise rotation. However, due to how vectors are calculated, these directions will be reversed if gravity is negative. Also, wind will have no effect if gravity is 0.0. Default is 0.0.</li>
			<li><code>animateBackground (boolean)</code>: Set to true to have the background gradient animated. Default is false.</li>
		</ul>
	</li>
	<li>Musician-specific UI options
		<ul>
			<li><code>locked (boolean)</code>: true means that the musician is locked in place (i.e., not affected by gravity, wind, or the push/pull of other Musicians. Default is false.</li>
			<li><code>radius (int)</code>: the size of the Musician on the screen, in pixels. Changing this also changes the "mass" of the Musician, meaning that it will have a greater tendency to pull other agents towards itself, and it will also be less affected by the pull of other agents (inverse-square law). Default is 20.</li>
			<li><code>position (x/y doubles)</code>: the position of the Musician on the screen, in pixels and fractions of pixels. Note that if a Musician's position is not locked, this will almost certainly change immediately.</li>
		</ul>
	</li>
	<li>Musician-specific Music/MIDI options
		<ul>
			<li><code>id (int)</code>: the unique ID of a Musician. This cannot be changed in the UI. If you edit a YAML file directly, ensure that all IDs are unique.</li>
			<li><code>busName (String)</code>: the name of the MIDI bus that this Musician should send over. The available options will depend on which external busses are available at runtime. The default is "All Available Busses".</li>
			<li><code>channel (int, 1-16)</code>: the MIDI channel that this Musician should send on. The default is 0. (NOTE: Java uses 0-based channels 0-15, but in the YAML file and Options window it will always be 1-based 1-16).</li>
			<li><code>peerIds (List of Integers)</code>: a list of IDs of connected Musicians. Translates into an arrow on the screen.</li>
			<li>keyName (String)</code>: The name of the set of notes that will be available to this musician. Default is "Chromatic" meaning that all notes are available. For more options, see the <code>Key</code> class.</li>
			<li>range: the range of notes available to a Musician.
				<ul>
					<li><code>min (int, 0-127)</code>: the minimum MIDI note available to the Musician. Default is 48.</li>
					<li><code>max (int, 0-127)</code>: the maximum MIDI note available to the Musician. Default is 92. The max must be greater than the min. NOTE: some external instruments, such as Drum Machines, only accept notes in a small range, typically 36-47.)</li>
				</ul>
			</li>
			<li><code>muted (boolean)</code>: Set to true to "mute" a Musician. Muted musicians may still be heard by each other, but they will not actually send MIDI signals. Default is true for new Musicians.</li>
			<li><code>listening (boolean)</code>: Set to true to have a Musician listen to its connected peers, false to have it ignore them. Default is true.</li>
		</ul>
	</li>
</ul>
			
### Setup

These are the steps I used to test MARGIA on Windows 11. I have not tested MARGIA on any other operating system (sorry). I have tested with the Reason DAW (my DAW of choice) and Ableton, as well as VCV Rack.

1. Download and install [loopMIDI](https://www.tobias-erichsen.de/software/loopmidi.html)
1. Run loopMIDI
	<ol type="a">
		<li>Click the + to add the default loopMIDI Port 1</li>
		<li>Close loopMIDI (if desired)</li>
	</ol>
1. Download and install [MIDIMapper Configurator](https://coolsoft.altervista.org/en/midimapper)
	<ol type="a">
		<li>Set the default port to 'loopMIDI Port'</li>
		<li>Apply and close</li>
	</ol>

If using Windows internal MIDI, stop. To use with the Reason DAW, continue:

1. In Reason:
	<ol type="a">
	<li>Open Edit -> Preferences -> Other Controls; change Bus A to "loopMIDI Port"</li>
	<li>Expand Hardware Interface</li>
	<li>Select Advance MIDI</li>
	<li>Under "Advanced MIDI Device" for the channel that Java will be sending events to, select the instrument that events should be routed to for each MIDI channel</li>
	</ol>
	
### Running:

I don't currently have a downloadable release of MARGIA because it is still under active development. To run:

<ol>
	<li>Ensure that you ahve Java 25 available</li>
	<li>Clone this repository</li>
	<li>Run the class <code>org.roach.margia.Main</code></li>
	<li>Optionally use the command-line option <code>--file [filename]</code></li>
</ol>

Many things are still hard-coded in the main method that I'd like to be able to export to preferences.

### The GUI

- Each Musician is represented as a colored circle that starts black but glows when it is playing a note. The hue of
  the Musician is the note being played by the Musician (0-127), normalized to the range that the Musician can play. The
  saturation of the Musician's color is controlled by the velocity of the note being played (0-127). The brightness
  will start at 1.0f and decrease to 0.0f over the course of the note's duration.
- Musicians that are "muted" (i.e., they can only be heard by connected Musicians, not by us) will be "grayed out"
- Lines will glow in the color of the note emitted by the Musician.