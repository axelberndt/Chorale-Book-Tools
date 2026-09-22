package voiceAnalyses;

import meico.supplementary.KeyValue;
import nu.xom.Element;
import supplementary.Pitch;
import supplementary.PitchInterval;
import msm.elements.maps.Score;

import java.util.*;

/**
 * This class represents the sequence of intervals between two musical voices.
 * The format of the TreeMap entries is (date, PitchInterval), so it will later be possible to
 * analyze the sequence and the timings.
 * @author Axel Berndt
 */
public class VoiceDistance extends TreeMap<Double, PitchInterval> {
    private final Score msmScore1;
    private final Score msmScore2;
    private final String voice1Name;
    private final String voice2Name;
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

        this.voice1Name = msmPart1.getAttributeValue("number") + " " + msmPart1.getAttributeValue("name");
        this.voice2Name = msmPart2.getAttributeValue("number") + " " + msmPart2.getAttributeValue("name");

        this.analyze();

        this.hashCode = Objects.hash(this.voice1Name, this.voice2Name, super.hashCode()); // content-based hash
//        this.hashCode = super.hashCode();
    }

    /**
     * perform the analysis of both voices
     */
    private void analyze() {
        // go through both scores and, at each date of a note onset, get the pitches
        TreeSet<Double> dates = new TreeSet<>();    // we also keep track of the dates already gathered, so we don't do it twice when checking the other voice

        // start checking from the perspective of voice1, i.e. for each note onset in voice1 we compute the PitchInterval to voice2
        for (KeyValue<Double, Element> note : this.msmScore1.getAllElementsOfType("note")) {
            double date = note.getKey();
            if (!dates.add(date))   // if the date is already contained in the set
                continue;           // go on with the next

            TreeSet<Pitch> pitches1 = this.msmScore1.getPitchesAt(date);
            if (pitches1.isEmpty())
                continue;

            TreeSet<Pitch> pitches2 = this.msmScore2.getPitchesAt(date);
            if (pitches2.isEmpty())
                continue;

            this.put(date, this.getPitchInterval(pitches1, pitches2));
        }

        // now check from the perspective of voice2, i.e. for each note onset in voice2 at a date that was not covered above, we compute and add the PitchInterval to the TreeMap
        for (KeyValue<Double, Element> note : this.msmScore2.getAllElementsOfType("note")) {
            double date = note.getKey();
            if (!dates.add(date))   // if the date is already contained in the set
                continue;           // go on with the next

            TreeSet<Pitch> pitches1 = this.msmScore1.getPitchesAt(date);
            if (pitches1.isEmpty())
                continue;

            TreeSet<Pitch> pitches2 = this.msmScore2.getPitchesAt(date);
            if (pitches2.isEmpty())
                continue;

            this.put(date, this.getPitchInterval(pitches1, pitches2));
        }
    }

    /**
     * compute the pitch interval between two voices
     * @param pitches1 supposedly the upper voice's pitches of which the lowest pitch will be taken
     * @param pitches2 supposedly the lower voice's pitches of which the highest pitch will be taken
     * @return the pitch interval between the two voices or null if one of the voices has no pitches
     */
    private PitchInterval getPitchInterval(TreeSet<Pitch> pitches1, TreeSet<Pitch> pitches2) {
        if (pitches1.isEmpty() || pitches2.isEmpty())
            return null;

        Pitch upper = pitches1.first();
        Pitch lower = pitches2.last();
        return new PitchInterval(upper, lower);
    }

    /**
     * get an identifying String for this msmPart1
     * @return msmPart1 number and name
     */
    public String getVoice1Name() {
        return this.voice1Name;
    }

    /**
     * get an identifying String for this msmPart2
     * @return msmPart2 number and name
     */
    public String getVoice2Name() {
        return this.voice2Name;
    }

    /**
     * get the interval counts
     * @return
     */
    public TreeMap<PitchInterval, Integer> getIntervalCounts() {
        TreeMap<PitchInterval, Integer> results = new TreeMap<>();
        for (Map.Entry<Double, PitchInterval> entry : this.entrySet()) {
            PitchInterval interval = entry.getValue();
            if (results.containsKey(interval))
                results.put(interval, results.get(interval) + 1);
            else
                results.put(interval, 1);
        }
        return results;
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
        if (!this.getVoice1Name().equals(other.getVoice1Name()) || !this.getVoice2Name().equals(other.getVoice2Name()))
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
        return "\n" + this.voice1Name + " / " + this.voice2Name + ":\n" + super.toString();
    }
}
