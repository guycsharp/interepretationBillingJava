public class PhoneMain {
    // Matches columns in phone_main table
    private int idphone_main;
    private Integer client_id;
    private String phone_number;
    private Boolean soft_delete;

    public PhoneMain() {}

    public int getIdphone_main() {
        return idphone_main;
    }
    public void setIdphone_main(int idphone_main) {
        this.idphone_main = idphone_main;
    }

    public Integer getClient_id() {
        return client_id;
    }
    public void setClient_id(Integer client_id) {
        this.client_id = client_id;
    }

    public String getPhone_number() {
        return phone_number;
    }
    public void setPhone_number(String phone_number) {
        this.phone_number = phone_number;
    }

    public Boolean getSoft_delete() {
        return soft_delete;
    }
    public void setSoft_delete(Boolean soft_delete) {
        this.soft_delete = soft_delete;
    }

    @Override
    public String toString() {
        return "PhoneMain{" +
                "id=" + idphone_main +
                ", clientId=" + client_id +
                ", phone='" + phone_number + '\'' +
                '}';
    }
}
