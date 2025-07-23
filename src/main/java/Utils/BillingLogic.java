package Utils;

import javax.swing.*;

public class BillingLogic {
    /*

     */
    public static double calculateTotalAmount(int offsetBy, int offsetunit, double mins, double lessThan30Rate) {
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
            total = tarif * ((adjustedMin - lessThan30Adjust) / 60) + lessThan30Rate;
        }

        // until minutes are less than (offsetunit + offsetBy) minutes, lessthan30 rate applies
        if (mins <= offsetunit + offsetBy) {
            total = lessThan30Rate;
        }
        return total;
    }
}
