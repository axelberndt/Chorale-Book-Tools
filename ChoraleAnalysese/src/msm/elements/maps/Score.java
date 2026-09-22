package msm.elements.maps;

import meico.mpm.elements.maps.GenericMap;
import meico.supplementary.KeyValue;
import msm.MsmX;
import msm.elements.maps.data.Note;
import nu.xom.Element;
import supplementary.Pitch;
import supplementary.PitchInterval;

import java.util.ArrayList;
import java.util.TreeSet;

/**
 * This class represents an MSM score, i.e. a list of note and rest elements.
 * @author Axel Berndt
 */
public class Score extends GenericMap {
    /**
     * default constructor
     * @throws Exception
     */
    protected Score() throws Exception {
        super(MsmX.SCORE);
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
        this.setType(MsmX.SCORE);            // make sure this is really a "score"
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
            if (kv.getKey() > date)     // we stop searching after the specified date
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

    /**
     * get all notes that play at the specified date
     * @param date
     * @return
     */
    public ArrayList<Note> getNotesAt(double date) {
        ArrayList<Note> results = new ArrayList<>();

        // for each element until (and including) the specified date
        for (int i=0; i < this.elements.size(); ++i) {
            KeyValue<Double, Element> kv = this.elements.get(i);
            if (kv.getKey() > date)     // we stop searching after the specified date
                break;

            if (!kv.getValue().getLocalName().equals("note"))   // it must be a note
                continue;

            double dateEnd = kv.getKey() + Double.parseDouble(kv.getValue().getAttributeValue("duration"));
            if (dateEnd <= date)         // if the note stops before or at the specified date
                continue;

            results.add(Note.createNote(kv.getValue()));
        }

        return results;
    }

    /**
     * Returns the sequence of melodic intervals. This expects a monophonic voice!
     * @param loopToFirstNote if true, the final note loops to the first note
     * @return the series of intervals between successive notes, entry format is (MIDI tick date of the 2nd note, PitchInterval between 1st and 2nd note)
     */
    public ArrayList<KeyValue<Double, PitchInterval>> getMelodicIntervalSequence(boolean loopToFirstNote) {
        ArrayList<KeyValue<Double, PitchInterval>> results = new ArrayList<>();

        ArrayList<KeyValue<Double, Element>> notes = this.getAllElementsOfType("note");

        if (loopToFirstNote)                            // the final note loops to the first note
            notes.add(notes.get(0));

        for (int i=0; i < notes.size() - 1; ++i) {      // for each note and its successor
            Pitch pitch1 = new Pitch(notes.get(i).getValue());
            Pitch pitch2 = new Pitch(notes.get(i+1).getValue());
            results.add(new KeyValue<>(notes.get(i + 1).getKey(), new PitchInterval(pitch1, pitch2)));
        }

        return results;
    }
}
