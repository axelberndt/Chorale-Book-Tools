package supplementary;

import meico.mei.Helper;
import meico.mpm.elements.Part;
import meico.mpm.elements.maps.GenericMap;
import meico.supplementary.KeyValue;
import msm.MsmX;
import msm.elements.MsmRoot;
import msm.elements.maps.Score;
import nu.xom.Attribute;
import nu.xom.Element;

import java.util.*;

/**
 * A class for useful methods not associated to another class.
 * @author Axel Berndt
 */
public class Supplementary {
    /**
     * Breadth-first search for a given type of elements.
     *
     * @param root     the root element to start the search from
     * @param findThis a list of Strings/Local-Names of elements to find
     * @param stopHere a list of Strings/Local-Names of elements to stop the search at and not diving deeper into the XML subtree
     * @return a list of elements that match the criteria
     */
    public static List<Element> depthFirstSearch(Element root, SortedSet<String> findThis, SortedSet<String> stopHere) {
        ArrayList<Element> out = new ArrayList<>();
        if ((root == null) || (findThis == null))
            return out;

        if (findThis.contains(root.getLocalName()))
            out.add(root);

        if ((stopHere != null) && stopHere.contains(root.getLocalName()))
            return out;

        for (Element child : root.getChildElements())
            out.addAll(depthFirstSearch(child, findThis, stopHere));

        return out;
    }

    /**
     * all elements of the provided map
     *
     * @param toThis this map must be part of a full MSM document
     */
    public static void addMeiTstamps(GenericMap toThis) {
        MsmX msmx = new MsmX(toThis.getXml().getDocument());
        MsmRoot msmRoot = msmx.getMsmRoot();

        GenericMap timeSignatureMap = msmRoot.getGlobal().getDated().getMap(MsmX.TIME_SIGNATURE_MAP);
        if (timeSignatureMap == null) {                                             // if no global timeSignatureMap was found
            Element localtsmapElement = ((Element) toThis.getXml().getParent()).getFirstChildElement(MsmX.TIME_SIGNATURE_MAP);  // try to find a local timeSignatureMap
            if (localtsmapElement != null)
                timeSignatureMap = GenericMap.createGenericMap(localtsmapElement);
        }

        int ppq = msmx.getPPQ();
        double ppq4 = 4.0 * ppq;
        int timeSignIndex = -1;
        double tsDate = 0.0;
        double tsNumerator = 4.0;
        int tsDenominator = 4;
        double ticksPerBeat = ppq;
        double tickLengthOfOneMeasure = ticksPerBeat * tsNumerator;

        for (KeyValue<Double, Element> kv : toThis.getAllElements()) {     // for each map entry in toThis
            double date = kv.getKey();

            // we need to make sure that the time signature data is still up to date
            if (timeSignatureMap != null) {
                boolean update = false;
                for (int tsIndex = timeSignIndex + 1; tsIndex < timeSignatureMap.size(); ++tsIndex) {
                    if (timeSignatureMap.getAllElements().get(tsIndex).getKey() > date)
                        break;
                    timeSignIndex = tsIndex;
                    update = true;
                }
                if (update) {
                    KeyValue<Double, Element> timeSign = timeSignatureMap.getAllElements().get(timeSignIndex);
                    tsDate = timeSign.getKey();
                    tsNumerator = Double.parseDouble(Helper.getAttributeValue("numerator", timeSign.getValue()));
                    tsDenominator = Integer.parseInt(Helper.getAttributeValue("denominator", timeSign.getValue()));
                    ticksPerBeat = ppq4 / tsDenominator;
                    tickLengthOfOneMeasure = ticksPerBeat * tsNumerator;
                }
            }

            double beat = 1.0 + ((date - tsDate) % tickLengthOfOneMeasure) / ticksPerBeat;  // get the beat position of the event
            kv.getValue().addAttribute(new Attribute("tstamp", Double.toString(beat)));     // add the tstamp attribute to the element

        }
    }

    /**
     * find the latest element from the provided IDs in the MSM document
     * @param msmx
     * @param ids
     * @return
     */
    public Element getLatest(MsmX msmx, ArrayList<String> ids) {
        // TODO ...
        return null;
    }
}
