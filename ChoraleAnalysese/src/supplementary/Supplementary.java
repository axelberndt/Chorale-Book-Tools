package supplementary;

import meico.mpm.elements.Part;
import meico.mpm.elements.maps.GenericMap;
import msm.MsmX;
import msm.elements.MsmRoot;
import nu.xom.Element;

import java.util.*;

/**
 * A class for useful methods not associated to another class.
 * @author Axel Berndt
 */
public class Supplementary {
    /**
     * Breadth-first search for a given type of elements.
     * @param root the root element to start the search from
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
     * @param toThis this map must be part of a full MSM document
     */
    public static void addMeiTstamps(GenericMap toThis) {
        MsmX msmx = new MsmX(toThis.getXml().getDocument());
        MsmRoot msmRoot = msmx.getMsmRoot();

        GenericMap timeSignatureMap = msmRoot.getGlobal().getDated().getMap(MsmX.TIME_SIGNATURE_MAP);
        if (timeSignatureMap == null) {
            for (Part part : msmRoot.getAllParts()) {
                timeSignatureMap = part.getDated().getMap(MsmX.TIME_SIGNATURE_MAP);
                if (timeSignatureMap != null) {
                    break;
                }
            }
        }

        int ppq = msmx.getPPQ();
        double ppq4 = 4.0 * ppq;
        double tsDate = 0.0;
        double tsNumerator = 4.0;
        int tsDenominator = 4;
        double ticksPerBeat = ppq;
        double tickLengthOfOneMeasure = ticksPerBeat * tsNumerator;

        // TODO ...
    }
}
