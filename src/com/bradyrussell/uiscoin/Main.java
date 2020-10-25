package com.bradyrussell.uiscoin;


import com.bradyrussell.uiscoin.script.ScriptBuilder;
import com.bradyrussell.uiscoin.script.ScriptExecution;
import com.bradyrussell.uiscoin.script.ScriptOperator;

import java.util.Collections;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        String InputScript;

        if(args.length == 0){
            System.out.println("Please enter a script either in text or base64: ");
            InputScript = scanner.nextLine();
        } else {
            InputScript = args[0];
        }

        //determine whether we are text or base64
        boolean bIsBase64 = !InputScript.strip().contains(" ");

        byte[] ScriptBytes = bIsBase64 ? Util.Base64Decode(InputScript) : new ScriptBuilder(InputScript.length()).fromText(InputScript.strip()).get();

        System.out.println("Compiled script is "+ScriptBytes.length+" bytes. ");
        System.out.println("Compiled script base64: "+Util.Base64Encode(ScriptBytes));
        System.out.println("Compiled script hex: ");
        Util.printBytesHex(ScriptBytes);
        System.out.println("Compiled script as OpCodes: ");
        PrintScriptOpCodes(ScriptBytes);

        System.out.println("Decompiled script: ");
        System.out.println("--------------- Begin Decompiled Script ---------------");
        String DecompiledScript = new ScriptBuilder(ScriptBytes.length).data(ScriptBytes).toText();
        System.out.println(DecompiledScript);
        System.out.println("---------------- End Decompiled Script ----------------");

        System.out.println("How would you like to execute the script? [N]ormally, [S]tep by step, With [B]reakPoints, Step and [E]dit: ");
        String execType = scanner.nextLine();

        ScriptExecution scriptExecution = new ScriptExecution();

        //specify a base64 stack on the command line
        if(args.length > 1) {
            scriptExecution.Initialize(ScriptBytes, Collections.enumeration(Collections.singletonList(Util.Base64Decode(args[1]))));
        } else {
            scriptExecution.Initialize(ScriptBytes);
        }

        System.out.println("Script initialized: "+scriptExecution.Script.length+" bytes long"+(scriptExecution.Stack.size() > 0 ? " with "+scriptExecution.Stack.size()+" elements on the stack.":"."));

        switch (execType.strip().toUpperCase().charAt(0)){
            case 'N'->{
                System.out.println("Instruction: "+(scriptExecution.InstructionCounter));
                while (scriptExecution.Step()) {
                    System.out.println(scriptExecution.getStackContents());
                    System.out.println("Instruction: "+(scriptExecution.InstructionCounter));
                }
            }
            case 'S'->{
                System.out.println("Instruction: "+(scriptExecution.InstructionCounter));
                while (scriptExecution.Step()) {
                    System.out.println(scriptExecution.getStackContents());

                    PrintScriptOpCodesSurroundingHighlight(scriptExecution.Script, scriptExecution.InstructionCounter-1, 5, "Just executed.");

                    System.out.println("Continue? (Y/N)");
                    if(scanner.next().toUpperCase().startsWith("N")) {
                        System.out.println("Script failed due to cancellation!");
                        scriptExecution.bScriptFailed = true;
                        break;
                    }

                    System.out.println("Instruction: "+(scriptExecution.InstructionCounter));
                }

            }
            case 'B'->{

            }
            case 'E'->{

            }

            default -> throw new IllegalStateException("Unexpected value: " + execType.strip().charAt(0));
        }

        System.out.println("Script terminated due to "+(scriptExecution.bScriptFailed?"failure":"success")+" at byte "+(scriptExecution.InstructionCounter-1)+".");

        PrintScriptOpCodesSurroundingHighlight(scriptExecution.Script, scriptExecution.InstructionCounter-1, 10, (scriptExecution.bScriptFailed?"failure":"success")+" occurred here!");
    }

    public static void PrintScriptOpCodesSurroundingHighlight(byte[] Script, int Highlight, int Surrounding, String Message){
        System.out.println("ScriptTrace:");

        int Start = Math.max(0,Highlight-Surrounding);
        int End = Math.min(Script.length-1,Highlight+Surrounding);

        boolean bJustHitPush = false;
        byte pushAmt = 0;

        for (int i = 0; i < Script.length; i++) {
            ScriptOperator byOpCode = ScriptOperator.getByOpCode(Script[i]);

            if(bJustHitPush) {
                pushAmt = (byte) (Script[i]+1);
            }
            if(byOpCode == ScriptOperator.PUSH && pushAmt <= 0) bJustHitPush = true;

            if (i >= Start && i <= End) System.out.print(i);
            if (i >= Start && i <= End) System.out.print(pushAmt > 0 ? " | > " : " | ");
            if (byOpCode == null || pushAmt > 0) {
                if (i >= Start && i <= End) System.out.print("0x");
                if (i >= Start && i <= End) System.out.printf("%02X", Script[i]);
                if(pushAmt > 0) {
                    if(bJustHitPush) {
                        bJustHitPush = false;
                        if (i >= Start && i <= End) System.out.print(" (Push Amount)");
                    } else {
                        if (i >= Start && i <= End) System.out.print(" (Push Byte)");
                    }
                    pushAmt--;
                }
            } else {
                if (i >= Start && i <= End) System.out.print(byOpCode);
            }

            if(i==Highlight){
                System.out.print(" <-- "+Message);
            }

            if (i >= Start && i <= End) System.out.println(" ");
        }
        System.out.println("");
    }

    public static void PrintScriptOpCodes(byte[] Script){
        boolean bJustHitPush = false;
        byte pushAmt = 0;

        for (int i = 0; i < Script.length; i++) {

            ScriptOperator byOpCode = ScriptOperator.getByOpCode(Script[i]);

            if(bJustHitPush) {
                pushAmt = (byte) (Script[i]+1);
            }
            if(byOpCode == ScriptOperator.PUSH && pushAmt <= 0) bJustHitPush = true;

            System.out.print(i);
            System.out.print(pushAmt > 0 ? " | > " : " | ");
            if (byOpCode == null || pushAmt > 0) {
                System.out.print("0x");
                System.out.printf("%02X", Script[i]);
                if(pushAmt > 0) {
                    if(bJustHitPush) {
                        bJustHitPush = false;
                        System.out.print(" (Push Amount)");
                    } else {
                        System.out.print(" (Push Byte)");
                    }
                    pushAmt--;
                }
            } else {
                System.out.print(byOpCode);
            }


            System.out.println(" ");
        }
        System.out.println("");
    }
}
