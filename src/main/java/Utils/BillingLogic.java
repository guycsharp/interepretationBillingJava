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

//    public static double calculateTotal_General(int offsetBy, int offsetunit, double tarif,
//                                                double mins) {
//        double isOffset = mins % offsetunit;
//        double adjustedMin = mins;
//        int count = 0;
//
//        while (mins > offsetunit && isOffset > offsetBy) {
//            adjustedMin = mins - isOffset + offsetunit;
//            isOffset = adjustedMin % offsetunit;
//            count++;
//            if (count > 1) {
//                JOptionPane.showMessageDialog(
//                        null,
//                        "Minute adjustment error has occurred",
//                        "Adjust Minute",
//                        JOptionPane.ERROR_MESSAGE);
//            }
//        }
//
//        if (isOffset <= offsetBy) {
//            adjustedMin = adjustedMin - isOffset;
//        }
//
//        double lessThan30Adjust = (adjustedMin % 60);
//        double total = 0;
//        if (lessThan30Adjust == 0) {
//            total = tarif * (adjustedMin / 60);
////        } else if (adjustedMin < 60){
//
//        } else {
//            // if say it is 1 hour 2 mins to 1 hour 29 minutes then apply "hour rate" to 1 hour
//            // and "less than 30" rate to the remaining minute
//            total = (tarif * ((adjustedMin - lessThan30Adjust) / 60)) + (tarif / 2);
//        }
//
//        // until minutes are less than (offsetunit + offsetBy) minutes, lessthan30 rate applies
//        if (mins <= offsetunit + offsetBy) {
//            total = (tarif / 2);
//        }
//        return total;
//    }

public static double calculateTotal_General(int offsetBy, int offsetunit, double tarif, double mins) {

    double adjustedMin = mins;

    // Minimum 30 minutes
    if (adjustedMin < 30) {
        adjustedMin = 30;
    }

    // Extract leftover minutes
    double leftover = adjustedMin % 60;
    double fullHours = (adjustedMin - leftover) / 60;

    // Offset rounding
    if (leftover > 0) {
        double minutesToNextHour = 60 - leftover;
        if (minutesToNextHour <= (offsetunit + offsetBy)) {
            leftover = 0;
            fullHours += 1;
        }
    }

    // Billing rules
    double total;

    if (adjustedMin == 30) {
        total = tarif / 2; // half hour
    }
    else if (leftover == 0) {
        total = tarif * fullHours;
    }
    else if (leftover < 30) {
        total = (tarif * fullHours) + (tarif / 2);
    }
    else {
        total = (tarif * fullHours) + tarif;
    }

    return total;
}


    public static double calculateTotal_CF(int offsetBy, int offsetunit, double tarif, double mins) {

        double adjustedMin = mins;

        // 1️⃣ Minimum 30 minutes rule
        // BUT 30 minutes should be billed as HALF HOUR, not full hour
        if (adjustedMin < 30) {
            adjustedMin = 30;
        }

        // 2️⃣ Extract leftover minutes after full hours
        double leftover = adjustedMin % 60;
        double fullHours = (adjustedMin - leftover) / 60;

        // 3️⃣ Apply offset rounding rule
        if (leftover > 0) {
            double minutesToNextHour = 60 - leftover;

            // If leftover is within offsetunit + offsetBy → round up
            if (minutesToNextHour <= (offsetunit + offsetBy)) {
                leftover = 0;
                fullHours += 1;
            }
            // else → keep leftover as is
        }

        // 4️⃣ Calculate total
        double total;

        // NEW FIX: 30 minutes = HALF HOUR (not full hour)
        if (adjustedMin == 30) {
            total = tarif / 2;   // 15 → 30 → half hour
        }
        else if (leftover == 0) {
            // exact hour
            total = tarif * fullHours;
        }
        else if (leftover < 30) {
            // less than 30 minutes → half hour
            total = (tarif * fullHours) + (tarif / 2);
        }
        else {
            // 30 minutes or more → full hour
            total = (tarif * fullHours) + tarif;
        }

        return total;
    }


    /*
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

        String isoAug2025 = "2025-08-01"; // August 2025
        java.sql.Date aug2025 = java.sql.Date.valueOf(isoAug2025);

        double lessThan30Adjust = (adjustedMin % 60);
        double total = 0;
        if (lessThan30Adjust == 0) {
            total = tarif * (adjustedMin / 60);
        } else {
            // if say it is 1 hour 2 mins to 1 hour 29 minutes then apply "hour rate" to 1 hour
            // and "less than 30" rate to the remaining minute
            total = (tarif * ((adjustedMin - lessThan30Adjust) / 60)) + lessThan30Rate;

            // new tarif decision for P T which applies to month of August 2025 and beyond
//            if (workedDate.after(aug2025) || workedDate.equals(aug2025)) {
//                total = (tarif * ((adjustedMin - lessThan30Adjust) / 60)) + (tarif / 2);
//            }

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
//            if (workedDate.after(aug2025) || workedDate.equals(aug2025)) {
//                total = (tarif / 2);
//            }
        }
        return total;
    }
*/
    public static double calculateTotal_PT(int offsetBy, int offsetunit, double tarif,
                                           double mins, double lessThan30Rate, java.sql.Date workedDate) {

        // PT RULE: if original minutes < 30 → bill lessThan30Rate
        if (mins < 30) {
            return lessThan30Rate;
        }

        double adjustedMin = mins;

        // Minimum 30 minutes
        if (adjustedMin < 30) {
            adjustedMin = 30;
        }

        // Extract leftover minutes
        double leftover = adjustedMin % 60;
        double fullHours = (adjustedMin - leftover) / 60;

        // Offset rounding
        if (leftover > 0) {
            double minutesToNextHour = 60 - leftover;
            if (minutesToNextHour <= (offsetunit + offsetBy)) {
                leftover = 0;
                fullHours += 1;
            }
        }

        // Billing rules
        double total;

        if (leftover == 0) {
            total = tarif * fullHours;
        } else if (leftover < 30) {
            total = (tarif * fullHours) + lessThan30Rate;
        } else {
            total = (tarif * fullHours) + tarif;
        }

        return total;
    }



}
