package msm.elements;

import meico.mei.Helper;
import meico.mpm.elements.Global;
import meico.mpm.elements.Part;
import meico.xml.AbstractXmlSubtree;
import nu.xom.Attribute;
import nu.xom.Element;

import java.util.ArrayList;
import java.util.LinkedList;

/**
 * This class represents an MSM root element.
 * @author Axel Berndt
 */
public class MsmRoot extends AbstractXmlSubtree {
    private int pulsesPerQuarter = 720;                         // the timing resolution of symbolic time (midi.date etc.)
    private Global global = null;                               // the global MSM information
    private final ArrayList<Part> parts = new ArrayList<>();    // the local MSM information
    private Attribute title = null;
    private Attribute id = null;                                // the id attribute

    /**
     * This constructor generates an empty MSM with only a name, global and dated environment.
     *
     * @param title the title of the MSM
     */
    private MsmRoot(String title) throws Exception {
        Element msm = new Element("msm");//, Mpm.MPM_NAMESPACE);
        Attribute titleAtt = new Attribute("title", title);
        msm.addAttribute(titleAtt);

        this.parseData(msm);
    }

    /**
     * this constructor instantiates the Msm object from an existing xml source handed over as XOM Element
     *
     * @param xml
     */
    private MsmRoot(Element xml) throws Exception {
        this.parseData(xml);
    }

    /**
     * MsmElement factory
     *
     * @param title the title of the MSM
     * @return
     */
    public static MsmRoot createMsmElement(String title) {
        MsmRoot msmElement;
        try {
            msmElement = new MsmRoot(title);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return msmElement;
    }

    /**
     * MsmElement factory
     *
     * @param title the name of the MSM
     * @param pulsesPerQuarter
     * @return
     */
    public static MsmRoot createMsmElement(String title, int pulsesPerQuarter) {
        MsmRoot msmElement;
        try {
            msmElement = new MsmRoot(title);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        msmElement.setPulsesPerQuarter(pulsesPerQuarter);
        return msmElement;
    }

    /**
     * MsmElement factory
     *
     * @param title the name of the MSM
     * @param pulsesPerQuarter
     * @param id
     * @return
     */
    public static MsmRoot createMsmElement(String title, int pulsesPerQuarter, String id) {
        MsmRoot msmElement;
        try {
            msmElement = new MsmRoot(title);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        msmElement.setPulsesPerQuarter(pulsesPerQuarter);
        msmElement.setId(id);
        return msmElement;
    }

    /**
     * MsmElement factory
     *
     * @param xml
     * @return
     */
    public static MsmRoot createMsmElement(Element xml) {
        MsmRoot msmElement;
        try {
            msmElement = new MsmRoot(xml);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return msmElement;
    }

    /**
     * set the data of this object, this parses the xml element and generates the according data structures
     * @param xml
     * @throws Exception
     */
    @Override
    protected void parseData(Element xml) throws Exception {
        if (xml == null)
            throw new Exception("Cannot generate Msm object. XML Element is null.");

        Attribute title = Helper.getAttribute("title", xml);
        if ((title == null) || title.getValue().isEmpty()) {                          // each MSM requires a title, if there is none or it is empty
            throw new Exception("Cannot generate Msm object. Attribute title is missing or empty.");  // throw exception
        }

        this.setXml(xml);
        this.title = Helper.getAttribute("title", this.getXml());
        this.id = Helper.getAttribute("id", this.getXml());

        // make sure that this element is really a "msm" element
        if (!this.getXml().getLocalName().equals("msm")) {
            this.getXml().setLocalName("msm");
        }

        // make sure the msm has a pulsesPerQuarter attribute
        Attribute ppqAtt = Helper.getAttribute("pulsesPerQuarter", this.getXml());
        if (ppqAtt == null) {                                                       // if there is no pulsesPerQuarter attribute
            ppqAtt = new Attribute("pulsesPerQuarter", "720");                      // generate one with default ppq of 720
            this.getXml().addAttribute(ppqAtt);                                     // add it to the xml
            this.pulsesPerQuarter = 720;                                            // set the corresponding class variable
        } else {
            this.pulsesPerQuarter = Integer.parseInt(ppqAtt.getValue());            // read the attribute value into the corresponding class variable
        }

        // make sure there is a global environment
        Element globalElt = Helper.getFirstChildElement("global", this.getXml());
        if (globalElt == null) {                                                    // if the MSM has no global environment
            this.global = Global.createGlobal();
            this.getXml().appendChild(this.global.getXml());                        // add it to the performance
        } else {
            this.global = Global.createGlobal(globalElt);
        }
        // add the parts to this.parts
        LinkedList<Element> parts = Helper.getAllChildElements("part", this.getXml());
        for (Element element : parts) {
            Part part = Part.createPart(element);                                   // try to generate an MpmPart object from the xml data
            if (part == null)
                continue;                                                           // continue with the next part
            part.setGlobal(this.global);                                            // set the global environment
            this.parts.add(part);                                                   // otherwise, add it to the parts list
        }
    }

    /**
     * this returns all parts in this MSM as an ArrayList
     * @return
     */
    public ArrayList<Part> getAllParts() {
        return this.parts;
    }

    /**
     * Access the part with the specified number.
     * If there are more than one part with this number, the first in the list is returned.
     * @param number
     * @return
     */
    public Part getPart(int number) {
        for (Part p : this.parts) {
            if (p.getNumber() == number)
                return p;
        }
        return null;
    }

    /**
     * Access the part with the specified name.
     * If there are more than one part with this name, the first in the list is returned.
     * @param name
     * @return
     */
    public Part getPart(String name) {
        for (Part p : this.parts) {
            if (p.getName().equals(name))
                return p;
        }
        return null;
    }

    /**
     * Access the part by its MIDI channel and port
     * If there are more than one part with this channel and port (which is bad practise!), the first in the list is returned.
     * @param midiChannel
     * @param midiPort
     * @return
     */
    public Part getPart(int midiChannel, int midiPort) {
        for (Part p : this.parts) {
            if ((p.getMidiChannel() == midiChannel) && (p.getMidiPort() == midiPort))
                return p;
        }
        return null;
    }

    /**
     * Access the part with the specified signature
     * @param number
     * @param name
     * @param midiChannel
     * @param midiPort
     * @return the part or null
     */
    public Part getPart(int number, String name, int midiChannel, int midiPort) {
        for (Part p : this.parts) {
            if ((p.getNumber() == number)
                    && p.getName().equals(name)
                    && (p.getMidiChannel() == midiChannel)
                    && (p.getMidiPort() == midiPort))
                return p;
        }
        return null;
    }

    /**
     * add the part to the MSM,
     * caution: if another part with the same number exists already in this MSM, getPart(number) will return only the first
     * @param part
     * @return success
     */
    public boolean addPart(Part part) {
        if ((part == null) || (this.parts.contains(part)))
            return false;

        Element parent = (Element) part.getXml().getParent();
        if ((parent == null) || (parent != this.getXml())) {
            part.getXml().detach();
            this.getXml().appendChild(part.getXml());   // add the xml code of the part to the performance's xml
        }
        part.setGlobal(this.getGlobal());           // link to the global environment where the part may find related information (styleDefs etc.)
        return this.parts.add(part);
    }

    /**
     * remove all parts with the specified number from this MSM
     * @param number
     */
    public void removePart(int number) {
        for (Part p : this.parts) {
            if (p.getNumber() == number) {
                this.parts.remove(p);
                this.getXml().removeChild(p.getXml());
//                p.getXml().detach();
            }
        }
    }

    /**
     * remove all parts with the specified name from this MSM
     * @param name
     */
    public void removePart(String name) {
        for (Part p : this.parts) {
            if (p.getName().equals(name)) {
                this.parts.remove(p);
                this.getXml().removeChild(p.getXml());
//                p.getXml().detach();
            }
        }
    }

    /**
     * remove the specified part from this MSM
     * @param part
     */
    public void removePart(Part part) {
        if (this.parts.remove(part)) {                  // if the part was in this performance and could be removed from the parts list
            this.getXml().removeChild(part.getXml());   // it can be removed from the xml structure
//            part.getXml().detach();
        }
    }

    /**
     * access the global information of this MSM
     * @return
     */
    public Global getGlobal() {
        return this.global;
    }

    /**
     * a getter for the MSM's title
     * @return
     */
    public String getTitle() {
        return this.title.getValue();
    }

    /**
     * set the MSM's title,
     * @param name
     */
    public void setTitle(String name) {
        this.title.setValue(name);
    }

    /**
     * read the pulses per quarter timing resolution (relevant for to interpret midi.date values)
     * @return
     */
    public int getPulsesPerQuarter() {
        return this.pulsesPerQuarter;
    }

    /**
     * read the pulses per quarter timing resolution (relevant for to interpret midi.date values)
     * @return
     */
    public int getPPQ() {
        return this.getPulsesPerQuarter();
    }

    /**
     * Set the pulses per quarter timing resolution attribute.
     * Be careful with this, it does not change any midi date values!
     * @param ppq
     */
    public void setPulsesPerQuarter(int ppq) {
        this.pulsesPerQuarter = ppq;
        Helper.getAttribute("pulsesPerQuarter", this.getXml()).setValue(Integer.toString(ppq));
    }

    /**
     * Set the pulses per quarter timing resolution attribute.
     * Be careful with this, it does not change any midi date values!
     * @param ppq
     */
    public void setPPQ(int ppq) {
        this.setPulsesPerQuarter(ppq);
    }

    /**
     * set the MSM's id
     * @param id a xml:id string or null
     */
    public void setId(String id) {
        if (id == null) {
            if (this.id != null) {
                this.id.detach();
                this.id = null;
            }
            return;
        }

        if (this.id == null) {
            this.id = new Attribute("id", id);
            this.id.setNamespace("xml", "http://www.w3.org/XML/1998/namespace");    // set correct namespace
            this.getXml().addAttribute(this.id);
            return;
        }

        this.id.setValue(id);
    }

    /**
     * get the MSM's id
     * @return a string or null
     */
    public String getId() {
        if (this.id == null)
            return null;

        return this.id.getValue();
    }
}
