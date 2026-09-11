package supplementary;

import java.util.HashMap;

/**
 * This class represents a pitch interval.
 */
public class PitchInterval implements Comparable<PitchInterval> {
    public static final HashMap<Integer, String> DIATONIC_INTERVAL_NAMES = new HashMap<>(){{
        put(0, "unison");
        put(1, "second");
        put(2, "third");
        put(3, "fourth");
        put(4, "fifth");
        put(5, "sixth");
        put(6, "seventh");
        put(7, "octave");
        put(8, "ninth");
        put(9, "tenth");
        put(10, "eleventh");
        put(11, "twelfth");
        put(12, "thirteenth");
        put(13, "fourteenth");
        put(14, "fifteenth");
        put(15, "sixteenth");
        put(16, "seventeenth");
        put(17, "eighteenth");
        put(18, "nineteenth");
        put(19, "twentieth");
        put(20, "twenty-first");
        put(21, "twenty-second");
        put(22, "twenty-third");
        put(23, "twenty-fourth");
        put(24, "twenty-fifth");
        put(25, "twenty-sixth");
        put(26, "twenty-seventh");
        put(27, "twenty-eighth");
        put(28, "twenty-ninth");
        put(29, "thirtieth");
        put(30, "thirty-first");
        put(31, "thirty-second");
    }};

    public final int semitones;
    public final int diatonic;      // to read this musically, add 1, so that 0->1=prime, 1->2=second, 2->3=third, 3->4=quart, ...
    private final int hashCode;

    /**
     * constructor
     * @param semitones
     * @param diatonic
     */
    public PitchInterval(int semitones, int diatonic) {
        this.semitones = semitones;
        this.diatonic = diatonic;
        this.hashCode = this.makeHashCode();
    }

    /**
     * constructor that computes the pitch interval by pitch1 - pitch2
     * @param pitch1
     * @param pitch2
     */
    public PitchInterval(Pitch pitch1, Pitch pitch2) {
        this.semitones = pitch1.midi - pitch2.midi;
        this.diatonic = (pitch1.pitchName.ordinal() + (pitch1.octave * 7)) - (pitch2.pitchName.ordinal() + (pitch2.octave * 7));
        this.hashCode = this.makeHashCode();
//        System.out.println(pitch1 + ", " + pitch2 + ", " + this.toString());
    }

    /**
     * computes the hash code
     * @return hash code
     */
    private int makeHashCode() {
        return this.diatonic * 1000 + this.semitones;
    }

    /**
     * make PitchInterval comparable
     * @param otherPitchInterval the object to be compared.
     * @return
     */
    @Override
    public int compareTo(PitchInterval otherPitchInterval) {
        if (this.semitones < otherPitchInterval.semitones)
            return -1;
        if (this.semitones > otherPitchInterval.semitones)
            return 1;
        else
            return 0;
    }

    /**
     * equality comparison
     * @param obj the reference object with which to compare.
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj == null || getClass() != obj.getClass())
            return false;

        PitchInterval other = (PitchInterval) obj;
        return (this.semitones == other.semitones) && (this.diatonic == other.diatonic);
    }

    /**
     * Hash code output
     * @return
     */
    @Override
    public int hashCode() {
        return this.hashCode;
    }

    /**
     * String representation of PitchInterval
     * @return
     */
    @Override
    public String toString() {
        return "PitchInterval{semi=" + semitones + ", diat=" + diatonic + "}";
    }
}
