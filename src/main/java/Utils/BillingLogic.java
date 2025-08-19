package Utils;

import javax.swing.*;

public class BillingLogic {
    /*

     */
    public static double calculateTotalAmount(int offsetBy, int offsetunit, double tarif,
                                              double mins, double lessThan30Rate, String company,
                                              java.sql.Date workedDate) {
        switch (company) {
            case "Planet Traduction":
                return calculateTotal_PT(offsetBy, offsetunit, tarif, mins, lessThan30Rate, workedDate);
            default:
                return calculateTotal_General(offsetBy, offsetunit, tarif, mins);
        }

    }

    public static double calculateTotal_General(int offsetBy, int offsetunit, double tarif,
                                                double mins) {
        double isOffset = mins % offsetunit;
        double adjustedMin = mins;
        int count = 0;

        while (mins > offsetunit && isOffset > offsetBy) {
            adjustedMin = mins - isOffset + offsetunit;
            isOffset = adjustedMin % offsetunit;
            count++;
            if (count > 1) {
                JOptionPane.showMessageDialog(
                        null,
                        "Minute adjustment error has occurred",
                        "Adjust Minute",
                        JOptionPane.ERROR_MESSAGE);
            }
        }

        if (isOffset <= offsetBy) {
            adjustedMin = adjustedMin - isOffset;
        }

        double lessThan30Adjust = (adjustedMin % 60);
        double total = 0;
        if (lessThan30Adjust == 0) {
            total = tarif * (adjustedMin / 60);
        } else {
            // if say it is 1 hour 2 mins to 1 hour 29 minutes then apply "hour rate" to 1 hour
            // and "less than 30" rate to the remaining minute
            total = (tarif * ((adjustedMin - lessThan30Adjust) / 60)) + (tarif / 2);
        }

        // until minutes are less than (offsetunit + offsetBy) minutes, lessthan30 rate applies
        if (mins <= offsetunit + offsetBy) {
            total = (tarif / 2);
        }
        return total;
    }

    public static double calculateTotal_PT(int offsetBy, int offsetunit, double tarif,
                                                         double mins, double lessThan30Rate, java.sql.Date workedDate) {
        double isOffset = mins % offsetunit;
        double adjustedMin = mins;
        int count = 0;

        while (mins > offsetunit && isOffset > offsetBy) {
            adjustedMin = mins - isOffset + offsetunit;
            isOffset = adjustedMin % offsetunit;
            count++;
            if (count > 1) {
                JOptionPane.showMessageDialog(
                        null,
                        "Minute adjustment error has occurred",
                        "Adjust Minute",
                        JOptionPane.ERROR_MESSAGE);
            }
        }

        if (isOffset <= offsetBy) {
            adjustedMin = adjustedMin - isOffset;
        }

        String isoJuly2025 = "2025-07-01"; // July 2025
        java.sql.Date july2025 = java.sql.Date.valueOf(isoJuly2025);

        double lessThan30Adjust = (adjustedMin % 60);
        double total = 0;
        if (lessThan30Adjust == 0) {
            total = tarif * (adjustedMin / 60);
        } else {
            // if say it is 1 hour 2 mins to 1 hour 29 minutes then apply "hour rate" to 1 hour
            // and "less than 30" rate to the remaining minute
            total = (tarif * ((adjustedMin - lessThan30Adjust) / 60)) + lessThan30Rate;

            // new tarif decision for P T which applies to month of JULY 2025 and beyond
            if (workedDate.after(july2025) || workedDate.equals(july2025)) {
                total = (tarif * ((adjustedMin - lessThan30Adjust) / 60)) + (tarif / 2);
            }

//            Check the later threshold first so earlier ones aren’t reconsidered:
//
//            if (workedDate.after(july2026) || workedDate.equals(july2026)) {
//                // rate B
//            } else if (workedDate.after(july2025) || workedDate.equals(july2025)) {
//                // rate A
//            }

        }

        // until minutes are less than (offsetunit + offsetBy) minutes, lessthan30 rate applies
        if (mins <= offsetunit + offsetBy) {
            total = lessThan30Rate;

            // new tarif decision for P T which applies to month of JULY 2025 and beyond
            if (workedDate.after(july2025) || workedDate.equals(july2025)) {
                total = (tarif / 2);
            }
        }
        return total;
    }
}
