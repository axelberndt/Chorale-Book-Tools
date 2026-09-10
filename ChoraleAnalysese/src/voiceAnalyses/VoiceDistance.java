package voiceAnalyses;

import nu.xom.Element;
import supplementary.PitchInterval;
import supplementary.Score;

import java.util.ArrayList;
import java.util.Objects;

/**
 * This class represents the sequence of intervals between two musical voices.
 * @author Axel Berndt
 */
public class VoiceDistance extends ArrayList<PitchInterval> {
    private final Score msmScore1;
    private final Score msmScore2;
    private final int hashCode;

    /**
     * constructor
     * @param msmPart1
     * @param msmPart2
     */
    public VoiceDistance(Element msmPart1, Element msmPart2) {
        if ((msmPart1 == null) || (msmPart2 == null) || !msmPart1.getLocalName().equals("part") || !msmPart2.getLocalName().equals("part"))
            throw new IllegalArgumentException("A VoiceDistance object requires two non-null <part> elements!");

        Element s1 = msmPart1.getFirstChildElement("dated").getFirstChildElement("score");
        if ((s1 == null) || (s1.getChildCount() == 0))
            throw new IllegalArgumentException("Part 1 does not contain a non-empty <score>!");

        Element s2 = msmPart2.getFirstChildElement("dated").getFirstChildElement("score");
        if ((s2 == null) || (s2.getChildCount() == 0))
            throw new IllegalArgumentException("Part 2 does not contain a non-empty <score>!");

        super();

        this.msmScore1 = Score.createScore(s1);
        this.msmScore2 = Score.createScore(s2);

        this.analyze();

        this.hashCode = Objects.hash(msmPart1.getAttributeValue("number"), msmPart1.getAttributeValue("name"), msmPart2.getAttributeValue("number"), msmPart2.getAttributeValue("name"), super.hashCode()); // content-based hash
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
    public String getVoiceName1() {
        Element part = (Element)this.msmScore1.getXml().getParent().getParent();
        return part.getAttributeValue("number") + " " + part.getAttributeValue("name");
    }

    /**
     * get an identifying String for this msmPart2
     * @return msmPart2 number and name
     */
    public String getVoiceName2() {
        Element part = (Element)this.msmScore2.getXml().getParent().getParent();
        return part.getAttributeValue("number") + " " + part.getAttributeValue("name");
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
        if (!this.getVoiceName1().equals(other.getVoiceName1()) || !this.getVoiceName2().equals(other.getVoiceName2()))
            return false;

        // check for equality of the interval list
        for (int i = 0; i < this.size(); i++)
            if (!Objects.equals(this.get(i), other.get(i)))
                return false;

        return true;
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
        Element part1 = (Element)this.msmScore1.getXml().getParent().getParent();
        Element part2 = (Element)this.msmScore2.getXml().getParent().getParent();
        return part1.getAttributeValue("number") + " " + part1.getAttributeValue("name") + " / " + part2.getAttributeValue("number") + " " + part2.getAttributeValue("name") + ":\n" + super.toString();
    }
}
