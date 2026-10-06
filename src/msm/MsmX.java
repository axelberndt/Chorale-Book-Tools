package msm;

import meico.msm.Msm;
import msm.elements.MsmRoot;
import nu.xom.Attribute;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

/**
 * This is an extension of meico's MSM class. The MsmElement object is an object that builds upon
 * functionality of the meico's MPM. However, this is not at a state where it can become part of meico!
 * @author Axel Berndt
 */
public class MsmX extends Msm {
    public static final String TIME_SIGNATURE_MAP = "timeSignatureMap";
    public static final String KEY_SIGNATURE_MAP = "keySignatureMap";
    public static final String SECTION_MAP = "sectionMap";
    public static final String PHRASE_MAP = "phraseMap";
    public static final String SEQUENCING_MAP = "sequencingMap";
    public static final String PROGRAM_CHANGE_MAP = "programChangeMap";
    public static final String CHORD_MAP = "chordMap";
    public static final String SCORE = "score";

    private final MsmRoot msmRoot;

    /**
     * constructor
     */
    public MsmX() {
        super();
        this.msmRoot = MsmRoot.createMsmElement(this.getRootElement());
    }

    /**
     * constructor
     *
     * @param msm the msm document of which to instantiate the Msm object
     */
    public MsmX(Document msm) {
        super(msm);
        this.msmRoot = MsmRoot.createMsmElement(this.getRootElement());
    }

    /**
     * constructor
     *
     * @param file the msm file to be read
     * @throws IOException
     * @throws ParsingException
     */
    public MsmX(File file) throws IOException, ParsingException, SAXException, ParserConfigurationException {
        super(file);
        this.msmRoot = MsmRoot.createMsmElement(this.getRootElement());
    }

    /**
     * constructor
     * @param file
     * @param validate
     * @param schema can be null
     * @throws IOException
     * @throws ParsingException
     */
    public MsmX(File file, boolean validate, URL schema) throws IOException, ParsingException, SAXException, ParserConfigurationException {
        super(file, validate, schema);
        this.msmRoot = MsmRoot.createMsmElement(this.getRootElement());
    }

    /**
     * constructor
     * @param xml xml code as UTF8 String
     * @throws IOException
     * @throws ParsingException
     */
    public MsmX(String xml) throws IOException, ParsingException, ParserConfigurationException, SAXException {
        super(xml);
        this.msmRoot = MsmRoot.createMsmElement(this.getRootElement());
    }

    /**
     * constructor
     * @param xml xml code as UTF8 String
     * @param validate validate the code?
     * @param schema can be null
     * @throws IOException
     * @throws ParsingException
     */
    public MsmX(String xml, boolean validate, URL schema) throws IOException, ParsingException, ParserConfigurationException, SAXException {
        super(xml, validate, schema);
        this.msmRoot = MsmRoot.createMsmElement(this.getRootElement());
    }

    /**
     * constructor
     * @param inputStream read from this input stream
     * @throws IOException
     * @throws ParsingException
     */
    public MsmX(InputStream inputStream) throws IOException, ParsingException {
        super(inputStream);
        this.msmRoot = MsmRoot.createMsmElement(this.getRootElement());
    }

    /**
     * constructor
     * @param inputStream read from this input stream
     * @param validate
     * @param schema can be null
     * @throws IOException
     * @throws ParsingException
     */
    public MsmX(InputStream inputStream, boolean validate, URL schema) throws IOException, ParsingException {
        super(inputStream, validate, schema);
        this.msmRoot = MsmRoot.createMsmElement(this.getRootElement());
    }

    /**
     * this factory creates an initial Msm instance with empty global maps
     * @param title
     * @param id an id string for the root element or null, in the latter case a random UUID will be created
     * @param ppq
     * @return
     */
    public static meico.msm.Msm createMsm(String title, String id, int ppq) {
        Element root = new Element("msm");                                          // create the root element of the msm/xml tree
        root.addAttribute(new Attribute("title", title));                           // add a title attribute to it

        Attribute idAttribute = new Attribute("id", (id == null) ? UUID.randomUUID().toString() : id);  // make new id attribute
        idAttribute.setNamespace("xml", "http://www.w3.org/XML/1998/namespace");    // set correct namespace
        root.addAttribute(idAttribute);                                             // and it to the MSM movement element

        // create global containers
        Element global = new Element("global");
        Element dated = new Element("dated");
        Element header = new Element("header");

        root.addAttribute(new Attribute("pulsesPerQuarter", Integer.toString(ppq)));// add the attribute to the root

        dated.appendChild(new Element("timeSignatureMap"));                         // global time signatures
        dated.appendChild(new Element("keySignatureMap"));                          // global key signatures
        dated.appendChild(new Element("markerMap"));                                // global rehearsal marks
        dated.appendChild(new Element("sectionMap"));                               // global map of section structure
        dated.appendChild(new Element("phraseMap"));                                // global map of phrase structure
        dated.appendChild(new Element("sequencingMap"));                            // global sequencingMap
        dated.appendChild(new Element("pedalMap"));                                 // global map for pedal instructions
        dated.appendChild(new Element("miscMap"));                                  // a temporal map that is filled with content that may be useful during processing but will be deleted in the final MSM

        global.appendChild(header);
        global.appendChild(dated);
        root.appendChild(global);

        return new meico.msm.Msm(new Document(root));
    }

    /**
     * a getter for the MSM's root element
     * @return
     */
    public MsmRoot getMsmRoot() {
        return this.msmRoot;
    }
}
