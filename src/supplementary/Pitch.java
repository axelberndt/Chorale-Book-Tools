package supplementary;

import meico.mei.Helper;
import nu.xom.Element;

import java.util.Objects;

/**
 * This class represents a musical pitch.
 * @author Axel Berndt
 */
public class Pitch implements Comparable<Pitch> {
//    public static final EnumMap<PitchName, Integer> PITCH_NAME_TO_INTEGER = new EnumMap<>(PitchName.class){{
//        put(PitchName.c, 0);
//        put(PitchName.d, 1);
//        put(PitchName.e, 2);
//        put(PitchName.f, 3);
//        put(PitchName.g, 4);
//        put(PitchName.a, 5);
//        put(PitchName.b, 6);
//    }};       // can be replaced by pitchName.ordinal()

    public final int midi;
    public final PitchName pitchName;
    public final Accidental accidental;
    public final int octave;
    private final int hashCode;

    /**
     * default constructor, creates a middle C
     */
    public Pitch() {
        this.midi = 60;
        this.pitchName = PitchName.c;
        this.accidental = Accidental.n;
        this.octave = 4;
        this.hashCode = this.makeHashCode();
    }

    /**
     * constructor
     * @param midi
     * @param pitchName
     * @param accidental
     * @param octave
     */
    public Pitch(int midi, PitchName pitchName, Accidental accidental, int octave) {
        this.midi = midi;
        this.pitchName = pitchName;
        this.accidental = accidental;
        this.octave = octave;
        this.hashCode = this.makeHashCode();
    }

    /**
     * constructor with MIDI input
     * @param midi
     */
    public Pitch(int midi) {
        this.midi = midi;

        String[] pnameAccid = new String[2];
        Helper.midi2PnameAndAccid(true, midi, pnameAccid);
        this.pitchName = PitchName.valueOf(pnameAccid[0].toLowerCase());
        this.accidental = Accidental.valueOf(Helper.accidDecimal2String(Double.parseDouble(pnameAccid[1])));
        this.octave = Helper.midi2Octave(midi);
        this.hashCode = this.makeHashCode();
    }

    /**
     * constructor with pitch name, accidental and octave input
     * @param pitchName
     * @param accidental
     * @param octave
     */
    public Pitch(PitchName pitchName, Accidental accidental, int octave) {
        this.pitchName = pitchName;
        this.accidental = accidental;
        this.octave = octave;
        this.midi = (int)Helper.pname2midi(pitchName.toString() + Helper.accidString2decimal(accidental.toString())) + (octave * 12);
        this.hashCode = this.makeHashCode();
    }

    /**
     * constructor with MSM note input
     * @param msmNote
     */
    public Pitch(Element msmNote) {
        this.pitchName = PitchName.valueOf(msmNote.getAttributeValue("pitchname"));
        this.accidental = Accidental.valueOf(Helper.accidDecimal2String(Double.parseDouble(msmNote.getAttributeValue("accidentals"))));
        this.octave = (int) Double.parseDouble(msmNote.getAttributeValue("octave"));
        this.midi = (int) Double.parseDouble(msmNote.getAttributeValue("midi.pitch"));
        this.hashCode = this.makeHashCode();
    }

    /**
     * compute hasCode, so this class can be used in HashMaps etc.
     * @return
     */
    private int makeHashCode() {
        return Objects.hash(this.pitchName, this.accidental, this.octave);
    }

    /**
     * make Pitch comparable
     * @param otherPitch the object to be compared.
     * @return
     */
    @Override
    public int compareTo(Pitch otherPitch) {
        if (this.midi < otherPitch.midi)
            return -1;
        if (this.midi > otherPitch.midi)
            return 1;
        else
            return 0;
    }

    /**
     * equality comparison
     * @param obj the reference object with which to compare
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj == null || getClass() != obj.getClass())
            return false;

        Pitch other = (Pitch) obj;
        return (this.pitchName == other.pitchName) && (this.accidental == other.accidental) && (this.octave == other.octave);
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
     * String representation of Pitch
     * @return
     */
    @Override
    public String toString() {
        return "Pitch{" + this.pitchName.toString() + this.accidental.toString() + " " + this.octave + " MIDI pitch=" + this.midi + "}";
    }
}
