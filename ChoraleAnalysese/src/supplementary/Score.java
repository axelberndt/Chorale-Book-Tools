package supplementary;

import meico.mpm.elements.maps.GenericMap;
import meico.supplementary.KeyValue;
import nu.xom.Element;

import java.util.TreeSet;

/**
 * This class represents an MSM score, i.e. a list of note and rest elements.
 */
public class Score extends GenericMap {
    /**
     * default constructor
     * @throws Exception
     */
    protected Score() throws Exception {
        super("score");
    }

    /**
     * constructor
     * @param xml an MSM score Element
     * @throws Exception
     */
    protected Score(Element xml) throws Exception {
        super(xml);
    }

    /**
     * Score factory
     * @return a new Score object
     */
    public static Score createScore() {
        Score s;
        try {
            s = new Score();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return s;
    }

    /**
     * Score factory
     * @param xml an MSM score Element
     * @return a new Score object
     */
    public static Score createScore(Element xml) {
        Score s;
        try {
            s = new Score(xml);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return s;
    }

    /**
     * set the data of this object, this parses the xml element and generates the according data structure
     * @param xml
     */
    protected void parseData(Element xml) throws Exception {
        super.parseData(xml);
        this.setType("score");            // make sure this is really a "score"
    }

    /**
     * get all pitches that sound at the specified date, even if they started before
     * @param date
     * @return
     */
    public TreeSet<Pitch> getPitchesAt(double date) {
        TreeSet<Pitch> results = new TreeSet<>();

        // for each element until (and including) the specified date
        for (int i=0; i < this.elements.size(); ++i) {
            KeyValue<Double, Element> kv = this.elements.get(i);
            if (kv.getKey() > date)     // we stop searching at the specified date
                break;

            if (!kv.getValue().getLocalName().equals("note"))   // it must be a note
                continue;

            double dateEnd = kv.getKey() + Double.parseDouble(kv.getValue().getAttributeValue("duration"));
            if (dateEnd <= date)         // if the note stops before or at the specified date
                continue;

            results.add(new Pitch(kv.getValue()));
        }

        return results;
    }
}
