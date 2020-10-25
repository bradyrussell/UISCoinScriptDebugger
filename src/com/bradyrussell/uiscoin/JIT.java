package com.bradyrussell.uiscoin;

import com.bradyrussell.uiscoin.script.ScriptBuilder;
import com.bradyrussell.uiscoin.script.ScriptExecution;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.logging.Level;

public class JIT {
    public static void main(String[] args) {
        Main.setLevel(Level.WARNING);

        Scanner scanner = new Scanner(System.in);

        System.out.println("Would you like to specify a stack? (y/n): ");
        String strSpecifyStack = scanner.nextLine();

        Enumeration<byte[]> stackElements;

        if (strSpecifyStack.toLowerCase().startsWith("y")) {
            System.out.println("Please provide a script to initialize the stack: ");
            System.out.println("(Enter /back anywhere in a line to delete it and the previous line. Enter /quit anywhere in a line to exit without saving that line.)");

            ScriptBuilder sb = ScriptInput.ScannerScriptEntry(scanner);

            ScriptExecution se = new ScriptExecution();
            se.Initialize(sb.get());

            //noinspection StatementWithEmptyBody
            while (se.Step()) ;

            stackElements = se.Stack.elements();
        } else {
            stackElements = Collections.emptyEnumeration();
        }

        JITState jitState = new JITState(10*1024, stackElements);

        while (StepJIT(scanner, jitState));

        ScriptExecution se = jitState.getExecution();

        Main.setLevel(Level.ALL);

        while (se.Step());

        System.out.println(jitState.scriptBuilder.toText());

        System.out.println("Final stack contents: ");
        System.out.println(se.getStackContents());

        System.out.println("Script terminated due to " + (se.bScriptFailed ? "failure" : "success") + " at byte " + (se.InstructionCounter - 1) + ".");

        Main.PrintScriptOpCodesSurroundingHighlight(se.Script, se.InstructionCounter - 1, 10, (se.bScriptFailed ? "failure" : "success") + " occurred here!");

        System.out.println("Script largest stack was "+jitState.LargestStack+" bytes and the deepest stack was "+jitState.DeepestStack+" elements deep.");

        System.out.println("Script Base64: ");
        System.out.println(Util.Base64Encode(se.Script));
    }

    public static boolean StepJIT(Scanner scanner, JITState state) {
        String scriptLine = ScriptInput.getScriptLine(scanner, state.linesList.size());
        if (scriptLine.toLowerCase().contains("/back")) {
            state.linesList.remove(state.linesList.size() - 1);
            return true;
        }
        if (scriptLine.toLowerCase().contains("/quit")) {
            return false;
        }
        state.linesList.add(scriptLine.strip());

        state.UpdateScriptBuilder();
        ScriptExecution stateExecution = state.getExecution();

        while (stateExecution.Step());

        System.out.println(stateExecution.getStackContents());

        if(stateExecution.getStackDepth() > state.DeepestStack) state.DeepestStack = stateExecution.getStackDepth();
        if(stateExecution.getStackBytes() > state.DeepestStack) state.LargestStack = stateExecution.getStackBytes();

        return !stateExecution.bScriptFailed;
    }

    private static class JITState {
        ArrayList<String> linesList;
        ScriptBuilder scriptBuilder;
        Enumeration<byte[]> initialStack;

        int DeepestStack = -1;
        int LargestStack = -1;

        public JITState(int BufferSize) {
            this.linesList = new ArrayList<>();
            this.scriptBuilder = new ScriptBuilder(BufferSize);
            this.initialStack = Collections.emptyEnumeration();
        }

        public JITState(int BufferSize, Enumeration<byte[]> initialStack) {
            this.linesList = new ArrayList<>();
            this.scriptBuilder = new ScriptBuilder(BufferSize);
            this.initialStack = initialStack;
        }

        public void UpdateScriptBuilder(){
            StringBuilder strBuilder = new StringBuilder();
            for (String s : linesList) {
                strBuilder.append(s);
                strBuilder.append("\n");
            }

            try {
                scriptBuilder = new ScriptBuilder(strBuilder.length()).fromText(strBuilder.toString());
            } catch (IllegalArgumentException e){
                System.out.println("Invalid script operator! Changing line to comment!");
                String comment = "#"+linesList.get(linesList.size() - 1)+"#";
                linesList.remove(linesList.size() - 1);
                linesList.add(comment);
            }

        }

        public ScriptExecution getExecution(){
            ScriptExecution scriptExecution = new ScriptExecution();
            scriptExecution.Initialize(scriptBuilder.get(),initialStack);
            return scriptExecution;
        }
    }
}
