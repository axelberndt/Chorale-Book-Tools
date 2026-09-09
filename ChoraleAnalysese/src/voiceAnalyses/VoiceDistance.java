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
    private final Element voice1;
    private final Element voice2;
    private final int hashCode;

    /**
     * constructor
     * @param msmPart1
     * @param msmPart2
     */
    public VoiceDistance(Element msmPart1, Element msmPart2) {
        super();
        this.voice1 = msmPart1;
        this.voice2 = msmPart2;

        this.hashCode = Objects.hash(this.voice1.getAttributeValue("number"), this.voice1.getAttributeValue("name"), this.voice2.getAttributeValue("number"), this.voice2.getAttributeValue("name")); // content-based hash
//        this.hashCode = super.hashCode();
    }

    /**
     * get an identifying String for this voice1
     * @return voice1 number and name
     */
    public String getVoice1() {
        return this.voice1.getAttributeValue("number") + " " + this.voice1.getAttributeValue("name");
    }

    /**
     * get an identifying String for this voice2
     * @return voice2 number and name
     */
    public String getVoice2() {
        return this.voice2.getAttributeValue("number") + " " + this.voice2.getAttributeValue("name");
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
        return this.voice1.getAttributeValue("number") + " " + this.voice1.getAttributeValue("name") + " / " + this.voice1.getAttributeValue("number") + " " + this.voice1.getAttributeValue("name") + ":\n" + super.toString();
    }
}
