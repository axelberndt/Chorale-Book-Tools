package keySignatures;

import meico.mei.Mei;
import meico.msm.Msm;

import java.util.HashMap;

/**
 * This class analyzes a given MEI/MSM to find out which key signatures are present.
 * @author Axel Berndt
 */
public class KeySignatures extends HashMap<KeySignature, Integer> {
    /**
     * constructor
     */
    public KeySignatures() {
        super();
    }

    /**
     * Perform an analysis of the given MEI object.
     * @param mei
     * @return
     */
    public static KeySignatures analyze(Mei mei) {
        // TODO ...
        return null;
    }

    /**
     * Perform an analysis of the given MSM object.
     * @param msm
     * @return
     */
    public static KeySignatures analyze(Msm msm) {
        // TODO ...
        return null;
    }

    /**
     * merge another KeySignatures object into this one
     * @param other the other KeySignatures object
     */
    public void merge(KeySignatures other) {
        for (KeySignature key : other.keySet())
            this.merge(key, other.get(key), Integer::sum);
    }

}
