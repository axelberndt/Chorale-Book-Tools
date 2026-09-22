package harmonicAnalysis;

import meico.mei.Mei;
import meico.mpm.elements.Part;
import meico.msm.Msm;
import meico.supplementary.KeyValue;
import msm.MsmX;
import msm.elements.MsmRoot;
import msm.elements.maps.ChordMap;
import msm.elements.maps.data.Chord;
import msm.elements.maps.data.Note;
import nu.xom.Attribute;
import nu.xom.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * This class represents a collection of chords and how often they occur.
 * @author Axel Berndt
 */
public class Chords extends HashMap<Chord, Integer> {
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
     * @param mei the MEI needs to be provided, so we can add annotations to it
     * @param msm the MSM is the basis for the analysis
     * @return
     */
    public Chords analyze(Mei mei, Msm msm) {
        // safety checks
        if (msm == null)
            return null;

        MsmX msmx = new MsmX(msm.getDocument());    // create an extended Msm (MsmX) from the Msm object
        MsmRoot msmRoot = msmx.getMsmRoot();
        ChordMap chordMap = ChordMap.createChordMap(msmRoot.getGlobal().getDated().addMap(MsmX.CHORD_MAP).getXml());
        if (chordMap == null)
            return null;

        // collect all notes sorted by date
        TreeMap<Double, ArrayList<Note>> notes = new TreeMap<>();
        for (Part part : msmx.getMsmRoot().getAllParts()) {
            for (KeyValue<Double, Element> kv : part.getDated().getMap(MsmX.SCORE).getAllElementsOfType("note")) {
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
            for (Note note : toRemove)
                stillSounding.remove(note);

            // add new notes that start at this date
            stillSounding.addAll(notes.get(date));

            // create a chord from it and add it to the chordMap
            Chord chord = new Chord(stillSounding);
            Element chordElement = chord.getXml();
            chordElement.addAttribute(new Attribute("date", date.toString()));  // add the date to it
            chordMap.addChord(date, chord);
        }

        //TODO ...
        // for each entry in the notes map
        //   compute the chord of all notes that sound at the respective date
        //   if the chord is already in the HashMap? increase its counter
        //   else if the chord is already in the exclusion map, ignore it
        //   else ask where it should be added
        //     if it is added tot the HashMap, add Chord.toChordDef() to the MEI chordTable
        //       can I add a corresponding <harm> to the MEI as a (proof-)readable visual annotation (with plist, tstamp etc.)?

        return null;
    }

    /**
     * merge another Chords object into this one
     * @param other the other Chords object
     */
    public void merge(Chords other) {
        for (Chord key : other.keySet())
            this.merge(key, other.get(key), Integer::sum);
    }

}
