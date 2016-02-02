package de.envisia.closure;

import com.google.javascript.jscomp.SourceFile;
import com.google.javascript.jscomp.CommandLineRunner;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Result;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class ClosureWrapper extends CommandLineRunner {

    public static void main(String[] args) {
        List<SourceFile> externs = Collections.emptyList();
        List<SourceFile> inputs = Collections.singletonList(
                new SourceFile("default.js"));

        CompilerOptions options = new CompilerOptions();
        CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);
        options.setAngularPass(true);

        com.google.javascript.jscomp.Compiler compiler = new com.google.javascript.jscomp.Compiler();
        Result result = compiler.compile(externs, inputs, options);
        String s = compiler.toSource();
    }

    public ClosureWrapper(String[] args) {
        super(args);
    }

    public void compile() throws IOException, FlagUsageException {
        this.doRun();
    }

}
