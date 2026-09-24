package msm.elements.maps.data;

import meico.mei.Helper;
import nu.xom.Attribute;
import nu.xom.Element;
import supplementary.PitchInterval;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

/**
 * This class represents a musical chord. Here, chords are defined in their most generic sense
 * as a list of intervals over a fundamental pitch.
 * @author Axel Berndt
 */
public class Chord extends TreeSet<PitchInterval> {
    private final ArrayList<String> participantsList = new ArrayList<>();   // a list of all notes (by ID) that contribute to this chord
    private final int hashCode;

    /**
     * Constructs a chord from a list of pitch intervals.
     * @param pitchIntervals the pitch intervals that make up the chord
     * @param ignoreOctave whether to ignore the octave of the pitch intervals when constructing the chord
     */
    public Chord(TreeSet<PitchInterval> pitchIntervals, boolean ignoreOctave) {
        TreeSet<PitchInterval> pis = new TreeSet<>();
        if (ignoreOctave) {
            for (PitchInterval pi : pitchIntervals) {
                pi = new PitchInterval(pi.semitones % 12, pi.diatonic % 7);
                pis.add(pi);
            }
        } else {
            pis.addAll(pitchIntervals);
        }

        super(pis);
        this.hashCode = this.computeHashCode();
    }

    /**
     * construct a chord from a list of notes
     * @param notes
     * @param ignoreOctave whether to ignore the octave of the notes when constructing the chord
     * @return
     */
    public Chord(List<Note> notes, boolean ignoreOctave) {
        if ((notes == null) || notes.isEmpty())
            throw new IllegalArgumentException("List notes must not be null or empty!");

        super();

        // find lowest note, because all other pitch intervals are relative to the lowest note
        Note lowest = notes.get(0);
        for (int i=0; i < notes.size(); ++i) {
            Note n = notes.get(i);
            if (n.getPitch().compareTo(lowest.getPitch()) < 0)
                lowest = n;
        }

        // collect the pitch intervals including P1 for the lowest note
        for (Note n : notes) {
            PitchInterval pi = new PitchInterval(n, lowest);
            if (ignoreOctave)
                pi = new PitchInterval(pi.semitones % 12, pi.diatonic % 7);
            this.add(pi);
            this.participantsList.add(n.getId());
        }

        this.hashCode = this.computeHashCode();
    }

    /**
     * constructor converts an MSM chord element to a Chord
     * @param xml
     * @return
     */
    public Chord(Element xml) {
        if ((xml == null) || !xml.getLocalName().equals("chord"))
            throw new IllegalArgumentException("Provided XML element must not be null and be of type <chord>!");

        for (Element pi : xml.getChildElements("pitchInterval")) {
            int semitones = Integer.parseInt(pi.getAttributeValue("semitones"));
            int diatonic = Integer.parseInt(pi.getAttributeValue("diatonic"));
            this.add(new PitchInterval(semitones, diatonic));
        }

        Attribute plist = xml.getAttribute("plist");
        if (plist != null) {
            for (String participant : plist.getValue().split(" "))
                this.participantsList.add(participant.replace("#", ""));
        }

        this.hashCode = this.computeHashCode();
    }

    /**
     * factory that converts an MEI compliant harmonic interval String
     * (<a href="https://music-encoding.org/guidelines/v5/data-types/data.INTERVAL.HARMONIC.html">...</a>)
     * to a Chord
     * @param inth an MEI compliant @inth String
     * @return
     */
    public static Chord fromInth(String inth) {
        TreeSet<PitchInterval> list = new TreeSet<>();
        String[] parts = inth.trim().split(" ");    // there might be more intervals in the string, they are space separated

        for (String part : parts) {
            PitchInterval pi = PitchInterval.fromInth(part);
            if (pi != null)
                list.add(pi);
        }

        return new Chord(list, false);
    }

    /**
     * equality check
     * @param obj object to be compared for equality with this set
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if ((obj == null) || !this.getClass().equals(obj.getClass()))
            return false;

        Chord object = (Chord) obj;
        if ((this.size() != object.size()) || !object.containsAll(this))
            return false;

        return true;
    }

    /**
     * String output
     * @return
     */
    @Override
    public String toString() {
        return super.toString();
    }

    /**
     * String output that is compatible with the MEI data.INTERVAL.HARMONIC schema.
     * @return
     */
    public String toInthString() {
        String out = "";
        for (PitchInterval pitchInterval : this) {
            String inth = pitchInterval.getHarmonicIntervalWithoutOctave();
            out += " " + inth;
        }
        return out.substring(1);
    }

    /**
     * generate an MEI chordDef representation of this chord
     * @return
     */
    public Element toChordDef() {
        Element chordDef = new Element("chordDef");

        // generate ID attribute from the inth String
        Attribute id = new Attribute("id", this.toInthString().replace(" ", ""));   // create ID attribute
        id.setNamespace("xml", "http://www.w3.org/XML/1998/namespace");     // set its namespace to xml
        chordDef.addAttribute(id);                                                  // add ID attribute to chordDef

        // add fundamental via P1 interval
//        Element p1 = new Element("chordMember");
//        p1.addAttribute(new Attribute("inth", "P1"));
//        chordDef.appendChild(p1);

        // add the other intervals
        for (PitchInterval pitchInterval : this) {
            Element p = new Element("chordMember");
            p.addAttribute(new Attribute("inth", pitchInterval.getHarmonicIntervalWithoutOctave()));
            chordDef.appendChild(p);
        }

        return chordDef;
    }

    /**
     * Hash code output
     * @return
     */
    @Override
    public int hashCode() {
        return this.hashCode;
    }

    /**
     * compute hash code of this object
     *
     * @return
     */
    private int computeHashCode() {
        String hc = "";
        for (PitchInterval pitchInterval : this)
            hc += " " + ((pitchInterval.semitones * 1000) + pitchInterval.diatonic);

        return Objects.hash(hc.substring(1));
    }

    /**
     * generate an MSM compliant XML representation of this chord
     * @return
     */
    public Element getXml() {
        Element out = new Element("chord");                  // create the Element
        out.addAttribute(new Attribute("date", "0.0"));  // needs being set by the user
        Helper.addUUID(out);                                    // generate ID attribute from the inth String

        String plist = "";
        for (String participant : this.participantsList)
            plist += " #" + participant;
        out.addAttribute(new Attribute("plist", plist.substring(1)));   // substring(1) removes the leading space

        // add the pitch intervals that define the chord
        for (PitchInterval pitchInterval : this)
            out.appendChild(pitchInterval.getXml());

        return out;
    }
}
