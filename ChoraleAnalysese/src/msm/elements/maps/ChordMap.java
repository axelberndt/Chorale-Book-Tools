package msm.elements.maps;

import meico.mpm.elements.Part;
import msm.elements.MsmRoot;
import msm.elements.maps.data.Chord;
import meico.mpm.elements.maps.GenericMap;
import meico.supplementary.KeyValue;
import msm.MsmX;
import nu.xom.Attribute;
import nu.xom.Element;
import supplementary.Supplementary;

import java.util.ArrayList;

public class ChordMap extends GenericMap {
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
     * converts this map to a list of MEI harm elements
     * @param printInthAtStaffN the MEI staff number to print the harm elements at or null, if not desired
     * @return the list of MEI harm elements, the format of the entries is (MIDI tick date, MEI harm element)
     */
    public ArrayList<KeyValue<Double, Element>> toHarmList(String printInthAtStaffN) {
        Supplementary.addMeiTstamps(this);              // all elements need tstamps

        ArrayList<KeyValue<Double, Element>> harmList = new ArrayList<>();
        for (KeyValue<Double, Element> kv : this.getAllElements()) {
            Chord chord = new Chord(kv.getValue());                         // from a Chord instance ...
            Element harm = chord.toHarm(printInthAtStaffN != null); // ... we more easily get the MEI harm element
            harm.addAttribute(new Attribute("tstamp", kv.getValue().getAttributeValue("tstamp")));  // we add the tstamp
            harmList.add(new KeyValue<>(kv.getKey(), harm));                // finally, add the harm to the list
        }

        // if we printInth is true and we found the highest staff number, i.w. the lowest staff, we now add a staff attribute to harm, so it gets printed by Verovio
        if (printInthAtStaffN != null) {
            for (KeyValue<Double, Element> kv : harmList) {
                kv.getValue().addAttribute(new Attribute("staff", printInthAtStaffN));
            }
        }


        return harmList;
    }

    /**
     * generate String output
     * @return
     */
    @Override
    public String toString() {
        String out = "<chordMap>\n";

        for (KeyValue<Double, Element> kv : this.getAllElements()) {
            Chord chord = new Chord(kv.getValue());
            String inth = chord.toInthString();
            out += "  " + kv.getKey() + ": \"" + inth + "\"\n";
        }

        return out;
    }
}
