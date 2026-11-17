package tn.isetrades.isetcare.data;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class Formats {

    private static final Locale FR = Locale.FRENCH;

    private Formats() {
    }

    /** 12 octobre */
    public static String jour(long ms) {
        return new SimpleDateFormat("d MMMM", FR).format(new Date(ms));
    }

    /** 20 OCT. · 09:00 */
    public static String jourHeure(long ms) {
        return new SimpleDateFormat("dd MMM · HH:mm", FR).format(new Date(ms)).toUpperCase(FR);
    }

    /** 20/10/2026 */
    public static String date(long ms) {
        return new SimpleDateFormat("dd/MM/yyyy", FR).format(new Date(ms));
    }

    /** 09:00 - 10/10/2026 */
    public static String heureDate(long ms) {
        return new SimpleDateFormat("HH:mm · dd/MM/yyyy", FR).format(new Date(ms));
    }

    /** 180 DT */
    public static String dt(double montant) {
        return String.format(FR, "%.0f DT", montant);
    }

    public static String pourcent(double v) {
        return Math.round(v * 100) + " %";
    }
}
