package supplementary;

/**
 * An enumeration of accidentals.
 * @author Axel Berndt
 */
public enum Accidental {
    s,      // Sharp.
    f,      // Flat.
    ss,     // Double sharp (written as 2 sharps).
    x,      // Double sharp (written using croix).
    ff,     // Double flat.
    xs,     // Triple sharp (written as a croix followed by a sharp).
    sx,     // Triple sharp (written as a sharp followed by a croix).
    ts,     // Triple sharp (written as 3 sharps).
    tf,     // Triple flat.
    n,      // Natural.
    nf,     // Natural + flat; used to cancel preceding double flat.
    ns,     // Natural + sharp; used to cancel preceding double sharp.
    su,     // Sharp note raised by quarter tone (sharp modified by arrow).
    sd,     // Sharp note lowered by quarter tone (sharp modified by arrow).
    fu,     // Flat note raised by quarter tone (flat modified by arrow).
    fd,     // Flat note lowered by quarter tone (flat modified by arrow).
    nu,     // Natural note raised by quarter tone (natural modified by arrow).
    nd,     // Natural note lowered by quarter tone (natural modified by arrow).
    xu,     // Double sharp note raised by quarter tone (double sharp modified by arrow).
    xd,     // Double sharp note lowered by quarter tone (double sharp modified by arrow).
    ffu,    // Double flat note raised by quarter tone (double flat modified by arrow).
    ffd,    // Double flat note lowered by quarter tone (double flat modified by arrow).
    q1f,    // 1/4-tone flat accidental.
    q3f,    // 3/4-tone flat accidental.
    q1s,    // 1/4-tone sharp accidental.
    q3s     // 3/4-tone sharp accidental.
}
