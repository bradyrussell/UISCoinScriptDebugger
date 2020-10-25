package com.bradyrussell.uiscoin;

import com.bradyrussell.uiscoin.script.ScriptBuilder;
import com.bradyrussell.uiscoin.script.ScriptExecution;
import com.bradyrussell.uiscoin.script.ScriptOperator;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Scanner;

public class ScriptInput {
    public static String getScriptLine(Scanner scanner, int LineNumber){
        System.out.print(LineNumber+"\t>> ");
        return scanner.nextLine()+"\n";
    }

    @NotNull
    public static ScriptBuilder ScannerScriptEntry(Scanner scanner) {
        ArrayList<String> linesList = new ArrayList<>();

        while (true) {
            String scriptLine = getScriptLine(scanner, linesList.size());
            if (scriptLine.toLowerCase().contains("/back")) {
                linesList.remove(linesList.size() - 1);
                continue;
            }
            linesList.add(scriptLine);
            if (scriptLine.toLowerCase().contains("return")) break;
        }

        StringBuilder strBuilder = new StringBuilder();
        for (String s : linesList) {
            strBuilder.append(s);
            strBuilder.append("\n");
        }

        ScriptBuilder sb = new ScriptBuilder(strBuilder.length()).fromText(strBuilder.toString());
        System.out.println(sb.toText());
        return sb;
    }
}
