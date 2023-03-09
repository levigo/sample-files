package org.jadice.pdf;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * This class can be used to break existing signatures of PDF documents by editing the documents' text.
 */
public class BreakSignature {
  private static final File inputFolder = new File("in");
  private static final File outputFolder = new File("out");

  public static void main(String[] args) {
    for (final File file : gatherInputFiles()) {
      try {
        breakSignature(cloneToOutputFile(file, "_broken_signature"));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private static void breakSignature(final File file) throws IOException {
    try (final PDDocument doc = PDDocument.load(file);
         FileOutputStream fos = new FileOutputStream(file)) {
      PDPageContentStream contentStream = new PDPageContentStream(doc, doc.getPage(0), PDPageContentStream.AppendMode.APPEND, true);
      contentStream.setFont(PDType1Font.TIMES_ROMAN, 14);
      contentStream.beginText();
      contentStream.showText("This text should break existing signature(s).");
      contentStream.endText();
      contentStream.close();
      doc.save(fos);
    }
  }

  private static File cloneToOutputFile(final File inputFile, final String suffix) throws IOException {
    final String inputFileName = inputFile.getName();
    final String outputFileName = inputFileName.substring(0, inputFileName.length() - 4) + suffix + ".pdf";
    final File outputFile = new File(outputFolder, outputFileName);
    FileUtils.copyFile(inputFile, outputFile);
    return outputFile;
  }

  private static List<File> gatherInputFiles() {
    return Arrays.asList(
        Objects.requireNonNull(
            inputFolder.listFiles((file, s) -> s.endsWith(".pdf"))
        )
    );
  }
}
