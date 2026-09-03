package keySignatures;

import meico.mei.Mei;
import meico.msm.Msm;
import nu.xom.Element;
import supplementary.Supplementary;

import java.util.*;

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
        if (mei == null)
            return null;

        ArrayList<Element> mdivs = mei.getAllMdivs();
        if (mdivs.isEmpty())
            return null;

        KeySignatures keySignatures = new KeySignatures();

        TreeSet<String> findThis = new TreeSet<>(Arrays.asList("scoreDef", "staffDef", "layerDef", "keySig"));    // key signature information can be found only in these elements
        TreeSet<String> stopHere = new TreeSet<>(Arrays.asList("section"));                                 // we do not check for key signatures in the musical text, only at the beginning in the initial scoreDef
        List<Element> candidates = Supplementary.depthFirstSearch(mdivs.getFirst(), findThis, stopHere);    // we check only the first mdiv, others are only verses with variants
        for (Element candidate : candidates) {
            KeySignature keySignature = KeySignature.fromMei(candidate);
            if ((keySignature != null) && !keySignature.isEmpty()) {    // we return the first key signature that has not just null in it
                keySignatures.put(keySignature, 1);
                return keySignatures;
            }
        }


        return null;
    }

    /**
     * Perform an analysis of the given MSM object.
     * @param msm
     * @return
     */
    public static KeySignatures analyze(Msm msm) {
        if (msm == null)
            return null;

        // find a non-empty keySignatureMap
        Element keySignatureMap = msm.getGlobal().getFirstChildElement("dated").getFirstChildElement("keySignatureMap");
        if ((keySignatureMap == null) || (keySignatureMap.getChildCount() == 0)) {
            for (Element part : msm.getParts()) {
                keySignatureMap = part.getFirstChildElement("dated").getFirstChildElement("keySignatureMap");
                if ((keySignatureMap != null) && (keySignatureMap.getChildCount() > 0))       // we found a part with a non-empty keySignatureMap
                    break;
            }
        }

        if  (keySignatureMap == null)   // if no non-empty keySignatureMap was found
            return null;                // done

        KeySignatures keySignatures = new KeySignatures();

        for (Element ks :  keySignatureMap.getChildElements()) {
            KeySignature keySignature = KeySignature.fromMsm(ks);
            if ((keySignature != null) && !keySignature.isEmpty()) {
                keySignatures.put(keySignature, 1);
                return keySignatures;
            }
        }

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
