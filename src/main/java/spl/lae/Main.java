package spl.lae;

import java.io.IOException;
import parser.*;

public class Main {

    public static void main(String[] args) throws IOException {

        if (args.length < 3) {
            System.err.println("Usage: java Main <numThreads> <input.json> <output.json>");
            return;
        }

        int numThreads;
        try {
            numThreads = Integer.parseInt(args[0]);
            if (numThreads <= 0) {
                System.err.println("Error: numThreads must be a positive integer.");
                return;
            }
        } catch (NumberFormatException e) {
            System.err.println("Error: numThreads must be an integer.");
            return;
        }

        String inputPath = args[1];
        String outputPath = args[2];

        LinearAlgebraEngine lae = new LinearAlgebraEngine(numThreads);
        InputParser inputParser = new InputParser();

        try {
            ComputationNode root = inputParser.parse(inputPath);
            ComputationNode res = lae.run(root);
            OutputWriter.write(res.getMatrix(), outputPath);
            System.out.println("Computation completed successfully!");
        } catch (Exception e) {
            System.err.println("Computation failed: " + e.getMessage());
            OutputWriter.write("Error: " + e.getMessage(), outputPath);
        } finally {
            System.out.println("\n=== Worker Report ===");
            System.out.println(lae.getWorkerReport());
        }
    }
}
