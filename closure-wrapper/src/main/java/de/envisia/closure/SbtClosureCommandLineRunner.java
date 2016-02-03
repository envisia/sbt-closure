package de.envisia.closure;

import com.google.javascript.jscomp.*;
import com.google.javascript.jscomp.Compiler;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


// CommandLineRunner
public class SbtClosureCommandLineRunner {
    private List<SourceFile> externs = Collections.emptyList();

    public SbtClosureCommandLineRunner() {

    }

    public String compile(List<File> files) {
        List<SourceFile> sf = files.stream().map(SourceFile::fromFile).collect(Collectors.toList());

        CompilerOptions options = new CompilerOptions();
        CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);
        options.setAngularPass(true);
        options.setLanguageIn(CompilerOptions.LanguageMode.ECMASCRIPT6);
        options.setLanguageOut(CompilerOptions.LanguageMode.ECMASCRIPT5);

        Compiler compiler = new Compiler();

        Result result = compiler.compile(externs, sf, options);
        return compiler.toSource();
    }

}
