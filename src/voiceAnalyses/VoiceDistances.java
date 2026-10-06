package voiceAnalyses;

import meico.msm.Msm;
import nu.xom.Element;
import supplementary.PitchInterval;

import java.util.*;

/**
 * This class provides analysis of voice distances.
 * HashMap entries have the form ("voice1name / voice2name", list of VoiceDistance analyses)
 * @author Axel Berndt
 */
public class VoiceDistances extends HashMap<String, ArrayList<VoiceDistance>> {
    /**
     * constructor
     */
    public VoiceDistances() {
        super();
    }

    /**
     * Perform analysis of voice distances.
     * @param msm
     * @return
     */
    public static VoiceDistances analyze(Msm msm) {
        VoiceDistances results = new VoiceDistances();

        // to find out, which parts to compare, we need to get them in a logical order
        TreeMap<Integer, Element> partsInAscendingOrder = new TreeMap<>();
        for (Element part : msm.getParts()) {
            Element score = part.getFirstChildElement("dated").getFirstChildElement("score");
            if ((score == null) || (score.getChildCount() == 0))
                continue;

            partsInAscendingOrder.put(Integer.valueOf(part.getAttributeValue("number")), part);
        }

        // do the analysis
        List<Integer> partNumbers = new ArrayList<>(partsInAscendingOrder.keySet());
        for (int i=0; i < partNumbers.size() - 1; ++i) {
            Element part1 = partsInAscendingOrder.get(partNumbers.get(i));
            Element part2 = partsInAscendingOrder.get(partNumbers.get(i+1));
            VoiceDistance voiceDistance = new VoiceDistance(part1, part2);
            String key = voiceDistance.getVoice1Name() + " / " + voiceDistance.getVoice2Name();

            if (results.containsKey(key)) {
                results.get(key).add(voiceDistance);
            } else {
                ArrayList<VoiceDistance> voiceDistances = new ArrayList<>();
                voiceDistances.add(voiceDistance);
                results.put(key, voiceDistances);
            }
        }

        // analyze frame interval (between highest and lowest voice)
        Element highest = partsInAscendingOrder.firstEntry().getValue();
        Element lowest = partsInAscendingOrder.lastEntry().getValue();
        VoiceDistance voiceDistance = new VoiceDistance(highest, lowest);
        String key = voiceDistance.getVoice1Name() + " / " + voiceDistance.getVoice2Name();

        if (results.containsKey(key)) {
            results.get(key).add(voiceDistance);
        } else {
            ArrayList<VoiceDistance> voiceDistances = new ArrayList<>();
            voiceDistances.add(voiceDistance);
            results.put(key, voiceDistances);
        }

        return results;
    }

    /**
     * add another VoiceDistances object to this one
     * @param other
     */
    public void merge(VoiceDistances other) {
        if (other == null)
            return;

        for (String key : other.keySet()) {
            if (this.containsKey(key))
                this.get(key).addAll(other.get(key));
            else
                this.put(key, other.get(key));
        }
    }

    /**
     * print voice distances statistics
     * @return
     */
    public String printStatistics() {
        String result = "";
        for (String key : this.keySet()) {
            HashMap<PitchInterval, Integer> pitchIntervalCounts = new HashMap<>();
            for (VoiceDistance voiceDistance : this.get(key)) {
                for (Map.Entry<Double, PitchInterval> entry : voiceDistance.entrySet()) {
                    PitchInterval pitchInterval = entry.getValue();
                    if (pitchIntervalCounts.containsKey(pitchInterval))
                        pitchIntervalCounts.put(pitchInterval, pitchIntervalCounts.get(pitchInterval) + 1);
                    else
                        pitchIntervalCounts.put(pitchInterval, 1);
                }
            }
            result += key + ": " + pitchIntervalCounts.toString() + "\n";
        }
        return result;
    }
}
