package harmonicAnalysis;

import meico.msm.Msm;

import java.util.HashMap;

/**
 * This class represents a collection of chords and how often they occur.
 */
public class Chords extends HashMap<Chord, Integer> {
    /**
     * constructor
     */
    public Chords() {
        super();
    }

    /**
     * perform an analysis of the given MSM and aggregate it with analysis done so fare
     * @param msm
     * @return
     */
    public Chords analyze(Msm msm) {
        // safety checks
        if (msm == null)
            return null;

        // TODO ...
        return null;
    }

    /**
     * merge another Chords object into this one
     * @param other the other Chords object
     */
    public void merge(Chords other) {
        for (Chord key : other.keySet())
            this.merge(key, other.get(key), Integer::sum);
    }

}
