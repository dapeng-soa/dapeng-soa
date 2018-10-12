import com.github.dapeng.core.helper.IPUtils;
import com.github.dapeng.router.token.IpToken;
import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TestRegex {
    private static final String IP_REGEX = "(^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\.(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
            + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\.(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d))(/(\\d{2}))?(:(\\d{2,5}))?$";

    private Pattern IP_PATTERN = Pattern.compile(IP_REGEX);

    private static final Pattern SIMPLE = Pattern.compile("\\d{2,4}");

    @Test
    public void testOne() {
        String str = "133";
        Matcher matcher = SIMPLE.matcher(str);

        System.out.println(matcher.matches());

    }

    @Test
    public void testTwo() {
        String str = "192.168.20.103";
        Matcher matcher = IP_PATTERN.matcher(str);

        if (matcher.matches()) {
            String ipStr = matcher.group(1);
            int ip = IPUtils.transferIp(ipStr);
            System.out.println(ip);

            String masks = matcher.group(7);
            if (masks != null) {
                int mask = Integer.parseInt(masks);
                System.out.println(mask);

            }

            String port = matcher.group(8);
            if (port != null) {
                System.out.println(port);
            }
        }else {
            System.err.println("not matcher");
        }

    }
}
