import java.sql.Timestamp;

public class ClientMain {
    // Matches columns in client_main table
    private int idclient_main;
    private String client_name;
    private String client_address;
    private Integer client_rate;
    private Integer client_rate_per_day;
    private String phone_number;
    private Timestamp insert_date;
    private Timestamp update_date;
    private Boolean soft_delete;

    public ClientMain() {}

    public int getIdclient_main() {
        return idclient_main;
    }
    public void setIdclient_main(int idclient_main) {
        this.idclient_main = idclient_main;
    }

    public String getClient_name() {
        return client_name;
    }
    public void setClient_name(String client_name) {
        this.client_name = client_name;
    }

    public String getClient_address() {
        return client_address;
    }
    public void setClient_address(String client_address) {
        this.client_address = client_address;
    }

    public Integer getClient_rate() {
        return client_rate;
    }
    public void setClient_rate(Integer client_rate) {
        this.client_rate = client_rate;
    }

    public Integer getClient_rate_per_day() {
        return client_rate_per_day;
    }
    public void setClient_rate_per_day(Integer client_rate_per_day) {
        this.client_rate_per_day = client_rate_per_day;
    }

    public String getPhone_number() {
        return phone_number;
    }
    public void setPhone_number(String phone_number) {
        this.phone_number = phone_number;
    }

    public Timestamp getInsert_date() {
        return insert_date;
    }
    public void setInsert_date(Timestamp insert_date) {
        this.insert_date = insert_date;
    }

    public Timestamp getUpdate_date() {
        return update_date;
    }
    public void setUpdate_date(Timestamp update_date) {
        this.update_date = update_date;
    }

    public Boolean getSoft_delete() {
        return soft_delete;
    }
    public void setSoft_delete(Boolean soft_delete) {
        this.soft_delete = soft_delete;
    }

    @Override
    public String toString() {
        return "ClientMain{" +
                "id=" + idclient_main +
                ", name='" + client_name + '\'' +
                ", address='" + client_address + '\'' +
                '}';
    }
}
