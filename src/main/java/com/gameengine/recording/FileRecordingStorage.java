package com.gameengine.recording;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FileRecordingStorage implements RecordingStorage {
    private BufferedWriter writer;

    @Override
    public void openWriter(String path) throws IOException {
        Path p = Paths.get(path);
        if (p.getParent() != null) Files.createDirectories(p.getParent());
        writer = Files.newBufferedWriter(p);
    }

    @Override
    public void writeLine(String line) throws IOException {
        if (writer == null) throw new IllegalStateException("writer not opened");
        writer.write(line);
        writer.newLine();
    }

    @Override
    public void closeWriter() {
        if (writer != null) {
            try { writer.flush(); } catch (Exception ignored) {}
            try { writer.close(); } catch (Exception ignored) {}
            writer = null;
        }
    }

    @Override
    public Iterable<String> readLines(String path) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(Paths.get(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    @Override
    public List<File> listRecordings() {
        File dir = new File("recordings");
        if (!dir.exists() || !dir.isDirectory()) return new ArrayList<>();
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json") || name.endsWith(".jsonl"));
        if (files == null) return new ArrayList<>();
        Arrays.sort(files, (a,b) -> Long.compare(b.lastModified(), a.lastModified()));
        return new ArrayList<>(Arrays.asList(files));
    }
}
