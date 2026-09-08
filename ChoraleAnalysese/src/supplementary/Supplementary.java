package supplementary;

import nu.xom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

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
}
