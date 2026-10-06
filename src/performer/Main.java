package performer;

import meico.Meico;
import meico.mei.Helper;
import meico.mei.Mei;
import meico.mpm.Mpm;
import meico.mpm.elements.Part;
import meico.mpm.elements.Performance;
import meico.mpm.elements.maps.ArticulationMap;
import meico.mpm.elements.maps.GenericMap;
import meico.msm.Msm;
import meico.supplementary.KeyValue;
import nu.xom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The main class with the main method that is the entry point for program execution.
 * @author Axel Berndt
 */
public class Main {
    public static final String VERSION = "0.3.0";
    protected static boolean IGNORE_MEI_TEMPO = false;
    protected static boolean IGNORE_MEI_PHRASING = false;
    private static final boolean PROCESS_ALL = true;

    /**
     * This method is the entry point for the program execution.
     * @param args
     */
    public static void main(String[] args) {
        // this is for testing, not for deployment, hence set the global constant TESTING false for deployment
//        if (Main.PROCESS_ALL) {
//            File directoryPath = new File("C:\\Arbeit\\Software\\Java\\NeuesThueringerChoralbuchDigital");
//            File[] files = directoryPath.listFiles();
//            if (files != null) {
//                for (File file : files) {
//                    if (file.getName().endsWith(".mei"))
//                        Main.performFile(file.getAbsolutePath());
//                }
//            }
//            System.exit(0);
//        }

        // parse commandline arguments in the following loop, but no the last one
        boolean printHelpText = false;
        for (int i = 0; i < args.length-1; ++i) {
            switch (args[i]) {
                case "-?":
                case "--help":
                    printHelpText = true;
                    break;

                case "-s":
                case "--swing":
                    performer.Performer.SWING = true;
                    break;

                case "-t":
                case "--tempo":
                    try {
                        Performer.TEMPO = Double.parseDouble(args[++i]);
                        Main.IGNORE_MEI_TEMPO = true;
                    } catch (NumberFormatException e) {
                        System.err.println("Error: Invalid tempo value: " + args[i] + ".");     // an invalid argument
                    } catch (IndexOutOfBoundsException e) {
                        System.err.println("Error: No tempo value specified.");
                    }
                    break;

                case "-p":
                case "--phrasing":
                    try {
                        Performer.TEMPO_MODULATION_INTENSITY = Double.parseDouble(args[++i]);
                    } catch (NumberFormatException e) {
                        System.err.println("Error: Invalid phrasing value: " + args[i] + ".");  // an invalid argument
                    } catch (IndexOutOfBoundsException e) {
                        System.err.println("Error: No phrasing value specified.");
                    }
                    break;

                case "-a":
                case "--accentuation":
                    try {
                        Performer.METRICAL_ACCENTUATION_SCALE = Double.parseDouble(args[++i]);
                    } catch (NumberFormatException e) {
                        System.err.println("Error: Invalid metrical accentuation value: " + args[i] + ".");
                    } catch (IndexOutOfBoundsException e) {
                        System.err.println("Error: No metrical accentuation value specified.");
                    }
                    break;

                default:
                    System.err.println("Error: Invalid argument: " + args[i] + ".");            // an invalid argument
                    System.exit(64);
            }
        }

        // when help text should be printed
        if (printHelpText || args[args.length-1].equals("-?") || args[args.length-1].equals("--help")) {
            Main.printHelpText();
            System.exit(0);
        }

        // do the magic
        System.exit(Main.performFile(args[args.length-1]));                                     // the last argument is the file to be loaded
    }

    /**
     * This method performs all the functionality to create and store a performance of the MEI input file
     * @param path
     * @return exit codes to be used in System.exit()
     */
    private static int performFile(String path) {
        Mei mei;
        try {
            mei = Main.readMei(path);
        } catch (NullPointerException | IOException | ParserConfigurationException | ParsingException | SAXException e) {
            System.err.println("MEI file could not be loaded.");                                // print error to console
            e.printStackTrace();
            return 66;
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("Missing argument: the MEI file to be processed.");              // print error to console
            return 64;
        }

        String origFilename = Helper.getFilenameWithoutExtension(mei.getFile().getAbsolutePath());

        // some exports before doing the magic, i.e., before the preprocessing
        Msm origMsm = mei.exportMsmMpm().getKey().get(0);
        origMsm.writeMsm(origFilename + "_00.msm");
        origMsm.exportMidi().writeMidi(origFilename + "_00.mid");

        // now the preprocessing
        String tempo = Helper.getAttributeValue("mm", Helper.getFirstChildElement("tempo", Helper.getFirstChildElement("work", Helper.getFirstChildElement("workList", mei.getMeiHead()))));
        if (!tempo.isEmpty()) {
            Main.IGNORE_MEI_TEMPO = true;
            Performer.setBasicTempo(Double.parseDouble(tempo));
        } else {
            Main.IGNORE_MEI_TEMPO = false;
        }

        mei.layersToStaffs();                                                                   // convert all layers into individual staffs (instead of having multiple layers in one staff)
        System.out.println(mei.addIds() + " IDs added.");                                       // TODO: necessary?
//        Main.allMetconFalse(mei);                                                               // set all metcons false, the input data doesn't encode this correctly
        renameAllCaesuraToBreath(mei);                                                          // rename all caesura elements into breath
        breathBeforeRepetition(mei);                                                            // add breath before repetition bar lines
        removeEndingsAndRepetitionmarks(mei);                                                   // repetition marks and endings should not be present in the through-composed version we will create subsequently, thus we remove them hereby
        addInvisMeterSigBeforeFirstMeasure(mei);
        mei.resolveExpansions();                                                                // unroll repetitions in MEI
        KeyValue<List<Msm>, List<Mpm>> msmpm = mei.exportMsmMpm(Performer.PPQ);                 // get MSM and MPM data from the MEI
//        HashMap<String, String> repetitionIdMappings = resolveSequencingMaps(msmpm);  // no longer necessary thanks to resolving expansions in MEI            // resolve repetitions into through-composed form; not necessary if unrollRepetitions() is functional

        // the preprocessing stage exports
        mei.writeMei(origFilename + "_01-preproc.mei");
        msmpm.getKey().get(0).writeMsm(origFilename + "_01-preproc.msm");
        msmpm.getValue().get(0).writeMpm(origFilename + "_01-preproc.mpm");
//        msmpm.getKey().get(0).exportPitches(new Key(Key.midiReferenceFrequenciesEqualTemperament440, false)).writePitches(origFilename + "_01-preproc.json");
//        exportRepetitionIDsAsCsv(repetitionIdMappings, origFilename + "_01-preproc-repetitionIdMappings.csv");    // no longer necessary

        // now the performance
        Performer performer = new Performer(msmpm.getKey().get(0), msmpm.getValue().get(0), mei);   // create a Performer instance
        Msm expressiveMsm = performer.generateExpressiveMsm();                                      // generate the performance, apply it to the MSM and get an augmented expressive MSM
        Performer.fitVelocities(expressiveMsm, 0.0, 127.0);                               // fit velocities with the MIDI limits
        Main.addMsmPerformanceToMei(mei, expressiveMsm);                                            // write the data back to the input MEI

        // export expressive data
        performer.getMpm().writeMpm(origFilename + "_02-expr.mpm");
        expressiveMsm.writeMsm(origFilename + "_02-expr.msm");
        mei.writeMei(origFilename + "_02-expr.mei");                                        // write the augmented MEI to the file system
        expressiveMsm.exportExpressiveMidi().writeMidi(origFilename + "_02-expr.mid");
//        expressiveMsm.exportPitches(new Key(Key.midiReferenceFrequenciesEqualTemperament440, false)).writePitches(origFilename + "_02-expr.json");

        return 0;
    }

    /**
     * The routine for reading the input MEI file.
     * @param path
     * @return
     * @throws NullPointerException
     * @throws IOException
     * @throws ParserConfigurationException
     * @throws ParsingException
     * @throws SAXException
     * @throws ArrayIndexOutOfBoundsException
     */
    private static Mei readMei(String path) throws NullPointerException, IOException, ParserConfigurationException, ParsingException, SAXException, ArrayIndexOutOfBoundsException {
        File meiFile = new File(path);                      // get mei file
        meiFile = new File(meiFile.getCanonicalPath());     // ensure that the absolute path is stored in the file object
        return new Mei(meiFile);                            // instantiate an Mei object and return it
    }

    /**
     * This method adds attribute metcon="false" to all measures.
     * Thereby, we avoid problems with underful measures (e.g. upbeats), when these measures are not correctly annotated with this attribute.
     * @param mei
     */
    private static void allMetconFalse(Mei mei) {
        Nodes measures = mei.getMusic().query("descendant::*[local-name()='measure']");
        for (Node measure : measures) {
            ((Element) measure).addAttribute(new Attribute("metcon", "false"));
        }
    }

    /**
     * Rename all caesura elements into breath elements so they get the same processing into an articulation as a breath.
     * @param mei
     */
    private static void renameAllCaesuraToBreath(Mei mei) {
        for (Node c : mei.getMusic().getFirstChildElement("body", mei.getRootElement().getNamespaceURI()).query("descendant::*[local-name()='caesura']")) {
            Element caesura = (Element) c;
            caesura.setLocalName("breath");
            caesura.addAttribute(new Attribute("ho", "5"));
        }
    }

    /**
     * Add breath before repetition end marks, so they are included in the phrasing later on.
     * @param mei
     */
    private static void breathBeforeRepetition(Mei mei) {
        for (Node m : mei.getMusic().getFirstChildElement("body", mei.getRootElement().getNamespaceURI()).query("descendant::*[local-name()='measure']")) {
            Element measure = (Element) m;
            Attribute right = measure.getAttribute("right");

            if ((right == null) || !(right.getValue().equals("rptend") || right.getValue().equals("rptboth") || right.getValue().equals("dbl")))  // phrases end at repetition barlines and double barlines
                continue;

            // associate the breath with the last note of the measure in the first staff and layer, i.e., the soprano
            Elements notes = measure.getFirstChildElement("staff", mei.getRootElement().getNamespaceURI()).getFirstChildElement("layer", mei.getRootElement().getNamespaceURI()).getChildElements("note", mei.getRootElement().getNamespaceURI());  // get all notes in the soprano
            if (notes.size() == 0)
                continue;

            Element breath = new Element("breath", mei.getRootElement().getNamespaceURI());
            Helper.addUUID(breath);
            String id = notes.get(notes.size() - 1).getAttributeValue("id", "http://www.w3.org/XML/1998/namespace");
            breath.addAttribute(new Attribute("startid", "#" + id));
            breath.addAttribute(new Attribute("ho", "5"));

            measure.appendChild(breath);
        }
    }

//    /**
//     * This method unrolls all repetitions encoded in bar lines.
//     */
//    public static void unrollRepetitions(Mei mei) {
//        // elements score and part (child of parts) can contain sections, so we find these first; also, repetitions will not go beyond a score/part but stay within
//        ArrayList<Element> scores = new ArrayList<>();
//
//        for (Element mdiv : mei.getAllMdivs()) {                           // for each mdiv
//            for (Element partsOrScore : mdiv.getChildElements()) {
//                if (partsOrScore.getLocalName().equals("score")) {
//                    scores.add(partsOrScore);
//                    continue;
//                }
//                for (Element part : partsOrScore.getChildElements()) {
//                    scores.add(part);
//                }
//            }
//        }
//
//        for (Element score : scores) {
//            Nodes relevantNodes = score.query("descendant::*[local-name()='measure' or local-name()='scoreDef' or local-name()='ending']"); // Is this all we need?
//            // TODO: ... fülle eine Output-Sequenz und eine "readyToCopy"-Sequenz für Wiederholungen. Letztere wird mit jedem neuen rptstart gelehrt und neu gefüllt.
//        }
//    }

    /**
     * expand repetitions into a through-composed form
     * @param msmpms
     * @return HashMap with the (key, value) pairs (originalId, resolvedId) from the MSM only, not the MPM
     */
    private static HashMap<String, String> resolveSequencingMaps(KeyValue<List<Msm>, List<Mpm>> msmpms) {
        HashMap<String, String> repetitionIDs = null;
        ArrayList<GenericMap> articulationMaps = new ArrayList<>();
        for (int i=0; i < msmpms.getKey().size(); ++i) {                                                    // for each msm and corresponding mpm
            Element globalSequencingMap = msmpms.getKey().get(i).getRootElement().getFirstChildElement("global").getFirstChildElement("dated").getFirstChildElement("sequencingMap");   // get the global sequencingMap of this msm
            for (Performance performance : msmpms.getValue().get(i).getAllPerformances()) {                 // access all performances from the corresponding mpm
                // global maps are expanded only by the global sequencingMap
                if (globalSequencingMap != null) {
                    HashMap<String, GenericMap> maps = performance.getGlobal().getDated().getAllMaps();
                    for (GenericMap map : maps.values()) {
                        map.applySequencingMap(globalSequencingMap);
                        if (map instanceof ArticulationMap)     // in articulationMaps the elements have noteid attribute that has to be updated after resolving the sequencingMaps in MSM
                            articulationMaps.add(map);          // so keep the articulationMaps for later reference
                    }
                }

                // local maps are expanded by either the local sequencingMap, if there is one, or the global sequencingMap
                Elements msmParts = msmpms.getKey().get(i).getParts();
                ArrayList<Part> mpmParts = performance.getAllParts();
                for (int pa = 0; pa < performance.size(); ++pa) {
                    Element msmPart = msmParts.get(pa);
                    Element sequencingMap = msmPart.getFirstChildElement("dated").getFirstChildElement("sequencingMap");
                    if (sequencingMap == null) {
                        sequencingMap = globalSequencingMap;
                        if (sequencingMap == null)
                            continue;
                    }
                    for (GenericMap map : mpmParts.get(pa).getDated().getAllMaps().values()) {
                        map.applySequencingMap(sequencingMap);
                        if (map instanceof ArticulationMap)     // in articulationMaps the elements have notid attribute that has to be updated after resolving the sequencingmaps in MSM
                            articulationMaps.add(map);          // so keep the articulationMaps for later reference
                    }
                }

                // finally, apply the sequencingMaps to MSM data, this will also delete the sequencingMaps, hence it has to be done at the end
                repetitionIDs = msmpms.getKey().get(i).resolveSequencingMaps();
                System.out.println("Added IDs: " + repetitionIDs.toString());

                // update the articulationMap's elements' notid attributes
                for (GenericMap map : articulationMaps) {
                    Helper.updateMpmNoteidsAfterResolvingRepetitions(map, repetitionIDs);
                }
            }
        }

        return repetitionIDs;
    }

    /**
     * writes the ID mappings after unrolling repetitions in MSM into a CSV file
     * @param repetitionIDs
     * @param csvPath
     */
    private static void exportRepetitionIDsAsCsv(HashMap<String, String> repetitionIDs, String csvPath) {
        try (PrintWriter writer = new PrintWriter(csvPath, "UTF-8")) {
            writer.println("resolvedId, originalId");
            for (String key : repetitionIDs.keySet()) {
                String value = repetitionIDs.get(key);
                writer.println(escapeCsv(value) + ", " + escapeCsv(key));
            }
        } catch (IOException e) {
            System.err.println("CSV-Export der repetitionIDs fehlgeschlagen: " + csvPath);
            e.printStackTrace();
        }
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
     * helper method for exportRepetitionIDsAsCsv() to ensure validity
     * @param value
     * @return
     */
    private static String escapeCsv(String value) {
        if (value == null)
            return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * From the expressive MSM add the milliseconds dates, milliseconds end dates and velocities
     * of all notes and rests to their correspondents in the MEI. Supported MEI elements are note, rest, mRest, space, and mSpace.
     * @param mei
     * @param msm
     */
    private static void addMsmPerformanceToMei(Mei mei, Msm msm) {
        // collect all MEI notes, rests and mRests with their xml:id in a lookup table/hashmap
        HashMap<String, Element> allNotesAndRests = new HashMap<>();
        Nodes nodes = mei.getMusic().query("descendant::*[local-name()='note' or local-name()='rest' or local-name()='mRest' or local-name()='space' or local-name()='mSpace']");
        for (int i=0; i < nodes.size(); ++i) {
            Element node = (Element) nodes.get(i);
            allNotesAndRests.put(Helper.getAttributeValue("id", node), node);
        }

        // traverse all MSM notes and rests and add their attributes velocity, milliseconds.date and milliseconds.date.end to the corresponding MEI element
        for (Element part : msm.getParts()) {
            Element score = Helper.getFirstChildElement("score", Helper.getFirstChildElement("dated", part));
            for (Element e : score.getChildElements()) {
                String id = e.getAttributeValue("id", "http://www.w3.org/XML/1998/namespace");  // get the xml:id of the MSM element, it will be the same as the xml:id of the corresponding MEI element
//                String id = Helper.getAttributeValue("id", e);                                  // this is more robust than the above but also less efficient as we can expect all IDs to be in the xml namespace

                Element m = allNotesAndRests.get(id);                                           // get the corresponding MEI element
                if (m == null)
                    continue;

                if (!e.getLocalName().equals("rest")) {                                         // rests have no velocity, so make sure we read this attribute only from notes in the MSM
                    double velocity = Double.parseDouble(Helper.getAttributeValue("velocity", e));
                    m.addAttribute(new Attribute("vel", Integer.toString((int) velocity)));
                }

                double date = Double.parseDouble(Helper.getAttributeValue("milliseconds.date", e));
                m.addAttribute(new Attribute("tstamp.real", Double.toString(date * 0.001)));

                double end = Double.parseDouble(Helper.getAttributeValue("milliseconds.date.end", e));
                m.addAttribute(new Attribute("tstamp2.real", Double.toString(end * 0.001)));
            }
        }
    }

    /**
     * print the help text to the console
     */
    private static void printHelpText() {
        System.out.println("ChoralePerformer v " + Main.VERSION + "\nMeico: MEI Converter v" + Meico.version);
        System.out.println("[-?] or [--help]                        show this help text");
//        System.out.println("[-d] or [--debug]                       write additional debug output to the commandline");
        System.out.println("[-s] or [--swing]                       perform the music with swing timing");
        System.out.println("[-t argument] or [--tempo argument]     set the basic tempo of the music in bpm (default is " + Performer.TEMPO + ")");
        System.out.println("[-p argument] or [--phrasing argument]  set the intensity of phrasing with a value >= 0.0 (default is " + Performer.TEMPO_MODULATION_INTENSITY + ", off is 0.0)");
        System.out.println("[-a argument] or [--accentuation argument] set metrical accentuation strength with a value >= 0.0 (default is " + Performer.METRICAL_ACCENTUATION_SCALE + ")");
        System.out.println("\nThe final argument should always be a path to a valid mei file (e.g., \"C:\\myMeiCollection\\test.mei\"); always in quotes! This is the only mandatory argument if you want to convert something.");
    }
}
