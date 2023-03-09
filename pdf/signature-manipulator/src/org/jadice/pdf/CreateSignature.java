package org.jadice.pdf;

import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSObject;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;

import java.io.*;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Objects;

/**
 * This class can be used to sign PDF documents with different signature sub-filters.
 * TODO: add signature algorithms for different subfilters
 */
class CreateSignature {

  private static final File inputFolder = new File("in");
  private static final File outputFolder = new File("out");

  public static void main(String[] args) {
    for (final File file : gatherInputFiles()) {
      try {
        signWithAdobePkcs7Detached(cloneToOutputFile(file, "_adbe.pkcs7.detached"));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  /**
   * Inspired by: https://stackoverflow.com/questions/62601879/pdfbox-2-0-create-signature-field-and-save-incremental-with-already-signed-docum
   * TODO: Not fully working yet, seems like the certificate is empty
   * @param file
   * @throws IOException
   */
  private static void signWithAdobePkcs7Detached(final File file) throws IOException {
    try (final PDDocument doc = PDDocument.load(file);
         FileOutputStream fos = new FileOutputStream(file)) {
      PDPage page = doc.getPage(0);

      PDAcroForm acroForm = doc.getDocumentCatalog().getAcroForm();
      if (acroForm == null) { // Add a new AcroForm and add that to the document
        acroForm = new PDAcroForm(doc);
        doc.getDocumentCatalog().setAcroForm(acroForm);
      } else {
        acroForm.getCOSObject().setNeedToBeUpdated(true);
        COSObject fields = acroForm.getCOSObject().getCOSObject(COSName.FIELDS);
        if (fields != null)
          fields.setNeedToBeUpdated(true);
      }

      acroForm.setSignaturesExist(true);
      acroForm.setAppendOnly(true);
      acroForm.getCOSObject().setDirect(true);

      // Create empty signature field
      PDSignatureField signatureField = new PDSignatureField(acroForm);

      // this was added by me
      PDSignature signature = getPDSignature(PDSignature.FILTER_ADOBE_PPKLITE, PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
      doc.addSignature(signature);
      signatureField.setValue(signature);
      signature.getCOSObject().setNeedToBeUpdated(true);

      PDAnnotationWidget widget = signatureField.getWidgets().get(0);
      PDRectangle rect = new PDRectangle(50, 250, 200, 50);
      widget.setRectangle(rect);
      widget.getCOSObject().setNeedToBeUpdated(true);
      widget.setPage(page);
      page.getAnnotations().add(widget);
      page.getCOSObject().setNeedToBeUpdated(true);
      acroForm.getFields().add(signatureField);

      // general updates
      doc.getDocumentCatalog().getCOSObject().setNeedToBeUpdated(true);
      doc.saveIncremental(fos);
    }
  }

  private static PDSignature getPDSignature(final COSName filter, final COSName subFilter) {
    PDSignature signature = new PDSignature();
    signature.setFilter(filter);
    signature.setSubFilter(subFilter);
    signature.setName("Test signer");
    signature.setLocation("Test lab");
    signature.setReason("To test");
    signature.setSignDate(Calendar.getInstance());
    return signature;
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