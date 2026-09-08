package meterSignatures;

import meico.mei.Mei;
import meico.msm.Msm;
import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import supplementary.Supplementary;

import java.util.*;

/**
 * This class analyzes a given MEI/MSM to find out which meter signatures are present and in how many measures.
 * @author Axel Berndt
 */
public class MeterSignatures extends HashMap<MeterSignature, Integer> {
    /**
     * constructor
     */
    public MeterSignatures() {
        super();
    }

    /**
     * Perform an analysis of the given MSM.  This method does not work for polyphonic meter signatures, i.e., the
     * musical parts have individual meter signatures that differ from other parts!
     * @param msm
     * @return a HashMap of the form (meter signature, number of measures)
     */
    public static MeterSignatures analyze(Msm msm) {
        // safety checks
        if (msm == null)
            return null;

        Element timeSignatureMap = msm.getGlobal().getFirstChildElement("dated").getFirstChildElement("timeSignatureMap");
        if (timeSignatureMap == null) {
            for (Element part : msm.getParts()) {
                timeSignatureMap = part.getFirstChildElement("dated").getFirstChildElement("timeSignatureMap");
                if (timeSignatureMap != null)       // we found a part with a timeSignatureMap
                    break;
            }
        }

        MeterSignatures meterSignatures = new MeterSignatures();    // this will hold the analysis result for this single MSM

        // if we found no timeSignatureMap, add a default one
        if (timeSignatureMap == null) {
            meterSignatures.put(new MeterSignature(0.0, 0), 1); // no meter signature
            return meterSignatures;
        }

        // collect the time signatures
        int ppq = msm.getPPQ();
        for (int ts = 0; ts < timeSignatureMap.getChildCount(); ++ts) {
            Element timeSignature = (Element) timeSignatureMap.getChild(ts);

            // no safety checks needed, as meico generated the data
            double date = Double.parseDouble(timeSignature.getAttributeValue("date"));              // get its date
            MeterSignature meterSignature = MeterSignature.fromMsm(timeSignature);

            // compute the number of measures that follow this time signature
            double duration;
            if (ts + 1 < timeSignatureMap.getChildCount()) {
                // compute duration from the date of the next sibling
                Element nextSibling = (Element) timeSignatureMap.getChild(ts + 1);
                duration = Double.parseDouble(nextSibling.getAttributeValue("date")) - date;
            } else {
                duration = msm.getEndDate() - date;     // compute duration from the end date
            }

            double durationOfOneMeasure = (ppq * 4.0 * meterSignature.numerator) / meterSignature.denominator;
            int numberOfMeasures = (int)(duration / durationOfOneMeasure);

            meterSignatures.merge(meterSignature, numberOfMeasures, Integer::sum);
        }


        return meterSignatures;
    }

    /**
     * Perform an analysis of the given MSMs and cumulate the results. This method does not erase the previous results.
     * So, performing it iteratively on a series of MSMs will produce a cumulative result.
     * @param msms the list of MSMs to analyze
     */
    public void analyze(List<Msm> msms) {
        if (msms == null)
            return;

        for (Msm msm : msms) {
            MeterSignatures meterSignatures = this.analyze(msm);
            this.merge(meterSignatures);    // accumulate the results
        }
    }

    /**
     * Check whether the given MEI has a meter signature in its scoreDef, i.e., it has a defined time signature.
     * @param mei the MEI to check
     * @return the first meter signature in the first mdiv's first scoreDef/staffDeff/layerDef or a 0/0 meter signature if there is none
     */
    public static MeterSignature hasMeterSignature(Mei mei) {
        if  (mei == null)
            return null;

        ArrayList<Element> mdivs = mei.getAllMdivs();
        if (mdivs.isEmpty())
            return null;

        TreeSet<String> findThis = new TreeSet<>(List.of("scoreDef", "staffDef", "layerDef", "meterSig"));    // meter signature information can be found only in these elements
        TreeSet<String> stopHere = new TreeSet<>(List.of("section", "meterSig"));                             // we do not check for meter signatures in the musical text, only at the beginning in the initial scoreDef
        List<Element> candidates = Supplementary.depthFirstSearch(mdivs.getFirst(), findThis, stopHere);      // we check only the first mdiv, others are only verses with variants
        for (Element candidate : candidates) {
            MeterSignature meterSignature = MeterSignature.fromMei(candidate);
            if (meterSignature != null)
                return meterSignature;
        }

        return null;
    }

    /**
     * Perform an analysis of the given MEI. This method does not erase the previous results. So, performing it
     * iteratively on a series of MEIs will produce a cumulative result.
     * @param mei
     * @return the analysis result for this single MEI
     */
    @Deprecated
    public MeterSignatures analyze(Mei mei) {
        // safety checks
        if (mei == null)
            return null;
        Element music = mei.getMusic();
        if (music == null)
            return null;

        Nodes scoreStaffLayerDefsMeterSigs = new Nodes();           // collect all scoreDef, staffDef, layerDef and meterSig elements

        music.query("descendant::*[local-name()='scoreDef']").forEach(scoreDef -> {                     // for each scoreDef element
            scoreStaffLayerDefsMeterSigs.append(scoreDef);                                                       // append it to the list

            scoreDef.query("descendant::*[local-name()='staffDef']").forEach(staffDef -> {              // search for staffDefs in the scoreDef
                scoreStaffLayerDefsMeterSigs.append(staffDef);                                                   // append also the staffDef to the list
                staffDef.query("descendant::*[local-name()='layerDef']").forEach(scoreStaffLayerDefsMeterSigs::append);  // search for layerDef elements and append them to the list
            });

            scoreDef.query("descendant::*[local-name()='meterSig']").forEach(scoreStaffLayerDefsMeterSigs::append);     // search for meterSig elements and append them to the list
        });

        MeterSignatures meterSignatures = new MeterSignatures();        // this will hold the analysis result for this single MEI

        // iterate over the list of elements and try to find meter signatures
        for (Node defNode : scoreStaffLayerDefsMeterSigs) {
            MeterSignature meterSignature;
            Element element = (Element) defNode;

            // check for @meter.count, @meter.unit, @count and @unit
            Attribute count = element.getAttribute("count");
            if (count == null)
                count = element.getAttribute("meter.count");
            Attribute unit = element.getAttribute("unit");
            if (unit == null)
                unit = element.getAttribute("meter.unit");
            if ((count != null) && (unit != null)) {
                String str = count.getValue();
                double numerator = 0.0;
                String num = "";
                for (int i = 0; i < str.length(); ++i) {
                    if (((str.charAt(i) >= '0') && (str.charAt(i) <= '9')) || (str.charAt(i) == '.')) { // if character is a number/digit or a decimal dot
                        num += str.charAt(i);                                                           // add to num to parse it as double
                        continue;
                    }
                    // in any other case parse the string in num as a double and begin with a new
                    numerator += (num.isEmpty()) ? 0.0 : Double.parseDouble(num);
                    num = "";
                }
                numerator += (num.isEmpty()) ? 0.0 : Double.parseDouble(num);

                int denominator = Integer.parseInt(unit.getValue());

                meterSignature = new MeterSignature(numerator, denominator);
                meterSignatures.merge(meterSignature, 1, Integer::sum);

                continue;
            }

            // check for @meter.sym
            Attribute sym = element.getAttribute("sym");
            if (sym == null)
                sym = element.getAttribute("meter.sym");
            if (sym != null) {
                switch (sym.getValue()) {
                    case "common":
                        meterSignature = new MeterSignature(4.0, 4);
                        break;
                    case "cut":
                        meterSignature = new MeterSignature(2.0, 2);
                        break;
                    case "open":    // senza misura/no time signature
                    default:
                        meterSignature = new MeterSignature(0.0, 0);
                }
                meterSignatures.merge(meterSignature, 1, Integer::sum);
            }
        }

        // if we found no meter signature definition, add a default one
        if (meterSignatures.isEmpty())
            meterSignatures.put(new MeterSignature(0.0, 0), 1);

        // merge the results into this object
        this.merge(meterSignatures);

        return meterSignatures;
    }

    /**
     * merge another MeterSignatures object into this one
     * @param other the other MeterSignatures object
     */
    public void merge(MeterSignatures other) {
        for (MeterSignature key : other.keySet())
            this.merge(key, other.get(key), Integer::sum);
    }
}
