import java.util.Optional;

/**
 * @author <a href=mailto:leihuazhe@gmail.com>maple</a>
 * @since 2018-10-24 11:17 AM
 */
public class OptionalTest {

    public static void main(String[] args) {
        Optional<Object> objectOptional = Optional.ofNullable(null);

        objectOptional.isPresent();

        Object o = objectOptional.get();

        System.out.println(o);
    }
}
