package UI;

import javax.swing.*;
import java.sql.Timestamp;
import java.util.Date;

public class CreateComponents {

    public static JSpinner makeDateSpinner() {
        SpinnerDateModel model = new SpinnerDateModel(new Date(), null, null, java.util.Calendar.DAY_OF_MONTH);
        JSpinner spinner = new JSpinner(model);
        spinner.setEditor(new JSpinner.DateEditor(spinner, "yyyy-MM-dd"));
        return spinner;
    }

    public static Object setDateSpinnerValue(JSpinner dateSpinner, Object returnObj){
        if (returnObj instanceof Timestamp) {
            return (new Date(((Timestamp) returnObj).getTime()));
        } else if (returnObj instanceof Date) {
           return (returnObj);
        }
        return null;
    }
}
