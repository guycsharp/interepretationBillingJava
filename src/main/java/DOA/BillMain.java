import java.sql.Timestamp;

public class BillMain {
    // Matches columns in bill_main table
    private int idbill_main;
    private String service_rendered;
    private Integer UnitDay;
    private Integer workedDayOrHours;
    private String CityServiced;
    private Timestamp insert_date;
    private Timestamp updated_date;

    public BillMain() {}

    public int getIdbill_main() {
        return idbill_main;
    }
    public void setIdbill_main(int idbill_main) {
        this.idbill_main = idbill_main;
    }

    public String getService_rendered() {
        return service_rendered;
    }
    public void setService_rendered(String service_rendered) {
        this.service_rendered = service_rendered;
    }

    public Integer getUnitDay() {
        return UnitDay;
    }
    public void setUnitDay(Integer unitDay) {
        UnitDay = unitDay;
    }

    public Integer getWorkedDayOrHours() {
        return workedDayOrHours;
    }
    public void setWorkedDayOrHours(Integer workedDayOrHours) {
        this.workedDayOrHours = workedDayOrHours;
    }

    public String getCityServiced() {
        return CityServiced;
    }
    public void setCityServiced(String cityServiced) {
        CityServiced = cityServiced;
    }

    public Timestamp getInsert_date() {
        return insert_date;
    }
    public void setInsert_date(Timestamp insert_date) {
        this.insert_date = insert_date;
    }

    public Timestamp getUpdated_date() {
        return updated_date;
    }
    public void setUpdated_date(Timestamp updated_date) {
        this.updated_date = updated_date;
    }

    @Override
    public String toString() {
        return "BillMain{" +
                "id=" + idbill_main +
                ", service='" + service_rendered + '\'' +
                ", city='" + CityServiced + '\'' +
                '}';
    }
}
