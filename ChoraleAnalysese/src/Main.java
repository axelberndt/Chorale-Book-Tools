import keySignatures.*;
import meico.Meico;
import meico.mei.Mei;
import meico.mpm.elements.maps.GenericMap;
import meico.msm.Msm;
import meterSignatures.MeterSignature;
import meterSignatures.MeterSignatures;
import nu.xom.*;
import org.xml.sax.SAXException;
import voiceAnalyses.MelodicIntervals;
import voiceAnalyses.MelodicIntervalsAnalyses;
import voiceAnalyses.PitchHistograms;
import voiceAnalyses.VoiceDistances;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Main class and entry point for program execution.
 * TODO: add homophony/polyphony analysis; which voice plays which chord tone; number of tones in chords
 * @author Axel Berndt
 */
public class Main {
    public static final String VERSION = "0.0.0";

    private KeySignatures keySignatures = null;                 // which key signatures are present and for how many measures?
    private MeterSignatures meterSignatures = null;             // which meter signatures are present and for how many measures?
    private PitchHistograms pitchHistograms = null;             // compute the pitch histogram for each musical voice
    private VoiceDistances voiceDistances = null;               // compute the distances between neighboring voices, ie.e soprano-alto, alto-tenor, tenor-bass
    private MelodicIntervalsAnalyses melodicity = null;         // compute melodic intervals for each voice
    private boolean chords = false;                             // perform harmonic analysis
    private boolean chordSequences = false;                     // compute a Markov model of the chord sequences
    private boolean nonchordTones = false;                      // get a list and classification of the nonchord tones

    private final HashMap<Mei, List<Msm>> meis2Msms = new HashMap<>(); // the MEI files to be analyzed

    /**
     * entry point of the program, parses the command line arguments, and performs the requested analyses
     * @param args
     */
    public static void main(String[] args) {
        Main main = new Main();                                 // an instance of Main to hold the data to be analyzed, the tasks and the results

        for (int i = 0; i < args.length; ++i) {
            switch (args[i]) {
                case "-?":
                case "-help":
                    Main.printHelpText();
                    break;

                case "-key-signatures":
                    main.keySignatures = new KeySignatures();
                    break;

                case "-meter-signatures":
                    main.meterSignatures = new MeterSignatures();
                    break;

                case"-pitch-histograms":
                    main.pitchHistograms = new PitchHistograms();
                    break;

                case "-voice-distances":
                    main.voiceDistances = new VoiceDistances();
                    break;

                case "-melodicity":
                    main.melodicity = new MelodicIntervalsAnalyses();
                    break;

                case "-chords":
                    main.chords = true;
                    break;

                case "chord-sequences":
                    main.chordSequences = true;
                    break;

                case "-nonchord-tones":
                    main.nonchordTones = true;
                    break;

                default:
                    File f = new File(args[i]);
                    if (f.isDirectory())
                        System.out.println(main.loadFolder(f) + " files loaded.");
                    else if (f.isFile())
                        System.out.println(main.loadFile(f) ? (f.getAbsolutePath() + " loaded.") : "Error: Invalid file.");
                    else
                        System.err.println("Error: Invalid argument: " + args[i] + ".");
            }
        }

        main.analyze();     // perform all requested analyses

        System.exit(0);
    }

    /**
     * perform all requested analyses on the MEI files
     */
    private void analyze(){
        // preprocessing
        for (Mei mei : this.meis2Msms.keySet()) {
            removeEndingsAndRepetitionmarks(mei);                               // the sequence of the chorale is encoded in <expan>; repetition marks and endings should not be present in the through-composed version, thus we remove them hereby
            addInvisMeterSigBeforeFirstMeasure(mei);
            this.meis2Msms.get(mei).forEach(msm -> this.uniteFragmentedTimeSignatures(msm));   // cleanup fragmented measures (e.g., at repetitions) by uniting them wherever they sum up to the time signature before that position
        }

        System.out.println("\n---------------------------------------------------------------------------");

        // run the analyses

        if (this.keySignatures != null)
            this.keySignatureAnalysis();

        System.out.println("\n---------------------------------------------------------------------------");

        if (this.meterSignatures != null)
            this.meterSignatureAnalysis();

        System.out.println("\n---------------------------------------------------------------------------");

        if (this.pitchHistograms != null)
            this.pitchHistogramAnalysis();

        System.out.println("\n---------------------------------------------------------------------------");

        if (this.voiceDistances != null)
            this.voiceDistancesAnalysis();

        System.out.println("\n---------------------------------------------------------------------------");

        if (this.melodicity != null)
            this.melodicityAnalysis();

        System.out.println("\n---------------------------------------------------------------------------");

        // TODO: more analyses ...

        System.out.println("\n---------------------------------------------------------------------------");
    }

    /**
     * run key signature analysis
     */
    private void keySignatureAnalysis() {
//        System.out.println("\n");
        for (Mei mei :  this.meis2Msms.keySet()) {
//            System.out.println("Processing " + mei.getFile().getName());
            KeySignatures kss = KeySignatures.analyze(mei);
//            KeySignatures kss = KeySignatures.analyze(this.meis2Msms.get(mei).get(0));
            if (kss != null)
                this.keySignatures.merge(kss);
        }

        System.out.println("\nKey Signature Statistics");
        System.out.println(this.keySignatures.size() + " different key signatures found:\n");
        for (KeySignature ks : this.keySignatures.keySet())
            System.out.println(this.keySignatures.get(ks).size() + "\t" + ks);
    }

    /**
     * run meter signature analysis
     */
    private void meterSignatureAnalysis() {
//        System.out.println("\n");
        int piecesWithNoMeterSig = 0;
        for (Mei mei : this.meis2Msms.keySet()) {
//            System.out.println("Processing " + mei.getFile().getName());
            MeterSignature meterSignature = MeterSignatures.hasMeterSignature(mei);
            MeterSignatures meterSignaturesOfThis = new MeterSignatures();
            if (meterSignature == null) {                                       // the music has no defined meter signature, so we add a default one
                ++piecesWithNoMeterSig;
            } else {                                                            // otherwise we have to do some work, though, we check only the first mdiv/MSM, others are only verses with variants
                meterSignaturesOfThis = MeterSignatures.analyze(this.meis2Msms.get(mei).get(0));    // get the meter signatures in this music and for how many measures it plays
            }
//            System.out.println("    " + meterSignaturesOfThis.toString());
            this.meterSignatures.merge(meterSignaturesOfThis);
        }
        System.out.println("\nMeter Signature Statistics");
        System.out.println(piecesWithNoMeterSig + " pieces without meter signature.");
        System.out.println((this.meis2Msms.size() - piecesWithNoMeterSig) + " pieces with meter signature(s).");
        System.out.println(this.meterSignatures.size() + " different meter signatures found:\n");
        for (MeterSignature ms :  this.meterSignatures.keySet())
            System.out.println(ms + "\t" + this.meterSignatures.get(ms));
    }

    /**
     * run a voice range analysis, it will print a histogram of MIDI pitches (int array) for each musical voice
     */
    private void pitchHistogramAnalysis() {
        for (Mei mei : this.meis2Msms.keySet()) {
//            System.out.println("Processing " + mei.getFile().getName());
            PitchHistograms pitchHistogramsOfThis = PitchHistograms.analyze(this.meis2Msms.get(mei).get(0));
            if (pitchHistogramsOfThis == null)
                continue;
//            System.out.println(pitchHistogramsOfThis.toString());
            this.pitchHistograms.merge(pitchHistogramsOfThis);
        }

        System.out.println("\nPitch Histogram per Voice");
        for (String key : this.pitchHistograms.keySet())
            System.out.println("\t" + this.pitchHistograms.get(key).toString());
    }

    /**
     * run an analysis of the distances between neighboring musical voices
     */
    private void voiceDistancesAnalysis() {
        for (Mei mei : this.meis2Msms.keySet()) {
//            System.out.println("\nProcessing " + mei.getFile().getName());
            VoiceDistances voiceDistancesOfThis = VoiceDistances.analyze(this.meis2Msms.get(mei).get(0));
//            System.out.println(voiceDistancesOfThis.printStatistics());
            this.voiceDistances.merge(voiceDistancesOfThis);
        }

        System.out.println("\nVoice Distances");
        System.out.println(this.voiceDistances.printStatistics());
    }

    /**
     * run an analysis of the melodic intervals in each musical voice
     */
    private void melodicityAnalysis() {
        for (Mei mei : this.meis2Msms.keySet()) {
            System.out.println("\nProcessing " + mei.getFile().getName());
            this.melodicity.analyze(this.meis2Msms.get(mei).get(0));
        }

        System.out.println("\nMelodic Intervals");
        System.out.println(this.melodicity);
    }

    /**
     * As the MEI will be expanded into a through-composed version, we need to remove repetition marks and endings.
     * @param mei
     */
    private static void removeEndingsAndRepetitionmarks(Mei mei) {
        Element music = mei.getMusic();

        // remove bar lines with repetition marks
        Nodes measures = music.query("descendant::*[local-name()='measure']");
        for (Node m : measures) {
            Element measure = (Element) m;
            Attribute barline = measure.getAttribute("left");
            if ((barline != null) && (barline.getValue().equals("rptend") || barline.getValue().equals("rptboth") || barline.getValue().equals("rptstart")))
                barline.detach();
            barline = measure.getAttribute("right");
            if ((barline != null) && (barline.getValue().equals("rptend") || barline.getValue().equals("rptboth") || barline.getValue().equals("rptstart")))
                barline.detach();
        }

        // remove endings from the XML tree
        Nodes endings = music.query("descendant::*[local-name()='ending']");
        for (Node e : endings) {
            Element ending = (Element) e;
            // move its children to the ending's parent at the same position as the ending
            Element parent = (Element) ending.getParent();
            int index = parent.indexOf(ending);
            Elements children = ending.getChildElements();
            for (Element child : children) {
                child.detach();
                parent.insertChild(child, index++);
            }
            ending.detach();
        }
    }

    /**
     * helper method to ensure metrical coherence in case of repetitions
     * @param mei
     */
    private static void addInvisMeterSigBeforeFirstMeasure(Mei mei) {
        Element score = mei.getMusic().getFirstChildElement("body", mei.getRootElement().getNamespaceURI())
                .getFirstChildElement("mdiv", mei.getRootElement().getNamespaceURI())
                .getFirstChildElement("score", mei.getRootElement().getNamespaceURI());

        Element generalSection = score.getFirstChildElement("section", mei.getRootElement().getNamespaceURI());

        Element expansion = generalSection.getFirstChildElement("expansion", mei.getRootElement().getNamespaceURI());
        if (expansion == null)              // no expansion means no repetition
            return;                         // hence, no issues here

        Element meterSig = score.getFirstChildElement("scoreDef", mei.getRootElement().getNamespaceURI()).getFirstChildElement("meterSig", mei.getRootElement().getNamespaceURI());
        if (meterSig == null)               // no time signature at the beginning
            return;                         // nothing to copy in the first subsection

        Element subsection = generalSection.getFirstChildElement("section", mei.getRootElement().getNamespaceURI());

        // add an invisible copy of the meterSig (surrounded by a scoreDef) before the first measure
        Element scoreDef2 = new Element("scoreDef", mei.getRootElement().getNamespaceURI());
        Element meterSig2 = meterSig.copy();
//        meterSig2.addAttribute(new Attribute("visible", "false"));
        scoreDef2.appendChild(meterSig2);
        subsection.insertChild(scoreDef2, 0);
    }

    /**
     * Some time signature changes emerged from fragmented measures. Fix this here.
     */
    private void uniteFragmentedTimeSignatures(Msm msm) {
        Element tsm = msm.getGlobal().getFirstChildElement("dated").getFirstChildElement("timeSignatureMap");
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
     * load all MEI files from the folder
     * @param folder
     * @return
     */
    private int loadFolder(File folder) {
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null)
            return 0;

        int filesLoaded = 0;
        for (File file : listOfFiles)                       // for each file in the folder
            filesLoaded += this.loadFile(file) ? 1 : 0;     // try loading it

        return filesLoaded;
    }

    /**
     * load file
     * @param file
     * @return success
     */
    private boolean loadFile(File file) {
        Mei mei;
        System.out.println("Loading \"" + file.getAbsolutePath() + "\"");
        try {
            mei = new Mei(file);
        } catch (IOException | ParsingException | SAXException | ParserConfigurationException e) {
            e.printStackTrace();
            return false;
        }

        if (!mei.getRootElement().getLocalName().equals("mei")) {
            System.err.println("Error: \"" + file.getAbsolutePath() + "\" is no MEI file.");
            return false;
        }

        mei.resolveCopyofsAndSameas();                                      // this is also done during MEI-to-MSM conversion, execute this line if the MEI data should be altered before further analyses
//        mei.resolveExpansions();                                            // execute this line if the MEI data should be altered before further analyses
        mei.layersToStaffs();                                               // separate individual voices from polyphonic staffs
        this.meis2Msms.put(mei, mei.exportMsm(720, true, true, true));   // we immediately also create the MSMs, as they are needed for the analyses
        return true;
    }

    /**
     * print the help text to the console
     */
    private static void printHelpText() {
        System.out.println("ChoraleAnalyses v " + Main.VERSION + "\nMeico: MEI Converter v" + Meico.version);
        System.out.println("[-?] or [-help]                show this help text");
        System.out.println("[-key-signatures]              get key signatures");
        System.out.println("[-meter-signatures]            get meter signatures");
        System.out.println("[-pitch-histograms]            get pitch histograms for each musical voice");
        System.out.println("[-voice-distances]             get distances between voices");
        System.out.println("[-melodicity]                  get melodic intervals");
        System.out.println("[-chords]                      get harmonic analysis");
        System.out.println("[-chord-sequences]             get Markov analysis of chord sequences");
        System.out.println("[-nonchord-tones]              get list and classification of nonchord tones");
        System.out.println("\nThe final argument should always be a path to a valid mei file (e.g., \"C:\\myMeiCollection\\test.mei\"); always in quotes! This is the only mandatory argument if you want to process something.");
    }
}
