package voiceAnalyses;

import nu.xom.Element;

import java.util.Arrays;
import java.util.Objects;

/**
 * This class represents the range analysis for one musical voice. It provides an array that counts for each
 * MIDI pitch how often it occurs in the voice, basically a discrete density function/histogram.
 * @author Axel Berndt
 */
public class PitchHistogram {
    private final Element msmPart;
    private final int[] midiPitches = new int[128];
    private final int hashCode;

    /**
     * constructor, it creates a VoiceRange from an MSM part element and performs the voice range analysis
     * @param msmPart the MSM part element that represents the voice
     * @throws IllegalArgumentException if msmPart is null or not a part element
     */
    public PitchHistogram(Element msmPart) throws IllegalArgumentException {
        if ((msmPart == null) || !msmPart.getLocalName().equals("part"))
            throw new IllegalArgumentException("Argument msmPart must be a <part> element!");

        this.msmPart = msmPart;

        this.analyze();

        this.hashCode = Objects.hash(this.msmPart.getAttributeValue("number"), this.msmPart.getAttributeValue("name"), Arrays.hashCode(this.midiPitches)); // content-based hash
//        this.hashCode = super.hashCode();
    }

    /**
     * perform voice range analysis
     */
    private void analyze() {
        Element score = this.msmPart.getFirstChildElement("dated").getFirstChildElement("score");
        if (score == null)
            return;

        // count how often each MIDI pitch occurs in this part
        for (Element note : score.getChildElements("note")) {
            int midiPitch = (int) Double.parseDouble(note.getAttributeValue("midi.pitch"));
            this.midiPitches[midiPitch]++;
        }
    }

    /**
     * determine whether the provided MSM part element corresponds to this one.
     * @param msmPart
     * @return
     */
    public boolean isSameVoice(Element msmPart) {
        return (msmPart != null)
                && msmPart.getLocalName().equals("part")
                && msmPart.getAttributeValue("number").equals(this.msmPart.getAttributeValue("number"))
//                && msmPart.getAttributeValue("midi.channel").equals(this.msmPart.getAttributeValue("midi.channel"))
//                && msmPart.getAttributeValue("midi.port").equals(this.msmPart.getAttributeValue("midi.port"))
                && msmPart.getAttributeValue("name").equals(this.msmPart.getAttributeValue("name"));
    }

    /**
     * add the provided voice range analysis to this one
     * @param other
     */
    public void add(PitchHistogram other) {
        for (int i = 0; i < this.midiPitches.length; i++) {
            this.midiPitches[i] += other.midiPitches[i];
        }
    }

    /**
     * get an identifying String for this voice
     * @return voice number and name
     */
    public String getVoice() {
        return this.msmPart.getAttributeValue("number") + " " + this.msmPart.getAttributeValue("name");
    }

    /**
     * get the analysis result as arrays of MIDI pitch counts;
     * e.g., midiPitches[60] = 42 means that the pitch C4 occurs 42 times in the voice;
     * it basically a discrete density function/histogram
     * @param midiPitches
     * @return a clone of the internal data to ensure immutability from outside
     */
    public int[] getMidiPitches(int[] midiPitches) {
        return this.midiPitches.clone();
    }

    /**
     * equality comparison
     * @param obj the reference object with which to compare.
     * @return true only if the voice is the same and the ranges are equal
     */
    @Override
    public boolean equals(Object obj) {
        if ((obj == null) || (obj.getClass() != this.getClass()))
            return false;

        PitchHistogram other = (PitchHistogram) obj;
        if (!isSameVoice(other.msmPart))
            return false;

        return Arrays.equals(this.midiPitches, other.midiPitches);
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
        return this.msmPart.getAttributeValue("number") + " " + this.msmPart.getAttributeValue("name") + ": " + Arrays.toString(this.midiPitches);
    }
}
