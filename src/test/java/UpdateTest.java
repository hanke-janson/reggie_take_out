import org.junit.jupiter.api.Test;

public class UpdateTest {
    @Test
    public void testDemo() {
        String fileName = "xxxxx.jpg";
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        System.out.println(suffix);
    }
}
