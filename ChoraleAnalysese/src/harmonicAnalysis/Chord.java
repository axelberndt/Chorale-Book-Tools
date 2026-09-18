package harmonicAnalysis;

import supplementary.PitchInterval;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/**
 * This class represents a musical chord. Here, chords are defined in their most generic sense
 * as stacks of intervals.
 * @author Axel Berndt
 */
public class Chord extends TreeSet<PitchInterval> {
    private final int hashCode;

    /**
     * Constructs a chord from a list of pitch intervals.
     * @param pitchIntervals the pitch intervals that make up the chord
     */
    public Chord(List<PitchInterval> pitchIntervals) {
        super(pitchIntervals);

        String hc = "";
        for (PitchInterval pitchInterval : this)
            hc += " " + ((pitchInterval.semitones * 1000) + pitchInterval.diatonic);

        this.hashCode = Objects.hash(hc.substring(1));
    }

    /**
     * factory that converts an MEI compliant harmonic interval String
     * (<a href="https://music-encoding.org/guidelines/v5/data-types/data.INTERVAL.HARMONIC.html">...</a>)
     * to a Chord
     * @param inth an MEI compliant @inth String
     * @return
     */
    public static Chord fromInth(String inth) {
        ArrayList<PitchInterval> list = new ArrayList<>();
        String[] parts = inth.trim().split(" ");    // there might be more intervals in the string, they are space separated

        for (String part : parts) {
            PitchInterval pi = PitchInterval.fromInth(part);
            if (pi != null)
                list.add(pi);
        }

        return new Chord(list);
    }

    /**
     * equality check
     * @param obj object to be compared for equality with this set
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if ((obj == null) || !this.getClass().equals(obj.getClass()))
            return false;

        Chord object = (Chord) obj;
        if ((this.size() != object.size()) || !object.containsAll(this))
            return false;

        return true;
    }

    /**
     * String output
     * @return
     */
    @Override
    public String toString() {
        return super.toString();
    }

    /**
     * String output that is compatible with the MEI data.INTERVAL.HARMONIC schema.
     * @return
     */
    public String toInthString() {
        String out = "";
        for (PitchInterval pitchInterval : this)
            out += " " + pitchInterval.getHarmonicInterval();
        return out.substring(1);
    }

    /**
     * Hash code output
     * @return
     */
    @Override
    public int hashCode() {
        return this.hashCode;
    }
}
