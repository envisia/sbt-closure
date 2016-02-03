package de.envisia.closure;

import com.google.javascript.jscomp.CommandLineRunner;
import com.google.javascript.jscomp.SourceFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;


// CommandLineRunner
public class SbtClosureCommandLineRunner extends CommandLineRunner {
    private List<SourceFile> externs = Collections.emptyList();

    public SbtClosureCommandLineRunner(String[] args) {
        super(args);
    }

    public void compile() throws IOException, FlagUsageException {
        doRun();
    }

}
