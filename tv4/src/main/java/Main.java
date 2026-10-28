import java.util.Arrays;

/** Điểm vào duy nhất của jar: "worker ..." chạy một lượt đo, còn lại là lệnh của Runner. */
public final class Main {
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("worker")) {
            Worker.main(Arrays.copyOfRange(args, 1, args.length));
        } else {
            Runner.main(args);
        }
    }
}
