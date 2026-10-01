package harmonicAnalysis;

import meico.mei.Mei;
import meico.mpm.elements.Part;
import meico.mpm.elements.maps.GenericMap;
import meico.msm.Msm;
import meico.supplementary.KeyValue;
import msm.MsmX;
import msm.elements.MsmRoot;
import msm.elements.maps.ChordMap;
import msm.elements.maps.data.Chord;
import msm.elements.maps.data.Note;
import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Node;
import nu.xom.Nodes;
import supplementary.Supplementary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * This class represents a collection of chords and how often they occur.
 * The counter is a double type, so chord on unaccentuated times can be counted with a diminished weight.
 * @author Axel Berndt
 */
public class Chords extends HashMap<Chord, Double> {
    /**
     * constructor
     */
    public Chords() {
        super();
    }

    /**
     * Perform an analysis of the given MSM and aggregate it with analysis done so far.
     * This method expects a global timeSignatureMap. Otherwise, if there are local
     * timeSignatureMaps, it will take the first it finds.
     * Attention: this method alters the MEI and MSM!
     * @param mei the MEI needs to be provided, so we can add annotations to it
     * @param msm the MSM is the basis for the analysis
     * @param weightOfUnaccentuatedChords the weight of unaccentuated chords
     * @return
     */
    public static Chords analyze(Mei mei, Msm msm, double weightOfUnaccentuatedChords) {
        // safety checks
        if (msm == null)
            return null;

        // prepare MSM data
        MsmX msmx = new MsmX(msm.getDocument());    // create an extended Msm (MsmX) from the Msm object
        MsmRoot msmRoot = msmx.getMsmRoot();
        ChordMap chordMap = ChordMap.createChordMap(msmRoot.getGlobal().getDated().addMap(MsmX.CHORD_MAP).getXml());
        if (chordMap == null)
            return null;

        // collect all notes sorted by date
        Chords out = new Chords();
        ArrayList<GenericMap> scores = new ArrayList<>();
        TreeMap<Double, ArrayList<Note>> notes = new TreeMap<>();
        for (Part part : msmx.getMsmRoot().getAllParts()) {
            GenericMap score = part.getDated().getMap(MsmX.SCORE);
            scores.add(score);
            for (KeyValue<Double, Element> kv : score.getAllElementsOfType("note")) {
                Note note = Note.createNote(kv.getValue()); // make a Note instance of the Element
                if (notes.containsKey(kv.getKey())) {
                    notes.get(kv.getKey()).add(note);
                } else {
                    ArrayList<Note> ns = new ArrayList<>();
                    ns.add(note);
                    notes.put(kv.getKey(), ns);
                }
            }
        }

        // construct the chords and fill the chordMap
        ArrayList<Note> stillSounding = new ArrayList<>();  // all notes that are still sounding at the current date
        for (Double date : notes.keySet()) {
            // check whether still sounding notes have stopped at of before this date
            ArrayList<Note> toRemove = new ArrayList<>();
            for (Note note : stillSounding)
                if (note.getEndDate() <= date)
                    toRemove.add(note);
            stillSounding.removeAll(toRemove);

            // add new notes that start at this date
            stillSounding.addAll(notes.get(date));

            // create a chord from it and add it to the chordMap
            Chord chord = new Chord(stillSounding, true);
            Element chordElement = chord.getXml();
            chordElement.addAttribute(new Attribute("date", date.toString()));  // add the date to it
            chordMap.addElement(chordElement);  // chordMap.addChord(date, chord);
        }

        // find the highest staff number, i.e. the lowest staff, to place the harm elements
        Integer printInthAtStaffN = null;
        for (Node sd : mei.getMusic().query("descendant::*[local-name()='staffDef']")) {
            Element staffDef = (Element) sd;
            int sdn;
            try {
                sdn = Integer.parseInt(staffDef.getAttributeValue("n"));
            } catch (NumberFormatException e) {
                e.printStackTrace();
                continue;
            }
            if ((printInthAtStaffN == null) || (sdn > printInthAtStaffN))
                printInthAtStaffN = sdn;
        }

        // annotate the MEI with harm elements, so the analysis is also readable
        ArrayList<KeyValue<Double, Element>> harms = chordMap.toHarmList(String.valueOf(printInthAtStaffN));   // this produces us a list of MEI harm elements, already with the tstamp attribute; the key in the key-value pair is the MIDI tick date

        // now, we need to insert them in the MEI in the correct measure
        Nodes measures = mei.getMusic().query("descendant::*[local-name()='measure']");
        for (KeyValue<Double, Element> kv : harms) {                        // for each harm
            // To find the measure element in MEI, we need to identify the latest chord note, as this determines the position of the harm. Then we find that note in the MEI and from there the measure element.
            Element harm = kv.getValue();
            String[] participantIds = harm.getAttributeValue("plist").replace("#", "").split(" ");  // extract the IDs from the harm's plist attribute
            Element latestMsmNote = Supplementary.getLatest(scores, Arrays.asList(participantIds));    // find out which participant is the latest
            if (latestMsmNote == null)
                continue;

            // retrieve the measure element in MEI
            String id = latestMsmNote.getAttributeValue("id", "http://www.w3.org/XML/1998/namespace");
            Element measure = null;
            for (Node m : measures) {
                if (m.query("descendant::*[local-name()='note' and @xml:id='" + id + "']").size() > 0) {
                    measure = (Element) m;
                    break;
                }
            }
            if (measure == null)
                continue;

            measure.appendChild(harm);      // add harm to measure

            // based on attribute tstamp, find out whether the harm is on-beat (accentuated) or off-beat (unaccentuated) to determine its weighting in the statistics
            Double tstamp = Double.parseDouble(harm.getAttributeValue("tstamp"));
            double weight = ((tstamp % 1) == 0) ? 1.0 : weightOfUnaccentuatedChords;

            out.add(Chord.fromInth(harm.getAttributeValue("inth")), weight);                     // add the chord to the output statistics
        }

        return out;
    }

    /**
     * add a Chord to this
     * @param chord
     * @param weight
     */
    private void add(Chord chord, double weight) {
        if (this.containsKey(chord))
            this.put(chord, this.get(chord) + weight);
        else
            this.put(chord, weight);
    }

    /**
     * merge another Chords object into this one
     * @param other the other Chords object
     */
    public void merge(Chords other) {
        for (Chord key : other.keySet())
            this.merge(key, other.get(key), Double::sum);
    }

    /**
     * print the analysis results, formatted as a list of inth String and count
     * @return
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Chord key : this.keySet())
            sb.append(key.toInthString()).append("\t").append(this.get(key)).append("\n");
        return sb.toString();
    }
}
