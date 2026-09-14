package voiceAnalyses;

import meico.supplementary.KeyValue;
import nu.xom.Element;
import supplementary.PitchInterval;
import supplementary.Score;

import java.util.ArrayList;
import java.util.TreeMap;

/**
 * This class implements the melodic interval analysis for an MSM part element.
 * The format is (interval, count)
 * @author Axel Berndt
 */
public class MelodicIntervals extends TreeMap<PitchInterval, Integer> {
    private final Score msmScore;
//    private final int hashCode;

    /**
     * constructor
     * @param msmPart
     */
    public MelodicIntervals(Element msmPart) {
        if ((msmPart == null) || !msmPart.getLocalName().equals("part"))
            throw new IllegalArgumentException("A VoiceDistance object requires two non-null <part> elements!");

        Element s = msmPart.getFirstChildElement("dated").getFirstChildElement("score");
        if ((s == null) || (s.getChildCount() == 0))
            throw new IllegalArgumentException("The MSM part does not contain a non-empty <score>!");

        super();

        this.msmScore = Score.createScore(s);

        this.analyze();

//        this.hashCode = Objects.hash(this.voiceName, super.hashCode()); // content-based hash
    }

    /**
     * perform the analysis
     */
    private void analyze() {
        ArrayList<KeyValue<Double, PitchInterval>> intervalSequence = this.msmScore.getMelodicIntervalSequence(false);

        // here, we are interested only in the intervals, not in their timings
        for (KeyValue<Double, PitchInterval> kv : intervalSequence) {
            PitchInterval interval = kv.getValue();
            if (this.containsKey(interval))
                this.put(interval, this.get(interval) + 1);
            else
                this.put(interval, 1);
        }
    }

    /**
     * merge the interval counts of another MelodicIntervals object into this one
     * @param other
     */
    public void merge(MelodicIntervals other) {
        for (PitchInterval interval : other.keySet()) {
            if (this.containsKey(interval))
                this.put(interval, this.get(interval) + other.get(interval));
            else
                this.put(interval, other.get(interval));
        }
    }
}
