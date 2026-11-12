package com.soin.services.git;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class GitService {

    public static ArrayList<String> getStatus(String gitPath, String OSB) {
        System.out.println("Git Path: " + gitPath);
        System.out.println("OSB: " + OSB);
        ArrayList<String> statusLines = new ArrayList<>();
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "status", "--porcelain","--untracked-files=all");
            pb.directory(new java.io.File(gitPath));

            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            int index = 0;
            while ((line = reader.readLine()) != null) {

                if (line.contains(OSB)) {
                    index = line.indexOf(OSB);
                    String s = "**" + line.substring(index + OSB.length());
                    System.out.println(s);
                    statusLines.add(s);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusLines;
    }

}
