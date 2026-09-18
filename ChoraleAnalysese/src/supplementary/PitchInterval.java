package supplementary;

import java.util.HashMap;

/**
 * This class represents a pitch interval.
 * @author Axel Berndt
 */
public class PitchInterval implements Comparable<PitchInterval> {
    private static final HashMap<PitchInterval, String> HARMONIC_INTERVAL = new HashMap<>(){{   // this follows the MEI data.INTERVAL.HARMONIC definition
        put(new PitchInterval(-1, 0), "d1");
        put(new PitchInterval(0, 0), "P1");
        put(new PitchInterval(1, 0), "A1");
        put(new PitchInterval(0, 1), "d2");
        put(new PitchInterval(1, 1), "m2");
        put(new PitchInterval(2, 1), "M2");
        put(new PitchInterval(3, 1), "A2");
        put(new PitchInterval(2, 2), "d3");
        put(new PitchInterval(3, 2), "m3");
        put(new PitchInterval(4, 2), "M3");
        put(new PitchInterval(5, 2), "A3");
        put(new PitchInterval(4, 3), "d4");
        put(new PitchInterval(5, 3), "P4");
        put(new PitchInterval(6, 3), "A4");
        put(new PitchInterval(6, 4), "d5");
        put(new PitchInterval(7, 4), "P5");
        put(new PitchInterval(8, 4), "A5");
        put(new PitchInterval(7, 5), "d6");
        put(new PitchInterval(8, 5), "m6");
        put(new PitchInterval(9, 5), "M6");
        put(new PitchInterval(10, 5), "A6");
        put(new PitchInterval(9, 6), "d7");
        put(new PitchInterval(10, 6), "m7");
        put(new PitchInterval(11, 6), "M7");
        put(new PitchInterval(12, 6), "A7");
        put(new PitchInterval(0, 6), "A7");     // special case that occurs when the octave is ignored (via modulo)
        put(new PitchInterval(11, 7), "d8");
        put(new PitchInterval(12, 7), "P8");
        put(new PitchInterval(13, 7), "A8");
    }};
    private static final HashMap<String, PitchInterval> HARMONIC_INTERVAL_TO_PITCH_INTERVAL = new HashMap<>(){{   // this follows the MEI data.INTERVAL.HARMONIC definition
        put("d1", new PitchInterval(-1, 0));
        put("P1", new PitchInterval(0, 0));
        put("A1", new PitchInterval(1, 0));
        put("d2", new PitchInterval(0, 1));
        put("m2", new PitchInterval(1, 1));
        put("M2", new PitchInterval(2, 1));
        put("A2", new PitchInterval(3, 1));
        put("d3", new PitchInterval(2, 2));
        put("m3", new PitchInterval(3, 2));
        put("M3", new PitchInterval(4, 2));
        put("A3", new PitchInterval(5, 2));
        put("d4", new PitchInterval(4, 3));
        put("P4", new PitchInterval(5, 3));
        put("A4", new PitchInterval(6, 3));
        put("d5", new PitchInterval(6, 4));
        put("P5", new PitchInterval(7, 4));
        put("A5", new PitchInterval(8, 4));
        put("d6", new PitchInterval(7, 5));
        put("m6", new PitchInterval(8, 5));
        put("M6", new PitchInterval(9, 5));
        put("A6", new PitchInterval(10, 5));
        put("d7", new PitchInterval(9, 6));
        put("m7", new PitchInterval(10, 6));
        put("M7", new PitchInterval(11, 6));
        put("A7", new PitchInterval(12, 6));
        put("d8", new PitchInterval(11, 7));
        put("P8", new PitchInterval(12, 7));
        put("A8", new PitchInterval(13, 7));
    }};
    private static final HashMap<Integer, String> DIATONIC_INTERVAL_NAMES = new HashMap<>(){{
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
     * factory that converts a MEI compliant harmonic interval String
     * (<a href="https://music-encoding.org/guidelines/v5/data-types/data.INTERVAL.HARMONIC.html">...</a>)
     * to a PitchInterval
     * @param inth an MEI compliant @inth String
     * @return PitchInterval instance or null
     */
    public static PitchInterval fromInth(String inth) {
        String[] parts = inth.trim().split(" ");    // there might be more intervals in the string, they are space separated

        // the first entry that can be converted to PitchInterval will be returned
        for (String part : parts) {
            PitchInterval pi = HARMONIC_INTERVAL_TO_PITCH_INTERVAL.get(part);
            if (pi != null)     // success
                return pi;      // return it
        }

        return null;
    }

    /**
     * output the harmonic interval, corresponding to the MEI data.INTERVAL.HARMONIC definition
     * @return MEI harmonic interval string or null if unknown
     */
    public String getHarmonicInterval() {
        return HARMONIC_INTERVAL.get(this);
    }

    /**
     * Output the harmonic interval corresponding to the MEI data.INTERVAL.HARMONIC definition,
     * but ignore the octave. An M2 and an M9 are the same, i.e. M2.
     * @return MEI harmonic interval string or null if unknown
     */
    public String getHarmonicIntervalWithoutOctave() {
        PitchInterval pi = new PitchInterval(this.semitones % 12, this.diatonic % 7);
        return HARMONIC_INTERVAL.get(pi);
    }

    /**
     * output the diatonic interval name
     * @return name string or null if unknown
     */
    public String getDiatonicIntervalName() {
        return DIATONIC_INTERVAL_NAMES.get(this.diatonic);
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

        if ((obj == null) || !this.getClass().equals(obj.getClass()))
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

    /**
     * Modifiers for diatonic intervals, follows MEI data.INTERVAL.HARMONIC definition
     */
    public enum DiatonicIntervalModifier {
        P,  // perfect
        M,  // major
        m,  // minor
        A,  // augmented
        d   // diminished
    }
}
