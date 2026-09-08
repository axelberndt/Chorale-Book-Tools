package voiceAnalyses;

import meico.msm.Msm;
import nu.xom.Element;

import java.util.HashMap;

/**
 * This class analyzes the voice ranges in an MSM. It is a list of part-specific voice ranges.
 * @author Axel Berndt
 */
public class PitchHistograms extends HashMap<String, PitchHistogram> {
    /**
     * constructor
     */
    public PitchHistograms() {
        super();
    }

    /**
     * Perform a voice range analysis on the provided MSM.
     * @param msm
     * @return
     */
    public static PitchHistograms analyze(Msm msm) {
        if (msm == null)
            return null;

        PitchHistograms result = new PitchHistograms();

        for (Element part : msm.getParts()) {
            PitchHistogram vr = new PitchHistogram(part);
            result.put(vr.getVoice(), vr);
        }

        return result;
    }

    /**
     * add another VoiceRanges object to this one
     * @param other
     */
    public void merge(PitchHistograms other) {
        if (other == null)
            return;

        for (String key : other.keySet()) {
            PitchHistogram value = this.get(key);
            if (value == null)
                this.put(key, other.get(key));
            else
                value.add(other.get(key));
        }
    }

    @Override
    public String toString() {
        String out = "";
        for (String key : this.keySet())
            out += this.get(key).toString() + "\n";

        return out;
    }
}
