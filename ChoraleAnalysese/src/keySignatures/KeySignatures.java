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
public class KeySignatures extends HashMap<KeySignature, SortedSet<String>> {
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

        TreeSet<String> findThis = new TreeSet<>(List.of("scoreDef", "staffDef", "layerDef", "keySig"));    // key signature information can be found only in these elements
        TreeSet<String> stopHere = new TreeSet<>(List.of("section"));                                 // we do not check for key signatures in the musical text, only at the beginning in the initial scoreDef
        List<Element> candidates = Supplementary.depthFirstSearch(mdivs.getFirst(), findThis, stopHere);    // we check only the first mdiv, others are only verses with variants
        for (Element candidate : candidates) {
            KeySignature keySignature = KeySignature.fromMei(candidate);
            if ((keySignature != null) && !keySignature.isEmpty()) {    // we return the first key signature that has not just null in it
                keySignatures.put(keySignature, new TreeSet<>(List.of(mei.getFile().getName())));
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

        KeySignatures keySignatures = new KeySignatures();

        if  (keySignatureMap == null) {
            KeySignature keySignature = KeySignature.fromMsm(msm.getRootElement());     // the root element is, of course, no <keySignature> element; this here enforces that also an MPM with no <keySignatureMap> gets processed; this is usually the case, if the <keySignatureMap> is empty (e.g. C major, D dorian etc.).
            keySignatures.put(keySignature, new TreeSet<>(List.of(msm.getFile().getName())));
            return keySignatures;
        }

        for (Element ks :  keySignatureMap.getChildElements()) {
            KeySignature keySignature = KeySignature.fromMsm(ks);
            if ((keySignature != null) && !keySignature.isEmpty()) {
                keySignatures.put(keySignature, new TreeSet<>(List.of(msm.getFile().getName())));
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
        for (KeySignature key : other.keySet()) {
            if (this.containsKey(key))
                this.get(key).addAll(other.get(key));
            else
                this.put(key, other.get(key));
        }
    }

    /**
     * Match the key signature that is encoded in MEI to the key signature that is automatically analyzed from the provided MSM.
     * This method can be used to identify key signature-related encoding errors in the MEI.
     * @param mei the MEI to be analyzed
     * @param msm the MSM should correspond with the MEI
     * @return true if the result is identical, else false
     */
    public static boolean match(Mei mei, Msm msm) {
        KeySignatures keySigsMei = KeySignatures.analyze(mei);
        KeySignatures keySigsMsm = KeySignatures.analyze(msm);

        KeySignature ksMei = keySigsMei.keySet().iterator().next();
        KeySignature ksMsm = keySigsMsm.keySet().iterator().next();

//        if (!ksMei.equals(ksMsm))
//            System.out.println("Check this: " + mei.getFile().getName() + " ... " + ksMei + " / " + ksMsm);

        return ksMei.equals(ksMsm);
    }

}
