package keySignatures;

import meico.mei.Helper;
import meico.supplementary.KeyValue;
import nu.xom.Attribute;
import nu.xom.Element;
import nu.xom.Elements;
import supplementary.Accidental;
import supplementary.PitchName;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Objects;

/**
 * This class represents a key signature.
 * @author Axel Berndt
 */
public class KeySignature {
    public static final KeyValue<PitchName, Accidental>[] CIRCLE_OF_FIFTHS = new KeyValue[]{
            new KeyValue<>(PitchName.f, Accidental.f),
            new KeyValue<>(PitchName.c, Accidental.f),
            new KeyValue<>(PitchName.g, Accidental.f),
            new KeyValue<>(PitchName.d, Accidental.f),
            new KeyValue<>(PitchName.a, Accidental.f),
            new KeyValue<>(PitchName.e, Accidental.f),
            new KeyValue<>(PitchName.b, Accidental.f),
            new KeyValue<>(PitchName.f, null),
            new KeyValue<>(PitchName.c, null),  // index=8
            new KeyValue<>(PitchName.g, null),
            new KeyValue<>(PitchName.d, null),
            new KeyValue<>(PitchName.a, null),
            new KeyValue<>(PitchName.e, null),
            new KeyValue<>(PitchName.b, null),
            new KeyValue<>(PitchName.f, Accidental.s),
            new KeyValue<>(PitchName.c, Accidental.s),
            new KeyValue<>(PitchName.g, Accidental.s),
            new KeyValue<>(PitchName.d, Accidental.s),
            new KeyValue<>(PitchName.a, Accidental.s),
            new KeyValue<>(PitchName.e, Accidental.s),
            new KeyValue<>(PitchName.b, Accidental.s)
    };

    public static final HashMap<Integer, KeyMode> DIATONIC_INTERVAL_TO_MODE = new HashMap<>(){{
        put(0, KeyMode.major);
        put(1, KeyMode.dorian);
        put(2, KeyMode.phrygian);
        put(3, KeyMode.lydian);
        put(4, KeyMode.mixolydian);
        put(5, KeyMode.minor);
        put(6, KeyMode.locrian);
    }};

    private final PitchName pitchName;                          // root of the key signature
    private final Accidental keyAccidental;                     // accidental on the root (e.g. e flat major)
    private final KeyMode mode;                                 // the mode of the key signature
    private final EnumMap<PitchName, Accidental> accidentals;   // the accidentals of this key signature
    private final int hashCode;

    /**
     * default constructor, key signature is C major
     */
    public KeySignature() {
        this(PitchName.c, Accidental.n, KeyMode.major, new EnumMap<>(PitchName.class));
    }

    /**
     * constructor
     * @param pitchName
     * @param keyAccidental
     * @param mode
     * @param accidentals
     */
    public KeySignature(PitchName pitchName, Accidental keyAccidental, KeyMode mode, EnumMap<PitchName, Accidental> accidentals) {
        this.pitchName = pitchName;
        this.keyAccidental = keyAccidental;
        this.mode = mode;
        this.accidentals = accidentals;

        this.hashCode = Objects.hash(this.pitchName, this.keyAccidental, this.mode, this.accidentals); // content-based hash
//        this.hashCode = super.hashCode();
    }

    /**
     * Create a KeySignature object from an MEI scoreDef, staffDef, layerDef, or keySig element.
     * @param element the MEI element to parse
     * @return the KeySignature object or null if no key signature information was found (we cannot just assume C major)
     */
    public static KeySignature fromMei(Element element) {
        if (element == null)
            return null;

        Attribute pname, accid, mode, sig;
        Elements keyAccids = null;

        switch (element.getLocalName()) {
            case "scoreDef":
            case "staffDef":
            case "layerDef":
                pname = element.getAttribute("key.pname");
                accid = element.getAttribute("key.accid");
                mode = element.getAttribute("key.mode");
                sig = element.getAttribute("keysig");
                if (sig == null)                            // in older versions of MEI, e.g. 3.0, the attribute has a different name
                    sig = element.getAttribute("key.sig");  // so, we have to check this as well
                break;
            case "keySig":
                pname = element.getAttribute("pname");
                accid = element.getAttribute("accid");
                mode = element.getAttribute("mode");
                sig = element.getAttribute("sig");
                if (sig == null)    // if no @sig was found, collect possible <keyAccid> elements and parse them
                    keyAccids = element.getChildElements("keyAccid");
                break;
            default:
                return null;
        }

        PitchName pn = (pname == null) ? null : PitchName.valueOf(pname.getValue());
        Accidental ac = (accid == null) ? null : Accidental.valueOf(accid.getValue());
        KeyMode km = (mode == null) ? null : KeyMode.valueOf(mode.getValue());

        EnumMap<PitchName, Accidental> accids = new EnumMap<>(PitchName.class);     // so far an empty map of accidentals, to be filled subsequently
        if (sig != null) {                                                          // first, we try processing @sig
            int accidCount = Integer.parseInt(sig.getValue().substring(0, sig.getValue().length()-1));                // get the accidentals count
            if ((sig.getValue().charAt(sig.getValue().length()-1)) == 'f')
                accidCount = -accidCount;                                           // flats are negative direction (see the sharps array below, with flats we start at the end and go back)
            String[] acsn = (accidCount > 0) ? new String[]{"f", "c", "g", "d", "a", "e", "b"} : new String[]{"b", "e", "a", "d", "g", "c", "f"};   // the sequence of pitches to apply the accidentals
            for (int i=0; i < Math.abs(accidCount); ++i) {                                           // create the accidentals
                PitchName kpn = PitchName.valueOf(acsn[i]);
                Accidental kac = (accidCount > 0) ? Accidental.s : Accidental.f;    // >0 = sharp, <0 = flat
                accids.put(kpn, kac);
            }
        } else if (keyAccids != null) {                                             // next we try our luck with the <keyAccid> elements, if there are some
            for (Element keyAccid : keyAccids) {
                if ((keyAccid.getAttribute("pname") == null) || (keyAccid.getAttribute("accid") == null))
                    continue;
                PitchName kpn = PitchName.valueOf(keyAccid.getAttributeValue("pname"));
                Accidental kac = Accidental.valueOf(keyAccid.getAttributeValue("accid"));
                accids.put(kpn, kac);
            }
        }

        if ((pn == null) && (ac == null) && (km == null) && accids.isEmpty())
            return null;

        return new KeySignature(pn, ac, km, accids);
    }

    /**
     * Create a KeySignature object from an MSM keySignature element.
     * @param element
     * @return
     */
    public static KeySignature fromMsm(Element element) {
        if (element == null)
            return null;

        EnumMap<PitchName, Accidental> accids = new EnumMap<>(PitchName.class);     // so far an empty map of accidentals, to be filled subsequently
        Accidental lastAccid = null;                                                // we use this later to guesstimate the key signature

        for (Element accidental : element.getChildElements("accidental")) {         // collect the accidentals
            PitchName pn = PitchName.valueOf(accidental.getAttributeValue("pitchname").toLowerCase());
            Accidental ac = Accidental.valueOf(Helper.accidDecimal2String(Double.parseDouble(accidental.getAttributeValue("value"))));
            accids.put(pn, ac);
            lastAccid = ac;
        }

        // "guesstimate" the root pitch name etc. from the accidentals pattern and the final chord
        PitchName majorRoot;
        Accidental majorRootAccid = null;
        // first, guess a major scale from the accidentals pattern
        if (lastAccid == null) {
            majorRoot = PitchName.c;
        } else {
            switch (lastAccid) {
                case f:
                    majorRoot = CIRCLE_OF_FIFTHS[8 - (accids.size())].getKey();
                    majorRootAccid = CIRCLE_OF_FIFTHS[8].getValue();
                    break;
                case s:
                    majorRoot = CIRCLE_OF_FIFTHS[8 + (accids.size())].getKey();
                    majorRootAccid = CIRCLE_OF_FIFTHS[8].getValue();
                    break;
                default:                                                // if we have a non-standard sharp or flat accidental
                    return new KeySignature(null, null, null, accids);  // we have no idea about the key signature
            }
        }

        // now we have a guess for a major key signature, however, it could be a related one (minor, dorian etc.), so we need to check the final chord and find its root
        Element lowestNote = null;
        int lowestPitch = 127;
        for (Element part : element.getDocument().getRootElement().getChildElements("part")) {  // go through all parts
            Element score = part.getFirstChildElement("dated").getFirstChildElement("score");
            if (score == null)
                continue;

            Elements notes = score.getChildElements("note");
            if (notes.size() == 0)
                continue;

            Element lastNote =  notes.get(notes.size() - 1);
            int midiPitch = (int)Double.parseDouble(lastNote.getAttributeValue("midi.pitch"));
            if (midiPitch < lowestPitch) {
                lowestPitch = midiPitch;
                lowestNote = lastNote;
            }
        }

        if (lowestNote == null) {
            return  new KeySignature(majorRoot, majorRootAccid, KeyMode.major, accids);
        }

        String pitchname = lowestNote.getAttributeValue("pitchname").toLowerCase();
        PitchName rootPitchName = PitchName.valueOf(pitchname);
        Accidental rootAccid = accids.get(rootPitchName);
        int diatornicDistance = (rootPitchName.ordinal() - majorRoot.ordinal() + 7) % 7;
        KeyMode keyMode = DIATONIC_INTERVAL_TO_MODE.get(diatornicDistance);

        return new KeySignature(rootPitchName, rootAccid, keyMode, accids);
    }

    /**
     * Convert the KeySignature object to an MEI keySig element.
     * @return the MEI element
     */
    public Element toMei() {
        Element out = new Element("keySig");
        out.addAttribute(new Attribute("pname", this.pitchName.toString()));
        out.addAttribute(new Attribute("accid", this.keyAccidental.toString()));
        out.addAttribute(new Attribute("mode", this.mode.toString()));

        for (PitchName pn : this.accidentals.keySet()) {
            Element accid = new Element("keyAccid");
            accid.addAttribute(new Attribute("pname", pn.toString()));
            accid.addAttribute(new Attribute("accid", this.accidentals.get(pn).toString()));
            out.appendChild(accid);
        }

        return out;
    }

    /**
     * Convert the KeySignature object to an MSM keySignature element.
     * @return the MSM element
     */
    public Element toMsm() {
        Element out = new Element("keySignature");
        out.addAttribute(new Attribute("date", ""));

        for (PitchName pn : this.accidentals.keySet()) {
            Element accid = new Element("accidental");
            accid.addAttribute(new Attribute("pitchname", pn.toString()));
            accid.addAttribute(new Attribute("value", ""+Helper.accidString2decimal(this.accidentals.get(pn).toString())));
            out.appendChild(accid);
        }

        return out;
    }

    /**
     * a getter for the pitch name of the root of the key signature
     * @return
     */
    public PitchName getPitchName() {
        return this.pitchName;
    }

    /**
     * a getter for the accidental on the root of the key signature
     * @return
     */
    public Accidental getKeyAccidental() {
        return this.keyAccidental;
    }

    /**
     * a getter for the mode of the key signature
     * @return
     */
    public KeyMode getMode() {
        return this.mode;
    }

    /**
     * a getter for the key signature's accidentals
     * @return
     */
    public EnumMap<PitchName, Accidental> getAccidentals() {
        return this.accidentals;
    }

    /**
     * is there key signature data?
     * @return
     */
    public boolean isEmpty() {
        return this.pitchName == null
                && this.keyAccidental == null
                && this.mode == null
                && this.accidentals == null;    // can be empty, e.g. for C major
    }

    /**
     * Equality comparison
     * @param obj   the reference object with which to compare.
     * @return true if this object is the same as the obj argument; false otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if ((obj == null) || (obj.getClass() != this.getClass()))
            return false;

        KeySignature object = (KeySignature) obj;
        return Objects.equals(this.pitchName, object.pitchName)
                && Objects.equals(this.keyAccidental, object.keyAccidental)
                && Objects.equals(this.mode, object.mode)
                && Objects.equals(this.accidentals, object.accidentals);
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
     * String output
     * @return
     */
    @Override
    public String toString() {
        return ((this.pitchName != null) ?          this.pitchName.toString()       : "[no root pitch]")
                + ((this.keyAccidental != null) ?   this.keyAccidental.toString()   : "") + " "
                + ((this.mode != null) ?            this.mode.toString()            : "[no mode]") + " "
                + ((this.accidentals != null) ?     this.accidentals.toString()     : "{}");
    }
}