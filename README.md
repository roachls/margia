# MARGIA (Music and Rhythm Generating Intelligent Agents)

Intelligent agents with limited intelligence that play music through MIDI and sharing information.

### Setup

To run on Windows 10+, follow these steps:

1. Download and install loopMIDI (https://www.tobias-erichsen.de/software/loopmidi.html)
1. Run loopMIDI
	a. Click the + to add the default loopMIDI Port 1
	b. Close loopMIDI (if desired)
1. Download and install MIDIMapper Configurator
	a. Set the default port to 'loopMIDI Port'
	b. Apply and close

If using Windows internal MIDI, stop. To use with the Reason DAW, continue:

1. In Reason:
	<ol type="a">
	<li>Open Edit -> Preferences -> Other Controls; change Bus A to "loopMIDI Port"</li>
	<li>Expand Hardware Interface</li>
	<li>Select Advance MIDI</li>
	<li>Under "Advanced MIDI Device" for the channel that Java will be sending events to, select the instrument
	   that events should be routed to for each MIDI channel</li>
	</ol>
	
### Running:

There are currently several main methods, all in the org.roach.margia.mains package. The one I've done the most
work on is "MainStateBased64". To run from Eclipse, run MainStateBased64 with the following command-line args:

- ```-r 8``` (for now required to be exactly this)
- ```-c 8``` (for now required to be exactly this)
- ```-ext``` (to run with external DAW instruments)
- ```-t <initial tempo>```
- ```-seq <starting sequence length>``` (default is 20)
- ```-seed <random seed>``` (default is 101)
- ```-channels <num channels>``` (only if using external DAW, this is the number of external MIDI channels to rotate notes around)

Many things are still hard-coded in the main method that I'd like to be able to export to preferences.

### The GUI

- Each agent is represented as a colored circle that starts black but glows when it is playing a note. The hue of
  the agent is the note being played by the agent (0-127), normalized to the range that the agent can play. The
  saturation of the agent's color is controlled by the velocity of the note being played (0-127). The brightness
  will start at 1.0f and decrease to 0.0f over the course of the note's duration.
- Agents that are "muted" (i.e., they can only be heard by connected agents, not by us) will be "grayed out"
- Lines will glow in the color of the note emitted by the agent.