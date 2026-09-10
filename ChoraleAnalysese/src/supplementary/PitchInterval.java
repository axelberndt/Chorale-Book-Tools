package supplementary;

/**
 * This class represents a pitch interval.
 */
public class PitchInterval {
    public final int semitones;
    public final int diatonic;
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
        this.semitones = pitch2.midi - pitch1.midi;
        this.diatonic = pitch2.pitchName.ordinal() - pitch1.pitchName.ordinal() + ((pitch1.octave - pitch2.octave) * 8);
        this.hashCode = this.makeHashCode();
    }

    /**
     * computes the hash code
     * @return hash code
     */
    private int makeHashCode() {
        return this.diatonic * 1000 + this.semitones;
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
        return "PitchInterval{semitones=" + semitones + ", diatonic=" + diatonic + "}";
    }
}
