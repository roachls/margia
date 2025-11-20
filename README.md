# MIDI Swarm

Intelligent agents with limited intelligence that act as a swarm by playing music through MIDI and sharing information.

### Setup

1. Download and install loopMIDI
2. Run loopMIDI
	a. Click the + to add the default loopMIDI Port 1
	b. Close loopMIDI (if desired)
3. Download and install MIDIMapper Configurator
	a. Set the default port to 'loopMIDI Port'
	b. Apply and close
4. In Reason:
	a. Open Edit -> Preferences -> Other Controls; change Bus A to "loopMIDI Port"
	b. Expand Hardware Interface
	c. Select Advance MIDI
	d. Under "Advanced MIDI Device" for the channel that Java will be sending events to, select the instrument
	   that events should be routed to
5. In Java code:
	a. Get all MIDI device infos with 'MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();'
	b. Loop through the infos until you find the one with the name "loopMIDI"
	c. Get the MidiDevice with 'MidiDevice device = MidiSystem.getMidiDevice(info)'
	d. Get the Receiver of events with 'Receiver receiver = outputDevice.getReceiver()'
	
You can now send events to the receiver and they should be received by the appropriate Reason Synth. This will allow
you to control up to 16 instruments from Java. If you need more, add another loopMIDI port with a different name and
configure it for Bus B-D in Reason.  