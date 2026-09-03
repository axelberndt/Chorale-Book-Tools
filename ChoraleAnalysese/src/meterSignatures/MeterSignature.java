package meterSignatures;

import nu.xom.Attribute;
import nu.xom.Element;

import java.util.Objects;

/**
 * This class represents a time signature.
 * @author Axel Berndt
 */
public class MeterSignature {
    public final double numerator;      // if not defined, numerator = 0
    public final int denominator;       // if not defined, denominator = 0
    private final int hashCode;

    /**
     * constructor
     * @param numerator the numerator of the time signature
     * @param denominator the denominator of the time signature
     */
    public MeterSignature(double numerator, int denominator) {
        this.numerator = numerator;
        this.denominator = denominator;

//        this.hashCode = (int)(this.numerator * 10000.0) + this.denominator;
        this.hashCode = Objects.hash(this.numerator, this.denominator); // content-based hash
//        this.hashCode = super.hashCode();
    }

    /**
     * Create a MeterSignature object from an MEI scoreDef, staffDef, layerDef, or meterSig element.
     * @param element the MEI element to parse
     * @return the MeterSignature object or null
     */
    public static MeterSignature fromMei(Element element) {
        if (element == null)
            return null;

        Attribute count, unit, sym;

        switch (element.getLocalName()) {
            case "scoreDef":
            case "staffDef":
            case "layerDef":
                count = element.getAttribute("meter.count");
                unit = element.getAttribute("meter.unit");
                sym = element.getAttribute("meter.sym");
                break;
            case "meterSig":
                count = element.getAttribute("count");
                unit = element.getAttribute("unit");
                sym = element.getAttribute("sym");
                break;
            default:
                return null;
        }

        if ((count != null) && (unit != null)) {
            return new MeterSignature(Double.parseDouble(count.getValue()), Integer.parseInt(unit.getValue()));
        }

        if (sym != null) {
            switch (sym.getValue()) {
                case "common":
                    return new MeterSignature(4.0, 4);
                case "cut":
                    return new MeterSignature(2.0, 2);
                case "open":    // senza misura/no time signature
                default:
            }
        }

        return null;
    }

    /**
     * Create a MeterSignature object from an MSM timeSignature element.
     * @param element the element to parse
     * @return the MeterSignature object or null
     */
    public static MeterSignature fromMsm(Element element) {
        if (element == null)
            return null;
        return new MeterSignature(Double.parseDouble(element.getAttributeValue("numerator")), Integer.parseInt(element.getAttributeValue("denominator")));
    }

    /**
     * Convert the MeterSignature object to an MEI meterSig element.
     * @return the MEI element
     */
    public Element toMei() {
        Element out = new Element("meterSig");
        out.addAttribute(new Attribute("count", String.valueOf(this.numerator)));
        out.addAttribute(new Attribute("unit", String.valueOf(this.denominator)));
        return out;
    }

    /**
     * Convert the MeterSignature object to an MSM timeSignature element.
     * @return the MSM element
     */
    public Element toMsm() {
        Element out = new Element("timeSignature");
        out.addAttribute(new Attribute("date", ""));
        out.addAttribute(new Attribute("numerator", String.valueOf(this.numerator)));
        out.addAttribute(new Attribute("denominator", String.valueOf(this.denominator)));
        return out;
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

        return Objects.equals(this.numerator, ((MeterSignature) obj).numerator) && Objects.equals(this.denominator, ((MeterSignature) obj).denominator);
    }

    /**
     * Hash code generation
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
        return this.numerator + "/" + this.denominator;
    }
}
