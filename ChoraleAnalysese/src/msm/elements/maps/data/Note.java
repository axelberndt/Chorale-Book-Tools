package msm.elements.maps.data;

import meico.mei.Helper;
import meico.xml.AbstractXmlSubtree;
import nu.xom.Attribute;
import nu.xom.Element;
import supplementary.Pitch;

/**
 * This class represents an MSM note element.
 * @author Axel Berndt
 */
public class Note extends AbstractXmlSubtree {
    /**
     * default constructor, generates a middle C
     * @throws Exception
     */
    private Note() throws Exception {
        Element note = new Element("note");
        note.addAttribute(new Attribute("date", "0.0"));
        note.addAttribute(new Attribute("midi.pitch", "60.0"));
        note.addAttribute(new Attribute("pitchname", "c"));
        note.addAttribute(new Attribute("accidentals", "0.0"));
        note.addAttribute(new Attribute("octave", "3.0"));
        note.addAttribute(new Attribute("duration", "720.0"));
        Helper.addUUID(note);
        this.parseData(note);
    }

    /**
     * constructor
     * @param xml
     * @throws Exception
     */
    private Note(Element xml) throws Exception {
        this.parseData(xml);
    }

    /**
     * Note factory
     * @return
     */
    public static Note createNote() {
        Note n;
        try {
            n = new Note();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return n;
    }

    /**
     * Note factory
     * @param xml
     * @return
     */
    public static Note createNote(Element xml) {
        Note n;
        try {
            n = new Note(xml);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return n;
    }

    /**
     * Note factory
     * @param date
     * @param midiPitch
     * @param pitchname
     * @param accidentals
     * @param octave
     * @param duration
     * @param id
     * @return
     */
    public static Note createNote(double date, double midiPitch, String pitchname, double accidentals, double octave, double duration, String id) {
        Element note = new Element("note");
        note.addAttribute(new Attribute("date", Double.toString(date)));
        note.addAttribute(new Attribute("midi.pitch", Double.toString(midiPitch)));
        note.addAttribute(new Attribute("pitchname", pitchname));
        note.addAttribute(new Attribute("accidentals", Double.toString(accidentals)));
        note.addAttribute(new Attribute("octave", Double.toString(octave)));
        note.addAttribute(new Attribute("duration", Double.toString(duration)));
        note.addAttribute(new Attribute("pitchname", pitchname));
        note.addAttribute(new Attribute("id", "http://www.w3.org/XML/1998/namespace", id));
        return Note.createNote(note);
    }

    /**
     * set the data of this object, this parses the xml element and generates the according data structure
     * @param xml
     * @throws Exception
     */
    @Override
    protected void parseData(Element xml) throws Exception {
        if (xml == null)
            return;

        if (!xml.getLocalName().equals("note"))
            throw new Exception("Invalid element type <" + xml.getLocalName() + ">. Expected <note>");



        this.setXml(xml);
    }

    /**
     * getter for the note's date
     * @return
     */
    public double getDate() {
        return Double.parseDouble(this.getXml().getAttributeValue("date"));
    }

    /**
     * getter for the note's duration
     * @return
     */
    public double getDuration() {
        return Double.parseDouble(this.getXml().getAttributeValue("duration"));
    }

    /**
     * compute the end date of the note
     * @return
     */
    public double getEndDate() {
        return this.getDate() + this.getDuration();
    }

    /**
     * getter for the note's midi.pitch
     * @return
     */
    public double getMidiPitch() {
        return Double.parseDouble(this.getXml().getAttributeValue("midi.pitch"));
    }

    /**
     * getter for the note's pitch name
     * @return
     */
    public String getPitchName() {
        return this.getXml().getAttributeValue("pitchname");
    }

    /**
     * getter for the note's accidentals
     * @return
     */
    public double getAccidentals() {
        return Double.parseDouble(this.getXml().getAttributeValue("accidentals"));
    }

    /**
     * convert the note's information to a Pitch object
     * @return
     */
    public Pitch getPitch() {
        return new Pitch(this.getXml());
    }

    /**
     * getter for the note's octave
     * @return
     */
    public double getOctave() {
        return Double.parseDouble(this.getXml().getAttributeValue("octave"));
    }

    /**
     * getter for the note's date
     * @return
     */
    public String getId() {
        return this.getXml().getAttributeValue("id", "http://www.w3.org/XML/1998/namespace");
    }
}
