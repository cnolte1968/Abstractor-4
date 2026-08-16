import com.google.openlocationcode.OpenLocationCode;

public class test_olc_java {
    public static void main(String[] args) {
        OpenLocationCode olc = new OpenLocationCode("8FW4V75V+8Q");
        System.out.println(olc.decode().getCenterLatitude() + ", " + olc.decode().getCenterLongitude());
    }
}
