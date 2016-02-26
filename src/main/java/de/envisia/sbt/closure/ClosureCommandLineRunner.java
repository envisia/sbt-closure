package de.envisia.sbt.closure;

import com.google.javascript.jscomp.CommandLineRunner;

import java.io.*;
import java.util.Optional;


public class ClosureCommandLineRunner extends CommandLineRunner {

    public ClosureCommandLineRunner(String[] args) {
        super(args);
    }

    public void compile(File target, Optional<String> sourceMap) throws IOException, FlagUsageException {
        doRun();
        sourceMap.filter((data) -> target.exists()).ifPresent((s) -> {
            try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(target, true)))) {
                out.print("//# sourceMappingURL=");
                out.print(s);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

}
