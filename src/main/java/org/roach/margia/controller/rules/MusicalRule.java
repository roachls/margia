package org.roach.margia.controller.rules;

import java.util.List;
import java.util.Set;

import org.roach.margia.actions.*;
import org.roach.margia.controller.rules.states.NoteBasedTransitionState;

/**
 * A state-machine based agent
 */
public class MusicalRule extends AbstractMusicianRule {

    @Override
    public void initActionsAfterMusicianAssigned() {
        var upFourth = new NoteBasedTransitionState("up 4th")
                .withAction(c -> List.of(new PlayChordUpInterval(musician, c, 5)));
        var downSecond = new NoteBasedTransitionState("down maj 2nd")
                .withAction(c -> List.of(new PlayChordDownInterval(musician, c, 2)));
        var upThird = new NoteBasedTransitionState("up min 3rd")
                .withAction(c -> List.of(new PlayChordUpInterval(musician, c, 3)));
        var directRepeat = new NoteBasedTransitionState("direct repeat")
                .withAction(c -> List.of(new PlayChord(musician, c)));

        super.initActionsAfterMusicianAssigned();

        directRepeat = directRepeat.withStateTransition(Set.of(1, 4, 9), upFourth)
                .withStateTransition(Set.of(6, 9, 2), downSecond).withStateTransition(Set.of(4, 7, 0), upThird)
                .withStateTransition(Set.of(7, 10, 3), upFourth);
        upFourth = upFourth.withStateTransition(Set.of(2, 6, 9), downSecond);
        downSecond = downSecond.withStateTransition(Set.of(4, 7, 0), upThird);
        upThird = upThird.withStateTransition(Set.of(7, 10, 3), upFourth);
        this.state = directRepeat;
        this.startingState = directRepeat;
    }

    @Override
    public String getName() { return "chordbased"; }

    @Override
    public AbstractMusicianRule copy() {
        return new MusicalRule();
    }

}
