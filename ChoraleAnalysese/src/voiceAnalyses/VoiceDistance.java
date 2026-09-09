package voiceAnalyses;

import nu.xom.Element;

import java.util.Objects;
import java.util.TreeMap;

/**
 * This class represents the histogram of intervals between two musical voices.
 * TreeMap entries have the form (semitone interval, number of occurrences)
 * @author Axel Berndt
 */
public class VoiceDistance extends TreeMap<Integer, Integer> {
    private final Element msmPart1;
    private final Element msmPart2;
    private final int hashCode;

    /**
     * constructor
     * @param msmPart1
     * @param msmPart2
     */
    public VoiceDistance(Element msmPart1, Element msmPart2) {
        if ((msmPart1 == null) || (msmPart2 == null) || !msmPart1.getLocalName().equals("part") || !msmPart2.getLocalName().equals("part"))
            throw new IllegalArgumentException("A VoiceDistance object requires two non-null <part> elements!");

        super();
        this.msmPart1 = msmPart1;
        this.msmPart2 = msmPart2;

        this.analyze();

        this.hashCode = Objects.hash(this.msmPart1.getAttributeValue("number"), this.msmPart1.getAttributeValue("name"), this.msmPart2.getAttributeValue("number"), this.msmPart2.getAttributeValue("name")); // content-based hash
//        this.hashCode = super.hashCode();
    }

    /**
     * perform the analysis of both voices
     */
    private void analyze() {
        // TODO: ...
    }

    /**
     * get an identifying String for this msmPart1
     * @return msmPart1 number and name
     */
    public String getVoice1() {
        return this.msmPart1.getAttributeValue("number") + " " + this.msmPart1.getAttributeValue("name");
    }

    /**
     * get an identifying String for this msmPart2
     * @return msmPart2 number and name
     */
    public String getVoice2() {
        return this.msmPart2.getAttributeValue("number") + " " + this.msmPart2.getAttributeValue("name");
    }

    /**
     * equality comparison
     * @param obj the reference object with which to compare.
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if ((obj == null) || (obj.getClass() != this.getClass()))
            return false;

        VoiceDistance other = (VoiceDistance) obj;

        // if the voices are different (they can be exchanged, though)
        if (!this.getVoice1().equals(other.getVoice1()) || !this.getVoice2().equals(other.getVoice2()))
            return false;

        // check for equality of the histogram
        return super.equals(obj);
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
     * String output
     * @return
     */
    @Override
    public String toString() {
        return this.msmPart1.getAttributeValue("number") + " " + this.msmPart1.getAttributeValue("name") + " / " + this.msmPart1.getAttributeValue("number") + " " + this.msmPart1.getAttributeValue("name") + ":\n" + super.toString();
    }
}
