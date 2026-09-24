package msm.elements.maps;

import msm.elements.maps.data.Chord;
import meico.mpm.elements.maps.GenericMap;
import meico.supplementary.KeyValue;
import msm.MsmX;
import nu.xom.Attribute;
import nu.xom.Element;

import java.util.TreeMap;

public class ChordMap extends GenericMap {
    private final TreeMap<Double, Chord> chordSequence = new TreeMap<>();

    /**
     * constructor, generates an empty chordMap
     * @throws Exception
     */
    private ChordMap() throws Exception {
        super(MsmX.CHORD_MAP);
    }

    /**
     * constructor, creates an instance from an XML element
     * @param xml
     * @throws Exception
     */
    private ChordMap(Element xml) throws Exception {
        super(xml);
    }

    /**
     * ChordMap factory
     * @return
     */
    public static ChordMap createChordMap() {
        ChordMap d;
        try {
            d = new ChordMap();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return d;
    }

    /**
     * ChordMap factory
     * @param xml
     * @return
     */
    public static ChordMap createChordMap(Element xml) {
        ChordMap d;
        try {
            d = new ChordMap(xml);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return d;
    }

    /**
     * set the data of this object, this parses the xml element and generates the according data structure
     * @param xml
     */
    protected void parseData(Element xml) throws Exception {
        super.parseData(xml);
        this.setType("chordMap");            // make sure this is really a "chordMap"
    }

    /**
     * add a chord to the chordMap at the given date
     * @param date
     * @param chord
     * @return
     */
    public int addChord(double date, Chord chord) {
        Element e = chord.getXml();
        e.addAttribute(new Attribute("date", Double.toString(date)));
        KeyValue<Double, Element> kv = new KeyValue<>(date, e);
        return this.insertElement(kv, false);
    }

    /**
     * get the chord at the given date
     * @param date
     * @return
     */
    public Chord getChordAt(double date) {
        Element e = this.getElementBeforeAt(date);
        if (e == null)
            return null;
        return new Chord(e);
    }

    /**
     * generate String output
     * @return
     */
    @Override
    public String toString() {
        String out = "<chordMap>: ";

        for (KeyValue<Double, Element> kv : this.getAllElements()) {
            Chord chord = new Chord(kv.getValue());
            String inth = chord.toInthString();
            out += "  " + kv.getKey() + ": \"" + inth + "\"\n";
        }

        return out;
    }
}
