package choralePerformer;

import choralePerformer.Main;
import meico.mei.Helper;
import meico.mei.Mei;
import meico.mpm.Mpm;
import meico.mpm.elements.Part;
import meico.mpm.elements.Performance;
import meico.mpm.elements.maps.*;
import meico.mpm.elements.maps.data.ArticulationData;
import meico.mpm.elements.styles.ArticulationStyle;
import meico.mpm.elements.styles.MetricalAccentuationStyle;
import meico.mpm.elements.styles.defs.AccentuationPatternDef;
import meico.mpm.elements.styles.defs.ArticulationDef;
import meico.msm.Msm;
import meico.supplementary.KeyValue;
import meico.supplementary.RandomNumberProvider;
import nu.xom.*;

import java.util.*;

/**
 * This class implements the performance generation routine.
 * @author Axel Berndt
 */
public class Performer {
    public static final int PPQ = 360;
    public static boolean SWING = false;
    public static double TEMPO = 100.0;
    public static final double PHRASE_TIME = 4.3;                                   // the time in seconds to sing one phrase without breathing
    private static final double TEMPO_REDUCTION_AT_VERSE_END = 0.5;
    private static final double TEMPO_REDUCTION_AT_PHRASE_END = 0.8;
    private static double TEMPO_AT_PHRASE_END = Performer.TEMPO * Performer.TEMPO_REDUCTION_AT_PHRASE_END;
    private static double TEMPO_AT_VERSE_END = Performer.TEMPO * Performer.TEMPO_REDUCTION_AT_VERSE_END;
    private static final double MEAN_TEMPO_AT_PHRASE_END = 0.95;
    private static final String MEAN_TEMPO_AT_VERSE_END = "0.9";
    private static final double TEMPO_AT_GENERAL_REST = Performer.TEMPO * 1.5;      // this is used to shorten general rests
    public static double TEMPO_MODULATION_INTENSITY = 1.0;                          // 0.0 = off, default = 1.0
    private static final double FINAL_RITARDANDO_INTENSITY = Performer.TEMPO_MODULATION_INTENSITY * 1.5;  // set the final ritardando intensity (typically greater than the usual tempo modulation intensity, default is 2.0)
    public static double METRICAL_ACCENTUATION_SCALE = 16.0;                        // 0.0 = no metrical accentuation, default = 20.0
    private static final double ACCENT_ON_NOUN = 7.0;                               // this is the amount of accent that is put on the first syllable of each noun, i.e. on syllables that start with an upper case character
    private static double DYNAMICS = 50.0;                                         // the basic dynamics for the current verse, it is changed a bit with every new verse using random (see below)
    private static final double DYNAMICS_REDUCTION_AT_PHRASE_END = 0.5;
    private static final double DYNAMICS_PROTRACTION_AT_PHRASE_END = 1.0;
    private static final double DYNAMICS_AT_PHRASE_END = Performer.DYNAMICS * Performer.DYNAMICS_REDUCTION_AT_PHRASE_END;
    private static final boolean SUBNOTE_DYNAMICS = true;

    private final Mei mei;
    private final String MEI_NAMESPACE;
    private final Msm msm;
    private final Mpm mpm;
    private final Performance performance;

    private static final RandomNumberProvider tempoRandomizer = RandomNumberProvider.createRandomNumberProvider_brownianNoiseDistribution(2, -3, 3);
    private static final RandomNumberProvider dynamicsRandomizer = RandomNumberProvider.createRandomNumberProvider_uniformDistribution(-5.0, 5.0);    // this is used to randomize the basic dynamics of phrase

    private final double beatLength;
    private final HashMap<Element, Element> breathNoteMap = new HashMap<>();    // a set of tuples (breath articulation, note)
    private final TreeSet<Double> generalRests = new TreeSet<>();               // a set of the dates where there are general rests, so we can shorten them in the phrasing

    /**
     * contructor
     * @param msm
     * @param mpm
     */
    public Performer(Msm msm, Mpm mpm, Mei mei) {
        this.mei = mei;
        this.MEI_NAMESPACE = this.mei.getRootElement().getNamespaceURI();
        this.msm = msm;
        this.mpm = mpm;
        this.beatLength = this.getBeatLength();
        this.performance = mpm.addPerformance("CaP Performance");
        this.performance.setPPQ(Performer.PPQ);
        this.addPartsToPerformance();
    }

    /**
     * This method makes sure that the performance has the same parts as the MSM.
     */
    private void addPartsToPerformance() {
        for (Element part : this.msm.getParts()) {
            Element clone = Helper.cloneElement(part);
            clone.setNamespaceURI(Mpm.MPM_NAMESPACE);
            this.performance.addPart(Part.createPart(clone));
        }
    }

    /**
     * access the MSM
     * @return
     */
    public Msm getMsm() {
        return this.msm;
    }

    /**
     * access the MPM
     * @return
     */
    public Mpm getMpm() {
        return this.mpm;
    }

    /**
     * Generate the performance, apply it to the MSM and return the augmented new MSM.
     * @return
     */
    public Msm generateExpressiveMsm() {
        this.globalizeArticulations();
        this.uniteFragmentedTimeSignatures();
        this.addMetricalAccentuation();
        this.redoSectionAndPhraseStructure();
        if (!Main.IGNORE_MEI_TEMPO)
            this.computeBasicTempo();
        this.addPhrasing();
        this.swing();
//        this.sylArticulation();
        this.addHumanizing();

        return this.performance.perform(this.msm);
    }

    /**
     * Get the beat length of the current piece.
     * @return
     */
    private double getBeatLength() {
        // read the beatLength from the first timeSignature you find in the MSM
        double beatLength = 0.25;
        Element timeSignatureMap = this.msm.getGlobal().getFirstChildElement("dated").getFirstChildElement("timeSignatureMap"); // we seek only the global time signature
        if (timeSignatureMap != null) {
            Element timeSignature = timeSignatureMap.getFirstChildElement("timeSignature");
            if (timeSignature != null) {
                int denominator = Integer.parseInt(timeSignature.getAttributeValue("denominator"));
                beatLength = 1.0 / denominator;
            }
        }
        return beatLength;
    }

    /**
     * In the MPM performance merge all articulations into a global articulationMap.
     *
     */
    private void globalizeArticulations() {
        // globalize in "MEI export performance"
        Performance performanceMeiExport = this.mpm.getPerformance("MEI export performance");
        if (performanceMeiExport == null)
            return;

        Performance performanceCaP = this.mpm.getPerformance("CaP Performance");
        ArticulationStyle articStyle = (ArticulationStyle) performanceCaP.getGlobal().getHeader().addStyleDef(Mpm.ARTICULATION_STYLE, "Default Articulations");
        articStyle.addDef(ArticulationDef.createDefaultArticulationDef("accent"));
        articStyle.addDef(ArticulationDef.createDefaultArticulationDef("legatissimo"));
        articStyle.addDef(ArticulationDef.createDefaultArticulationDef("marcato"));
        articStyle.addDef(ArticulationDef.createDefaultArticulationDef("nonlegato"));
        articStyle.addDef(ArticulationDef.createDefaultArticulationDef("pizzicato"));
        articStyle.addDef(ArticulationDef.createDefaultArticulationDef("portato"));
        articStyle.addDef(ArticulationDef.createDefaultArticulationDef("sforzato"));
        articStyle.addDef(ArticulationDef.createDefaultArticulationDef("snap pizzicato"));
        articStyle.addDef(ArticulationDef.createDefaultArticulationDef("spiccato"));
        articStyle.addDef(ArticulationDef.createDefaultArticulationDef("staccato"));
        articStyle.addDef(ArticulationDef.createDefaultArticulationDef("staccatissimo"));
        articStyle.addDef(ArticulationDef.createDefaultArticulationDef("tenuto"));

        ArticulationDef legato = ArticulationDef.createDefaultArticulationDef("legato");
        legato.setRelativeDuration(1.0);
        legato.setAbsoluteVelocityChange(5.0);
        articStyle.addDef(legato);

        ArticulationDef legatoStop = ArticulationDef.createDefaultArticulationDef("legatoStop");
        legatoStop.setRelativeDuration(0.9);
        articStyle.addDef(legatoStop);

        ArticulationDef breath = ArticulationDef.createDefaultArticulationDef("breath");
        breath.setAbsoluteDurationChangeMs(-200.0);
        articStyle.addDef(breath);

        ArticulationMap globalArticMapCaP = (ArticulationMap) performanceCaP.getGlobal().getDated().addMap(Mpm.ARTICULATION_MAP);
        globalArticMapCaP.addStyleSwitch(0.0, "Default Articulations", "nonlegato");

        GenericMap score = GenericMap.createGenericMap(this.msm.getParts().get(0).getFirstChildElement("dated").getFirstChildElement("score"));
        for (Part part : performanceMeiExport.getAllParts()) {
            GenericMap localArticulationMap = part.getDated().getMap(Mpm.ARTICULATION_MAP);
            if (localArticulationMap == null)
                continue;

            for (KeyValue<Double, Element> articulation : localArticulationMap.getAllElementsOfType("articulation")) {
                // create a copy, add it to the global map, make it applicable to all notes by removing association to a specific noteid
                Element articElt = articulation.getValue().copy();
                globalArticMapCaP.addElement(articElt);

                if (!articElt.getAttributeValue("name.ref").equals("breath"))       // if it is not a breath
                    continue;                                                       // we are done, the note association is kept

                Attribute noteid = articElt.getAttribute("noteid");
                Element note = null;
                if (noteid != null) {
                    note = score.getElementByID(noteid.getValue().substring(1));    // find the note that this breath is associated to
                    articElt.removeAttribute(noteid);                               // remove that association, so all notes at the same date are affected by it
                }
                this.breathNoteMap.put(articElt, note);                             // store the breath-to-note association for later use in phrasing
            }
        }
    }

    /**
     * This method parses the lyrics element of each MSM note and computes an according articulation.
     */
    private void sylArticulation() {
        // initialize the articulation style with all required articulation definitions
        ArticulationStyle style = (ArticulationStyle) this.performance.getGlobal().getHeader().addStyleDef(Mpm.ARTICULATION_STYLE, "Cap");

        ArticulationDef defaultDef = ArticulationDef.createArticulationDef("default");
        defaultDef.setAbsoluteDurationChangeMs(-10.0);
        style.addDef(defaultDef);

        ArticulationDef dashDef = ArticulationDef.createArticulationDef("-");
//        dashDef.setAbsoluteDurationChangeMs(-5.0);
        dashDef.setRelativeDuration(1.0);
        style.addDef(dashDef);

        ArticulationDef underscoreDef = ArticulationDef.createArticulationDef("_");
        underscoreDef.setRelativeDuration(1.0);
        style.addDef(underscoreDef);

        ArticulationDef endSylDef = ArticulationDef.createArticulationDef("endSyl");
//        endSylDef.setAbsoluteDurationChangeMs(-70.0);
        endSylDef.setAbsoluteDurationChangeMs(-50.0);
        style.addDef(endSylDef);

        ArticulationDef commaDef = ArticulationDef.createArticulationDef(",");
        commaDef.setAbsoluteDurationChangeMs(-100.0);
        style.addDef(commaDef);

        ArticulationDef semicolonDef = ArticulationDef.createArticulationDef(";");
        semicolonDef.setAbsoluteDurationChangeMs(-110.0);
        style.addDef(semicolonDef);

        ArticulationDef colonDef = ArticulationDef.createArticulationDef(":");
        colonDef.setAbsoluteDurationChangeMs(-110.0);
        style.addDef(colonDef);

        ArticulationDef periodDef = ArticulationDef.createArticulationDef(".");
        periodDef.setAbsoluteDurationChangeMs(-120.0);
        periodDef.setRelativeVelocity(0.8);
        style.addDef(periodDef);

        // articulate all notes in all parts
        ArticulationMap prevMap = null;
        for (Element part : this.msm.getParts()) {                                                                      // for each part
            // create an articulationMap in the corresponding MPM part
            ArticulationMap map;
            if (part.getLocalName().equals("part"))
                map = (ArticulationMap) this.performance.getPart(Integer.parseInt(part.getAttributeValue("number"))).getDated().addMap(Mpm.ARTICULATION_MAP);
            else
                map = (ArticulationMap) this.performance.getGlobal().getDated().addMap(Mpm.ARTICULATION_MAP);

            map.addStyleSwitch(0.0, style.getName(), defaultDef.getName());                                             // add the style switch with default articulation at the beginning of the map

            // with prev we keep the articulation of the previous note
            ArticulationData prev = null;

            // create the articulations for each note
            Element score = part.getFirstChildElement("dated").getFirstChildElement("score");
            for (int n = 0; n < score.getChildElements().size(); ++n) {                                                           // for each part's note and rest
                Element note = score.getChildElements().get(n);

                // a rest switches prev back to default articulation
                if (note.getLocalName().equals("rest")) {
                    prev = null;
                    continue;
                }

                // make sure the note has an xml:is
                String id;
                Attribute idAtt = note.getAttribute("id", "http://www.w3.org/XML/1998/namespace");
                if (idAtt == null)
                    id = Helper.addUUID(note);
                else
                    id = idAtt.getValue();

                // articulate according to the lyrics string
                Element lyrics = note.getFirstChildElement("lyrics");                                                   // get the lyrics string
                if (lyrics == null) {                                                                                   // if no lyrics
                    if (prev != null) {                                                                                 // if we have a previous articulation, otherwise the note gets no special articulation, just the default
                        ArticulationData artic = prev.clone();                                                          // clone it
                        artic.absoluteVelocityChange = 0.0;                                                             // an accentuation from a noun should not be inherited by the successor
                        artic.date = Double.parseDouble(note.getAttributeValue("date"));                                // set it's date to the date of the note
                        artic.noteid = "#" + id;                                                                        // set the noteid attribute to the current note
                        map.addArticulation(artic);                                                                     // add the articulation to the map
                    }
                    continue;
                }

                // initialize the articulation and add it to the map
                prev = new ArticulationData();
                prev.date = Double.parseDouble(note.getAttributeValue("date"));
                prev.noteid = "#" + id;

                // if the syllable starts with an upper case character, it is a noun or beginning of sentence and gets a little emphasis
                char first = lyrics.getValue().charAt(0);
                if (Character.isUpperCase(first))
                    prev.absoluteVelocityChange = Performer.ACCENT_ON_NOUN;

                // if this was the last note or a rest follows, we do not need to shorten it (for breathing), but will decrease its loudness a little bit
                if ((n == score.getChildElements().size() - 1) || score.getChildElements().get(n + 1).getLocalName().equals("rest")) {
                    prev.relativeVelocity = 0.8;
                    map.addArticulation(prev);
                    continue;
                }

                // we have a lyrics string, create the according articulation
                char last = lyrics.getValue().charAt(lyrics.getValue().length() - 1);
                switch (last) {
                    case '-':
                    case '_':
                    case ',':
                    case ';':
                    case ':':
                    case '.':
                        prev.articulationDefName = "" + last;
                        break;
                    default:
                        prev.articulationDefName = "endSyl";
                        break;
                }

                map.addArticulation(prev);
            }

            // if the map is empty, copy the articulations of the previous map, but without noteids
            if ((map.size() == 1) && (prevMap != null)) {
                double prevDate = Double.MIN_VALUE;
                for (KeyValue<Double, Element> artic : prevMap.getAllElementsOfType("articulation")) {  // for each articulation in the previous articulationMap
                    if (artic.getKey() == prevDate)                                                     // do not copy an articulation for a date where we already have one, the interplay of two articulations that were for two different notes can cause strange results
                        continue;

                    Element clone = artic.getValue().copy();                                            // copy the articulation
                    Attribute noteid = Helper.getAttribute("noteid", clone);
                    if (noteid != null)                                                                 // if there is a noteid attribute
                        clone.removeAttribute(noteid);                                                  // remove it, it is only valid for the other part, not this one
                    map.addElement(clone);                                                              // add the copy to the current articulationMap

                    prevDate = artic.getKey();
                }
            }

            prevMap = map;                                                                              // set this map to be the previous map in the next iteration
        }
    }

    /**
     * This method adds a swing timing.
     */
    private void swing() {
        if (!Performer.SWING)
            return;

        for (Element part : this.msm.getRootElement().getChildElements()) {                                             // for the global and all part elements in MSM
            Element timeSignatureMap = part.getFirstChildElement("dated").getFirstChildElement("timeSignatureMap");     // get the timeSignatureMap if it has one
            if (timeSignatureMap == null)
                continue;

            // create a rubatoMap in the corresponding MPM part/global
            RubatoMap map;
            if (part.getLocalName().equals("part"))
                map = (RubatoMap) this.performance.getPart(Integer.parseInt(part.getAttributeValue("number"))).getDated().addMap(Mpm.RUBATO_MAP);
            else
                map = (RubatoMap) this.performance.getGlobal().getDated().addMap(Mpm.RUBATO_MAP);

            // fill the rubatoMap
            for (Element timeSignature : timeSignatureMap.getChildElements()) {             // at each time signature (change) we need to reinitialize the rubato so it is always on beat
                double date = Double.parseDouble(timeSignature.getAttributeValue("date"));
                double numerator = Double.parseDouble(timeSignature.getAttributeValue("numerator"));
                int denominator = Integer.parseInt(timeSignature.getAttributeValue("denominator"));

                double quarterCount = (numerator * 4.0) / denominator;
                double residual = quarterCount - Math.floor(quarterCount);
                if (residual != 0.0)        // if this measure has a non-integer number of quarter notes, we need to shift the beginning of the swing a bit so it falls on the beats and not in-between
                    date -= residual * Performer.PPQ;

                map.addRubato(date, Performer.PPQ, 0.7, 0.0, 1.0, true);    // add the swing rubato to the rubatoMap
            }
        }
    }

    /**
     * Add basic humanizing data (timing, articulation and dynamics variation) to the performance.
     * Only global information (the respective imprecisionMaps) are added, hence no local (part-specific) maps are generated.
     */
    private void addHumanizing() {
        ImprecisionMap timing = (ImprecisionMap) this.performance.getGlobal().getDated().addMap(Mpm.IMPRECISION_MAP_TIMING);                // add a timing imprecisionMap
        timing.addDistributionCompensatingTriangle(0.0, 4.0, -184.89, 165.95, -184.89, 165.95, 95.0);   // timing imprecision is best achieved by a correlated distribution; values from Manuel's measurements
//        timing.addDistributionCompensatingTriangle(0.0, 4.0, -75.0, 75.0, -80.0, 80.0, 95.0);   // timing imprecision is best achieved by a correlated distribution
//        timing.addDistributionCompensatingTriangle(0.0, 4.0, -80.0, 80.0, -80.0, 80.0, 300.0);                                              // timing imprecision is best achieved by a correlated distribution

        ImprecisionMap toneDuration = (ImprecisionMap) this.performance.getGlobal().getDated().addMap(Mpm.IMPRECISION_MAP_TONEDURATION);    // add an imprecisionMap for articulation/tone duration
        toneDuration.addDistributionTriangular(0.0, -100.0, 0.0, 0.0, -100.0, 0.0); // values from Manuel's measurements
//        toneDuration.addDistributionTriangular(0.0, -30.0, 0.0, 0.0, -30.0, 0.0);

        ImprecisionMap dynamics = (ImprecisionMap) this.performance.getGlobal().getDated().addMap(Mpm.IMPRECISION_MAP_DYNAMICS);            // add a dynamics imprecisionMap
        dynamics.addDistributionGaussian(0.0, 7.5, -15.0, 15.0);
    }

    /**
     * Generate a new section and phrase structure in MSM phraseMap and sectionMap.
     */
    private void redoSectionAndPhraseStructure() {
        // check if we already have a global phraseMap
        Element phraseMap = this.msm.getGlobal().getFirstChildElement("dated").getFirstChildElement("phraseMap");
        if (phraseMap != null) {
            if (Main.IGNORE_MEI_PHRASING) {     // if we ignore the given phrase structure from the MEI
                phraseMap.detach();             // remove it from the MSM tree
                phraseMap = null;
            } else {
                return;                         // we do not overwrite an existing phraseMap but work on with this one
            }
        }

        // empty the current sectionMap
        Element sectionMap = this.msm.getGlobal().getFirstChildElement("dated").getFirstChildElement("sectionMap");     // get the global sectionMap
        if (sectionMap == null) {                                                                                           // if there is none
            this.msm.getGlobal().getFirstChildElement("dated").appendChild(new Element("sectionMap"));                  // create one
            return;                                                                                                         // done
        }
        sectionMap.removeChildren();                                                                                        // empty the sectionMap

        // add a section from beginning to end
        Element section = new Element("section");
        section.addAttribute(new Attribute("date", "0.0"));
        section.addAttribute(new Attribute("date.end", "" + this.msm.getEndDate()));
        Helper.addUUID(section);
        sectionMap.appendChild(section);

        // find general rests (they mark the end of the preceding phrase; the next phrase, however, starts at the end of the rest!)
        HashMap<Double, ArrayList<Element>> rests = new HashMap<>();            // tuples are (date, list of rests at that date)
        for (Element part : this.msm.getParts()) {
            for (Element rest : part.getFirstChildElement("dated").getFirstChildElement("score").getChildElements("rest")) {
                double date = Double.parseDouble(rest.getAttributeValue("date"));
                if (!rests.containsKey(date))
                    rests.put(date, new ArrayList<>());
                rests.get(date).add(rest);
            }
        }
        SortedMap<Double, Double> phrasesEndStart = new TreeMap<>();            // a sorted map of (end date, start date) tuples for each phrase
        phrasesEndStart.put(0.0, 0.0);                                          // first entry to have a start date of the first phrase
        for (Map.Entry<Double, ArrayList<Element>> entry : rests.entrySet()) {  // check all rests
            if (entry.getValue().size() < 4)                                    // if there are less than 4 rests (we analyze 4-part music here), it is no general rest
                continue;                                                       // ignore it
            double duration = Double.parseDouble(entry.getValue().get(0).getAttributeValue("duration"));
            double startDate = entry.getKey() + duration;   // get the date+duration of the first rest (that of the soprano); this will be the start date of the next phrase
            Element prevSib = Helper.getPreviousSiblingElement(entry.getValue().get(0));
            if (prevSib == null)
                continue;
            double endDate = Double.parseDouble(prevSib.getAttributeValue("date"));
            phrasesEndStart.put(endDate, startDate);                            // the date of the rest is the end date of the preceding phrase, the end of the phrase is the start date of the next phrase
            if (duration >= (Performer.PPQ / this.beatLength))                  // if the rest is equal or longer than a beat
                this.generalRests.add(entry.getKey());                          // store this as the position of a general rest which get a slightly different tempo treatment
        }

        // add breath marks to the phraseEndStart map
        for (Map.Entry<Element, Element> breathNote : this.breathNoteMap.entrySet()) {
            if (breathNote.getValue() == null) {                                // if there is no note associated with the breath
                double date = Double.parseDouble(breathNote.getKey().getAttributeValue("date"));
                phrasesEndStart.put(date, date);                                // its date is the end date of the preceding phrase and the start date of the next phrase
                continue;
            }
            // otherwise the date of the note is the end of the previous phrase, the date+duration of the note is the start of the next phrase
            double endDate = Double.parseDouble(breathNote.getValue().getAttributeValue("date"));
            double startDate = endDate + Double.parseDouble(breathNote.getValue().getAttributeValue("duration"));
            phrasesEndStart.put(endDate, startDate);
        }

        // create and fill the phraseMap
        phraseMap = new Element("phraseMap");
        this.msm.getGlobal().getFirstChildElement("dated").appendChild(phraseMap);
        Map.Entry<Double, Double> previousEntry = null;
        for (Map.Entry<Double, Double> entry : phrasesEndStart.entrySet()) {
            if (previousEntry != null) {
                Element phrase = new Element("phrase");
                phrase.addAttribute(new Attribute("date", "" + previousEntry.getValue()));
                phrase.addAttribute(new Attribute("date.end", "" + entry.getKey()));
                phraseMap.appendChild(phrase);
            }
            previousEntry = entry;
        }
        if (previousEntry != null) {
            Element phrase = new Element("phrase");
            phrase.addAttribute(new Attribute("date", "" + previousEntry.getValue()));
            Elements scorePart1 = this.msm.getParts().get(0).getFirstChildElement("dated").getFirstChildElement("score").getChildElements();
            phrase.addAttribute(new Attribute("date.end", scorePart1.get(scorePart1.size() - 1).getAttributeValue("date")));   // the last phrase ends at the date of the last note in the first part, not after it!
            phraseMap.appendChild(phrase);
        }
    }

    /**
     * Analyze the phrase lengths and find a tempo that matches the breath length into the phrase.
     * Thanks to Andreas Münzmay for this suggestion!
     */
    private void computeBasicTempo() {
        Elements phrases = this.msm.getGlobal().getFirstChildElement("dated").getFirstChildElement("phraseMap").getChildElements();
        if (phrases.size() <= 1) {      // if the whole piece consists of only one phrase
            return;                     // use the default values
        }

        // collect all phrase lengths
        ArrayList<Double> phraseLengths = new ArrayList<>();
        for (Element phrase : phrases) {
            double dateStart = Double.parseDouble(phrase.getAttributeValue("date"));
            double dateEnd = Double.parseDouble(phrase.getAttributeValue("date.end"));
            double phraseLength = dateEnd - dateStart;
            phraseLengths.add(phraseLength);
        }
        if (phraseLengths.size() < 2) {
            return;                     // use the default values
        }

        // match the tempo to the longest phrase length
        double averagePhraseLength = phraseLengths.stream().mapToDouble(Double::doubleValue).average().getAsDouble();
        double phraseTime = Performer.PHRASE_TIME * 1000.0;                                                 // convert phrase time to milliseconds
        Performer.setBasicTempo((60000.0 * averagePhraseLength) / (phraseTime * 4.0 * Performer.PPQ * this.beatLength));    // compute and set the new default tempo
    }

    /**
     * a setter for the basic tempo and related values
     * @param bpm
     */
    public static void setBasicTempo(double bpm) {
        Performer.TEMPO = bpm;
        Performer.TEMPO_AT_PHRASE_END = Performer.TEMPO * Performer.TEMPO_REDUCTION_AT_PHRASE_END;
        Performer.TEMPO_AT_VERSE_END = Performer.TEMPO * Performer.TEMPO_REDUCTION_AT_VERSE_END;

    }

    /**
     * generate phrasing
     */
    private void addPhrasing() {
        // phrasing will be rendered into the global dynamicsMap and tempoMap
        TempoMap tempoMap = (TempoMap) this.performance.getGlobal().getDated().addMap(Mpm.TEMPO_MAP);       // create a global tempoMap that all parts will follow
        DynamicsMap dynamicsMap = (DynamicsMap) this.performance.getGlobal().getDated().addMap(Mpm.DYNAMICS_MAP);

        // generate the phrasing
        Element lastPhraseTempo = null;
        int randomTempoIndex = 0, randomDynamicsIndex = 0;
        for (Element phrase : this.msm.getGlobal().getFirstChildElement("dated").getFirstChildElement("phraseMap").getChildElements("phrase")) {
            double dateStart = Double.parseDouble(phrase.getAttributeValue("date"));
            double dateEnd = Double.parseDouble(phrase.getAttributeValue("date.end"));

            // for each phrase start, create a tempo and dynamics instruction
            double tempoStart = Performer.TEMPO + Performer.tempoRandomizer.getValue(randomTempoIndex++);
            double tempoEnd = tempoStart * Performer.TEMPO_REDUCTION_AT_PHRASE_END;
            double dynamicsStart = Performer.DYNAMICS + Performer.dynamicsRandomizer.getValue(randomDynamicsIndex++);
            double dynamicsEnd = dynamicsStart * Performer.DYNAMICS_REDUCTION_AT_PHRASE_END;
            int index = tempoMap.addTempo(dateStart, ("" + tempoStart), ("" + tempoEnd), this.beatLength, Performer.MEAN_TEMPO_AT_PHRASE_END);    // each phrase is basically a long ritardando that intensifies only near the end
            lastPhraseTempo = tempoMap.getElement(index);                                                                                           // keep the tempo instruction for the last phrase for later reference
            dynamicsMap.addDynamics(dateStart, ("" + dynamicsStart), ("" + dynamicsEnd), 0, Performer.DYNAMICS_PROTRACTION_AT_PHRASE_END, Performer.SUBNOTE_DYNAMICS);  // same for dynamics

            // for each phrase end, create a tempo and dynamics instruction
            tempoMap.addTempo(dateEnd, ("" + (tempoEnd + Performer.tempoRandomizer.getValue(randomTempoIndex++))), this.beatLength);    // TODO: should it better be a tempo halfway between start and end tempo?
            if (Performer.SUBNOTE_DYNAMICS)
                dynamicsMap.addDynamics(dateEnd, ("" + dynamicsEnd), ("" + dynamicsEnd), 0.0, 0.0, Performer.SUBNOTE_DYNAMICS);
            else
                dynamicsMap.addDynamics(dateEnd, ("" + dynamicsEnd));
        }

        // shorten general rests
        for (Double rest : this.generalRests) {
            tempoMap.addTempo(rest, ("" + (Performer.TEMPO_AT_GENERAL_REST + Performer.tempoRandomizer.getValue(randomTempoIndex++))), this.beatLength);
        }

        // for the final ritardando overwrite the following attributes
        double finalTempo = Performer.TEMPO_AT_VERSE_END + Performer.tempoRandomizer.getValue(randomTempoIndex);
        if (lastPhraseTempo != null) {
            lastPhraseTempo.addAttribute(new Attribute("transition.to", ("" + finalTempo)));
            lastPhraseTempo.addAttribute(new Attribute("meanTempoAt", Performer.MEAN_TEMPO_AT_VERSE_END));
        }
        tempoMap.getLastElement().addAttribute(new Attribute("bpm", "" + finalTempo));
    }

    /**
     * Generate tempo and dynamics modulations on the basis of breath and section information.
     */
    private void addPhrasing_Legacy() {
        // read the beatLength from the first timeSignature you find in the MSM
        double beatLength = 0.25;
        for (Element part : this.msm.getRootElement().getChildElements()) {                                             // for the global and all part elements in MSM
            Element timeSignatureMap = part.getFirstChildElement("dated").getFirstChildElement("timeSignatureMap");     // get the timeSignatureMap if it has one
            if (timeSignatureMap == null)
                continue;

            Element timeSignature = timeSignatureMap.getFirstChildElement("timeSignature");
            int denominator = Integer.parseInt(timeSignature.getAttributeValue("denominator"));
            beatLength = 1.0 / denominator;
            break;
        }

        TreeMap<Double, Element> ritPoints = new TreeMap<>();

        Element sectionMap = this.msm.getGlobal().getFirstChildElement("dated").getFirstChildElement("sectionMap");
        if (sectionMap != null) {
            for (Element section : sectionMap.getChildElements()) {
                ritPoints.put(Double.parseDouble(section.getAttributeValue("date.end")), section);
            }
        }

        GenericMap articulationMap = this.mpm.getPerformance(0).getPart(1).getDated().getMap(Mpm.ARTICULATION_MAP); // we take breath articulations only from the first part so all phrasing will follow this
        if (articulationMap != null) {
            ArticulationMap amap = (ArticulationMap) articulationMap;
            for (KeyValue<Double, Element> e : amap.getAllElements()) {
                if (e.getValue().getAttributeValue("name.ref").equals("breath")) {
                    ritPoints.put(e.getKey(), e.getValue());
                }
            }
        }

        GenericMap score = GenericMap.createGenericMap(this.msm.getParts().get(0).getFirstChildElement("dated").getFirstChildElement("score")); // the score of the first part (usually the melody) is needed as we will later search for the last note/rest in a section

        DynamicsMap dynamicsMap = (DynamicsMap) this.performance.getGlobal().getDated().addMap(Mpm.DYNAMICS_MAP);
        dynamicsMap.addDynamics(0.0, "" + Performer.DYNAMICS);

        TempoMap tempoMap = (TempoMap) this.performance.getGlobal().getDated().addMap(Mpm.TEMPO_MAP);       // create a global tempoMap that all parts will follow
        tempoMap.addTempo(0.0, ("" + Performer.TEMPO), beatLength);                                         // set initial tempo

        // create the tempo instructions at the chosen points
        double prevEntryDate = 0.0;                                                                         // we keep the date where the previous ritardando ended so we do not start another ritardando before it
        int verseNumber = -1;
        for (Map.Entry<Double, Element> entry : ritPoints.entrySet()) {
            boolean nextSection = false;
            boolean attacca = false;
            double date, transitionTo = Performer.TEMPO, meanTempoAt = 0.7;
            switch (entry.getValue().getLocalName()) {
                case "articulation": {                                                                      // breath articulations are goal of subtle ritardandi
                    date = entry.getKey() - (Performer.PPQ * 4.0 * beatLength * 2.0);                       // start the ritardando a bit before the position, the final factor is the amount of beats before the target point where the ritardando starts
                    transitionTo = Performer.TEMPO * Math.pow(Performer.TEMPO_REDUCTION_AT_PHRASE_END, Performer.TEMPO_MODULATION_INTENSITY);
                    meanTempoAt = 0.7;
                    break;
                }
                case "section": {                                                                           // stronger ritardandi at section ends
                    Element lastBefore = score.getElement(score.getElementIndexBefore(entry.getKey()));     // find the last note/rest before the end of the section, this becomes the target point of the ritardando
                    date = Double.parseDouble(lastBefore.getAttributeValue("date")) - (Performer.PPQ * 4.0 * beatLength * 4.0);  // start the ritardando a bit before the position, the final factor is the amount of beats before the target point where the ritardando starts
                    nextSection = true;
                    attacca = Helper.getAttributeValue("attacca", entry.getValue()).equals("true");
                    if (!attacca) {                                                                         // if the section should start attacca, we do not perform phrasing here but play through
                        transitionTo = Performer.TEMPO * Math.pow(Performer.TEMPO_REDUCTION_AT_VERSE_END, Performer.TEMPO_MODULATION_INTENSITY);
                        meanTempoAt = 0.75;
                    }
                    break;
                }
                default:
                    continue;
            }

            if (date < prevEntryDate)                                                                       // make sure we do not start this ritardando before the end of the previous
                date = prevEntryDate;

            prevEntryDate = entry.getKey();

            if (!attacca) {
                tempoMap.addTempo(date, ("" + Performer.TEMPO), "" + transitionTo, beatLength, meanTempoAt);    // create the ritardando
                tempoMap.addTempo(entry.getKey(), ("" + Performer.TEMPO), beatLength);                          // a tempo after the ritardando
            }

            dynamicsMap.addDynamics(date, "" + Performer.DYNAMICS, "" + (Performer.DYNAMICS * 0.7), 0.4, -0.2); // add a slight decrescendo to the dynamicsMap
            if (nextSection) {                                                                              // if a new section begins
                verseNumber++;                                                                              // increase the verse counter
                Performer.DYNAMICS = Performer.dynamicsRandomizer.getValue(verseNumber);                                // set a new basic dynamics value
            }
            dynamicsMap.addDynamics(entry.getKey(), "" + Performer.DYNAMICS);                               // back to normal dynamics
        }

        // increase the final ritardando
        Element lastTempo = tempoMap.getElement(tempoMap.size() - 2);
        lastTempo.getAttribute("transition.to").setValue("" + (Performer.TEMPO * Math.pow(Performer.TEMPO_REDUCTION_AT_VERSE_END, Performer.FINAL_RITARDANDO_INTENSITY)));
        lastTempo.getAttribute("meanTempoAt").setValue("0.75");
    }

    /**
     * Check the mei for the type of barline which gives us a clue for metrical accentuation strength.
     * If the music has a real time signature, the metrical accentuation is stronger than with an implicit
     * time signature (mensural barlines). Invisible barlines mean no time signature, hence no metrical accentuation.
     */
    private BarMethod getBarMethod() {
        Element score = this.mei.getMusic().getFirstChildElement("body", this.MEI_NAMESPACE).getFirstChildElement("mdiv", this.MEI_NAMESPACE).getFirstChildElement("score", this.MEI_NAMESPACE);

        // check if we have mostly invisible barlines; this indicates that there is no real time signature
        int invisibleBarlines = 0;      // this increases with each invisible barline and decreases with each visible barline
        Nodes measures = score.query("descendant::*[local-name()='measure']");
        for (Node m : measures) {
            Element measure = (Element) m;

            Attribute left = measure.getAttribute("left");
            if (left != null) {
                if (left.getValue().equals("invis"))
                    invisibleBarlines++;
                else
                    invisibleBarlines--;
            }

            Attribute right = measure.getAttribute("right");
            if (right != null) {
                if (right.getValue().equals("invis"))
                    invisibleBarlines++;
                else
                    invisibleBarlines--;
            }
        }
        if (invisibleBarlines > 0)
            return BarMethod.none;

        // check if we have a staffGrp that would define a specific barline method
        Element staffGrp = score.getFirstChildElement("scoreDef", this.MEI_NAMESPACE).getFirstChildElement("staffGrp", this.MEI_NAMESPACE);
        if (staffGrp == null)
            return BarMethod.none;

        // check if we have mensural barlines
        Attribute barMethod = staffGrp.getAttribute("bar.method");
        if ((barMethod != null) && (barMethod.getValue().equals("mensur")))
            return BarMethod.mensur;

        if ((barMethod != null) && (barMethod.getValue().equals("takt")))
            return BarMethod.takt;


        return BarMethod.common;      // in any other case the barline method is assumed to be takt
    }

    private enum BarMethod {
        mensur,
        takt,
        common,
        none
    }

    /**
     * Some time signature changes emerged from fragmented measures. Fix this here.
     */
    private void uniteFragmentedTimeSignatures() {
        Element tsm = this.msm.getGlobal().getFirstChildElement("dated").getFirstChildElement("timeSignatureMap");
        if (tsm == null)
            return;

        GenericMap tsMap = GenericMap.createGenericMap(tsm);

        // process fragmented time signatures
        for (int t = tsMap.size() - 3; t >= 0; --t) {                   // this goes from back to front to avoid false positive cases of time signature combinations
            Element ts0 = tsMap.getElement(t);
            Element ts1 = tsMap.getElement(t + 1);                  // ts1 and ts2 are the ones in question; do they combine to a full measure according to ts0?
            Element ts2 = tsMap.getElement(t + 2);

            if (ts2 == null)
                continue;

            String denom0 = ts0.getAttributeValue("denominator");
            String denom1 = ts1.getAttributeValue("denominator");
            String denom2 = ts2.getAttributeValue("denominator");
            if (!denom0.equals(denom1) || !denom0.equals(denom2))       // if the denominators are not equal
                continue;                                               // the time signatures should not be combined

            double num0 = Double.parseDouble(ts0.getAttributeValue("numerator"));
            double num1 = Double.parseDouble(ts1.getAttributeValue("numerator"));
            double num2 = Double.parseDouble(ts2.getAttributeValue("numerator"));
            if (num0 != num1 + num2)                                    // if the sum of the numerators does not combine to a full measure in the previous time signature
                continue;                                               // we cannot delete the latter ones

            // remove the fragmented time signatures
            tsMap.removeElement(ts1);
            tsMap.removeElement(ts2);

            // check for consecutive equal, i.e. redundant, time signatures
            Element ts3 = tsMap.getElement(t + 3);                  // this might be removed, if ts1 and ts2 get removed ant ts3 equals ts0
            if ((ts3 != null) && (denom0.equals(ts3.getAttributeValue("denominator")) && num0 == Double.parseDouble(ts3.getAttributeValue("numerator"))))
                tsMap.removeElement(ts3);
        }
    }

    /**
     * Generate metrical accentuation in global and the parts from their corresponding time signatures.
     */
    private void addMetricalAccentuation() {
        double accentuationStrength = 1.0;
        switch (this.getBarMethod()) {
            case mensur:
                accentuationStrength = 0.5;     // reduced accentuation strength
                break;
            case none:
            case takt:
                return;                         // no accentuation at all
            default:                            // keep full accentuation strength
        }


        // create metrical accentuation patterns for time signatures with (2, 3,) 4, 6, 8, and 9 beats
        MetricalAccentuationStyle style = (MetricalAccentuationStyle) this.performance.getGlobal().getHeader().addStyleDef(Mpm.METRICAL_ACCENTUATION_STYLE, "CaP"); // create a global metricalAccentuation to be filled with accentuationPatternDefs
        HashMap<Double, String> patternForBeats = new HashMap<>();

        AccentuationPatternDef unoPatternDef = AccentuationPatternDef.createAccentuationPatternDef("uno", 1.0);
        unoPatternDef.addAccentuation(1.0, 0.0, -1.0, 1.0);
        style.addDef(unoPatternDef);
        patternForBeats.put(unoPatternDef.getLength(), unoPatternDef.getName());

//        AccentuationPatternDef binaryPatternDef = AccentuationPatternDef.createAccentuationPatternDef("binary", 2.0);       // this pattern is generated on the fly
//        binaryPatternDef.addAccentuation(1.0, 1.0, -1.0, 1.0);
//        style.addAccentuationPatternDef(binaryPatternDef);
//        patternForBeats.put(binaryPatternDef.getLength(), binaryPatternDef.getName());

//        AccentuationPatternDef ternaryPatternDef = AccentuationPatternDef.createAccentuationPatternDef("ternary", 3.0);     // this pattern is generated on the fly
//        ternaryPatternDef.addAccentuation(1.0, 1.0, -1.0, 1.0);
//        style.addAccentuationPatternDef(ternaryPatternDef);
//        patternForBeats.put(ternaryPatternDef.getLength(), ternaryPatternDef.getName());

        AccentuationPatternDef quadPatternDef = AccentuationPatternDef.createAccentuationPatternDef("quad", 4.0);
        quadPatternDef.addAccentuation(1.0, 1.0, 0.0, -1.0);                        //  1.0 -      /
        quadPatternDef.addAccentuation(3.0, 0.0, -1.0, 1.0);                        //  0.5    _  /
        style.addDef(quadPatternDef);                            // -0.5  \   /
        patternForBeats.put(quadPatternDef.getLength(), quadPatternDef.getName());  // -1.0   \ /

        AccentuationPatternDef hexPatternDef = AccentuationPatternDef.createAccentuationPatternDef("hex", 6.0);
        hexPatternDef.addAccentuation(1.0, 1.0, 0.0, -1.0);                        //  1.0 -      /
        hexPatternDef.addAccentuation(4.0, 0.0, -1.0, 1.0);                        //  0.5    _  /
        style.addDef(hexPatternDef);                            // -0.5  \   /
        patternForBeats.put(hexPatternDef.getLength(), hexPatternDef.getName());   // -1.0   \ /

        AccentuationPatternDef octPatternDef = AccentuationPatternDef.createAccentuationPatternDef("oct", 8.0);
        octPatternDef.addAccentuation(1.0, 1.0, 0.0, -1.0);                         //  1.0 -               /
        octPatternDef.addAccentuation(3.0, 0.0, 0.0, 0.5);                          //  0.5         /-     /
        octPatternDef.addAccentuation(5.0, 0.5, 0.0, -1.0);                         //  0.0  \  -  /  \  -/
        octPatternDef.addAccentuation(7.0, 0.0, 0.0, 1.0);                          //  0.5   \   /    \
        style.addDef(octPatternDef);                             // -1.0    \ /      \
        patternForBeats.put(octPatternDef.getLength(), octPatternDef.getName());

        AccentuationPatternDef nonPatternDef = AccentuationPatternDef.createAccentuationPatternDef("non", 9.0);
        nonPatternDef.addAccentuation(1.0, 1.0, 0.0, -1.0);                         //  1.0 -           /
        nonPatternDef.addAccentuation(4.0, 0.0, 0.0, -1.0);                         //  0.5            /
        nonPatternDef.addAccentuation(7.0, -0.5, -0.5, 1.0);                        //  0.0  \  -\    /
        style.addDef(nonPatternDef);                             // -0.5   \   \ -/
        patternForBeats.put(nonPatternDef.getLength(), nonPatternDef.getName());    // -1.0    \   \

        // process the parts and global and generate their metricalAccentuationMaps
        for (Element part : this.msm.getRootElement().getChildElements()) {                                             // for the global and all part elements in MSM
            Element timeSignatureMap = part.getFirstChildElement("dated").getFirstChildElement("timeSignatureMap");     // get the timeSignatureMap if it has one, otherwise we generate no metrical accentuation
            if (timeSignatureMap == null)
                continue;

            // create a metricalAccentuationMap in the corresponding MPM part/global
            MetricalAccentuationMap map;
            if (part.getLocalName().equals("part"))
                map = (MetricalAccentuationMap) this.performance.getPart(Integer.parseInt(part.getAttributeValue("number"))).getDated().addMap(Mpm.METRICAL_ACCENTUATION_MAP);
            else
                map = (MetricalAccentuationMap) this.performance.getGlobal().getDated().addMap(Mpm.METRICAL_ACCENTUATION_MAP);

            map.addStyleSwitch(0.0, style.getName());   // add the style switch at the beginning so we can apply the accentuationPatternDefs in there

            // is the first measure metcon=false and the second is not? Then it is an upbeat!
            boolean startsWithUpbeat = false;
            Element globalSection = this.mei.getMusic().getFirstChildElement("body", this.MEI_NAMESPACE).getFirstChildElement("mdiv", this.MEI_NAMESPACE).getFirstChildElement("score", this.MEI_NAMESPACE).getFirstChildElement("section", this.MEI_NAMESPACE);
            Element firstMeasure = globalSection.getFirstChildElement("measure", this.MEI_NAMESPACE);
            if (firstMeasure == null)
                firstMeasure = globalSection.getFirstChildElement("section", this.MEI_NAMESPACE).getFirstChildElement("measure", this.MEI_NAMESPACE);
            Attribute metcon1 = firstMeasure.getAttribute("metcon");
            if ((metcon1 != null) && metcon1.getValue().equals("false")) {                              // if the first measure does not follow the meter
                Element secondMeasure = Helper.getNextSiblingElement("measure", firstMeasure);
                if (secondMeasure != null) {
                    Attribute metcon2 = secondMeasure.getAttribute("metcon");
                    if ((metcon2 == null) || metcon2.getValue().equals("true")) {                           // but the second measure does
                        startsWithUpbeat = true; // mark the first time signature as an upbeat
                    }
                }
            }

            // for each timeSignature in the MSM timeSignatureMap create an accentuationPattern in the MPM metricalAccentuationMap
            Elements timeSignatures = timeSignatureMap.getChildElements();
            for (int ts = 0; ts < timeSignatures.size(); ++ts) {        // all remaining measures get the normal treatment
                Element timeSignature = timeSignatures.get(ts);
                double date = Double.parseDouble(timeSignature.getAttributeValue("date"));
                double numerator = Double.parseDouble(timeSignature.getAttributeValue("numerator"));

                String patternName = null;

                if ((ts == 0) && startsWithUpbeat) {                    // if it is an upbeat measure, create and apply an upbeat pattern
                    AccentuationPatternDef upbeatPatternDef = AccentuationPatternDef.createAccentuationPatternDef("upbeat", numerator);
                    upbeatPatternDef.addAccentuation(1.0, -1.0, -1.0, 1.0);
                    style.addDef(upbeatPatternDef);
                    patternName = upbeatPatternDef.getName();
                } else {                                                // if we have no accentuation pattern for this type of time signature, generate one
                    patternName = patternForBeats.get(numerator);
                    if (patternName == null) {
                        if (numerator != Math.floor(numerator)) {                                           // if the numerator of this time signature is not integer it is probably a pickup, hence, doesn't get an accentuation on the first note
                            AccentuationPatternDef pickupPatternDef = AccentuationPatternDef.createAccentuationPatternDef(("" + numerator), numerator);
                            pickupPatternDef.addAccentuation(1.0, -1.0, -1.0, 1.0);
                            style.addDef(pickupPatternDef);
                            patternForBeats.put(pickupPatternDef.getLength(), pickupPatternDef.getName());
                            patternName = pickupPatternDef.getName();
                        } else {                                                                            // in every other case we put full accentuation on the first beat
                            AccentuationPatternDef defaultPatternDef = AccentuationPatternDef.createAccentuationPatternDef(("" + numerator), numerator);
                            defaultPatternDef.addAccentuation(1.0, 1.0, -1.0, 1.0);                             //  1.0 -   /
                            style.addDef(defaultPatternDef);                                                                                    //  0.5    /
                            patternForBeats.put(defaultPatternDef.getLength(), defaultPatternDef.getName());                                    // -0.5   /
                            patternName = defaultPatternDef.getName();                                                                          // -1.0  /
                        }
                    }
                }
                map.addAccentuationPattern(date, patternName, Performer.METRICAL_ACCENTUATION_SCALE * accentuationStrength, true, true);   // add entry to the metricalAccentuationMap
            }
        }
    }

    /**
     * This method checks whether the velocity values hold the specified limits. If not, they are scaled down.
     * @param min
     * @param max
     */
    public static void fitVelocities(Msm msm, double min, double max) {
        // if min is greater than max, switch the values
        if (min > max) {
            double x = min;
            min = max;
            max = x;
        }

        // find all velocity attributes and get their values
        ArrayList<KeyValue<Double, Attribute>> velocities = new ArrayList<>();      // a list of tuplets with the attributes and their value
        double lowest = Double.MAX_VALUE;                                           // this will get the lowest velocity value
        double highest = Double.MIN_VALUE;                                          // this will get the highest velocity value
        Elements parts = msm.getParts();
        for (Element part : parts) {                                                // in each part
            Element dated = Helper.getFirstChildElement("dated", part);             // get the part's dated environment
            if (dated == null)
                continue;
            Element score = Helper.getFirstChildElement("score", dated);            // get the score element
            if (score == null)
                continue;
            LinkedList<Element> notes = Helper.getAllChildElements("note", score);  // get all note elements in the score
            for (Element note : notes) {                                            // for each note
                Attribute velAtt = Helper.getAttribute("velocity", note);           // get its velocity attribute
                if (velAtt == null)
                    continue;
                double value = Double.parseDouble(velAtt.getValue());               // read the attribute's value into a double
                if (value < lowest)                                                 // if this is lower than the lowest so far
                    lowest = value;                                                 // keep the value
                else if (value > highest)                                           // if the value is greater than the highest so far
                    highest = value;                                                // keep the value
                velocities.add(new KeyValue<>(value, velAtt));                      // create a tuplet and add it to the ArrayList
            }
        }

        boolean scaleLowerHalf = (lowest < min);
        boolean scaleUpperHalf = (highest > max);
        if (!(scaleLowerHalf || scaleUpperHalf))                                    // if the velocity values hold the limits
            return;                                                                 // we are done

        // otherwise we need to apply compression
        System.out.println("Warning: velocity values [" + lowest + ", " + highest + "] break the specified limits [" + min + ", " + max + "] and get compressed.");
        Performer.computePartwiseCompression(velocities, lowest, highest, min, max);
    }

    /**
     * This method computes a compression of a limited input domain (lowest &le; x &le; highest) to a limited output domain (limited by min and max).
     * It uses a partwise linear mapping. It tries to limit the range of compression depending on how much the limits are broken by lowest and highest value.
     * @param attributes the values to be mapped according to the compession
     * @param lowest
     * @param highest
     * @param min
     * @param max
     */
    private static void computePartwiseCompression(ArrayList<KeyValue<Double, Attribute>> attributes, double lowest, double highest, double min, double max) {
        // on the basis of the lowest and highest value (the extremes of the input domain), compute the range to be compresed, i.e. [lowest, lowerCompMax] and [upperCompMin, highest]
        double lowerCompMax = min;
        double upperCompMin = max;
        if (lowest < min)
            lowerCompMax = max - (((max - min) * (max - min)) / (max - lowest));
        if (highest > max)
            upperCompMin = min + (((max - min) * (max - min)) / (highest - min));
        if (lowerCompMax > upperCompMin) {
            lowerCompMax = (lowerCompMax + upperCompMin) / 2.0;
            upperCompMin = lowerCompMax;
        }

        // the rolloffFactor (0.0 < rolloffFactor < 1.0) lowers the degree of compression for values within the range [min, max] the higher it is set; values beyond the limits will be more compressed
        double rolloffFactor = 0.66;
        double upperRolloff1 = 0.0, upperRolloff2 = 0.0, lowerRolloff1 = 0.0, lowerRolloff2 = 0.0;
        double upperRaise = upperCompMin;
        double lowerRaise = min;
        if (highest > max) {
            upperRolloff1 = ((max - upperCompMin) * rolloffFactor) / (max - upperCompMin);
            upperRolloff2 = ((1.0 - rolloffFactor) * (max - upperCompMin)) / (highest - max);
            upperRaise = upperCompMin + (rolloffFactor * (max - upperCompMin));
//        } else {
//            upperRolloff1 = (max - upperCompMin) / (highest - upperCompMin);
//            upperRolloff2 = upperRolloff1;
        }
        if (lowest < min) {
            lowerRolloff1 = ((lowerCompMax - min) * (1.0 - rolloffFactor)) / (min - lowest);
            lowerRolloff2 = ((lowerCompMax - min) * rolloffFactor) / (lowerCompMax - lowest);
            lowerRaise = min + ((lowerCompMax - min) * (1.0 - rolloffFactor));
//        } else {
//            lowerRolloff1 = (lowerCompMax - min) / (lowerCompMax - lowest);
//            lowerRolloff2 = lowerRolloff1;
        }

        for (KeyValue<Double, Attribute> attribute : attributes) {
            double x = attribute.getKey();
            double result = x;

            if (x < lowerCompMax) {
//                result = (((lowerCompMax - min) * (x - lowest)) / (lowerCompMax - lowest)) + min;                                       // interpolation with one linear segment
                result = (x >= min) ? (lowerRolloff2 * (x - min)) + lowerRaise : (lowerRolloff1 * (x - lowest)) + min;                  // interpolation with two linear segments
            } else if (x > upperCompMin) {
//                result = (((max - upperCompMin) * (x - upperCompMin)) / (highest - upperCompMin)) + upperCompMin;                       // interpolation with one linear segment
                result = (x <= max) ? (upperRolloff1 * (x - upperCompMin)) + upperCompMin : (upperRolloff2 * (x - max)) + upperRaise;   // interpolation with two linear segments
            }
            else {
                continue;
            }
//            System.out.println("DEBUG " + x + " -> " + attribute.getValue().getValue());
            attribute.getValue().setValue(Double.toString(result));
        }
    }
}
