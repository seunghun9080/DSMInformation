import org.thinkinggms.utils.ImageUtils;
import org.thinkinggms.utils.URLUtils;

public class Test {
    public static void main(String[] args) {
        ImageUtils.renderTimeTable(URLUtils.parseTimeTable(URLUtils.baseTimeTable(), 2, 2), 2, 2);
    }
}
