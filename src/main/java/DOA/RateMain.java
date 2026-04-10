package Utils;

import java.sql.Date;
import java.sql.Timestamp;

public class RateMain {
    private int clientId;
    private String language;
    private double ratePerHour;
    private double ratePerDay;
    private int offsetBy;
    private String weekend;
    private int offsetUnit;
    private Date rateApplyDateFrom;
    private Timestamp insertDate;
    private Timestamp updateDate;

    // Getters and setters
    public int getClientId() { return clientId; }
    public void setClientId(int clientId) { this.clientId = clientId; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public double getRatePerHour() { return ratePerHour; }
    public void setRatePerHour(double ratePerHour) { this.ratePerHour = ratePerHour; }

    public double getRatePerDay() { return ratePerDay; }
    public void setRatePerDay(double ratePerDay) { this.ratePerDay = ratePerDay; }

    public int getOffsetBy() { return offsetBy; }
    public void setOffsetBy(int offsetBy) { this.offsetBy = offsetBy; }

    public String getWeekend() { return weekend; }
    public void setWeekend(String weekend) { this.weekend = weekend; }

    public int getOffsetUnit() { return offsetUnit; }
    public void setOffsetUnit(int offsetUnit) { this.offsetUnit = offsetUnit; }

    public Date getRateApplyDateFrom() { return rateApplyDateFrom; }
    public void setRateApplyDateFrom(Date rateApplyDateFrom) { this.rateApplyDateFrom = rateApplyDateFrom; }

    public Timestamp getInsertDate() { return insertDate; }
    public void setInsertDate(Timestamp insertDate) { this.insertDate = insertDate; }

    public Timestamp getUpdateDate() { return updateDate; }
    public void setUpdateDate(Timestamp updateDate) { this.updateDate = updateDate; }
}
