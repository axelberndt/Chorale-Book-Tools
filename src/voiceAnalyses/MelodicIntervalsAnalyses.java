package voiceAnalyses;

import meico.msm.Msm;
import nu.xom.Element;

import java.util.HashMap;

/**
 * This class represents an aggregation of melodic interval analyses.
 * The format is (voiceName, MelodicIntervals)
 * @author Axel Berndt
 */
public class MelodicIntervalsAnalyses extends HashMap<String, MelodicIntervals> {
    /**
     * constructor
     */
    public MelodicIntervalsAnalyses() {
        super();
    }

    /**
     * perform the analysis on the provided MSM and merge the result into this aggregation
     * @param msm
     */
    public void analyze(Msm msm) {
        for (Element part : msm.getParts()) {
            Element score = part.getFirstChildElement("dated").getFirstChildElement("score");
            if ((score == null) || (score.getChildCount() == 0))    // we do not analyze empty voices
                continue;

            String voiceName = part.getAttributeValue("number") + " " + part.getAttributeValue("name");
            MelodicIntervals melodicIntervals = new MelodicIntervals(part);
            this.merge(voiceName, melodicIntervals);
        }
    }

    /**
     * merge a melodic interval analysis to this aggregation
     * @param voiceName
     * @param melodicIntervals
     */
    private void merge(String voiceName, MelodicIntervals melodicIntervals) {
        if (!this.containsKey(voiceName)) {         // if the voice is not contained in the aggregation, so far
            this.put(voiceName, melodicIntervals);  // we just add it
            return;
        }

        // if the voice is already present, add the interval counts
        this.get(voiceName).merge(melodicIntervals);
    }

    /**
     * print results
     * @return
     */
    @Override
    public String toString() {
        String out = "";
        for (String voiceName : this.keySet()) {
            out += voiceName + ": " + this.get(voiceName) + "\n";
        }
        return out;
    }
}
