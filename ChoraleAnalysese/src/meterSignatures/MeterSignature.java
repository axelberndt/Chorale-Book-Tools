package meterSignatures;

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
     * Equality comparison
     * @param obj   the reference object with which to compare.
     * @return
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
