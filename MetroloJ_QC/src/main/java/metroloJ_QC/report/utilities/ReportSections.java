package metroloJ_QC.report.utilities;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import ij.ImagePlus;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReportSections {
  public int TITLE = 1;
  
  public int TITLE2 = 2;
  
  public Image logoRTMFM() {
    Image logo = null;
    try {
      logo = Image.getInstance(getClass().getResource("/metroloJ_QC/resources/logo_RT-MFM.jpg"));
      logo.setAlignment(1);
      logo.scalePercent(30.0F);
    } catch (BadElementException ex) {
      Logger.getLogger(ReportSections.class.getName()).log(Level.SEVERE, (String)null, (Throwable)ex);
    } catch (MalformedURLException ex) {
      Logger.getLogger(ReportSections.class.getName()).log(Level.SEVERE, (String)null, ex);
    } catch (IOException ex) {
      Logger.getLogger(ReportSections.class.getName()).log(Level.SEVERE, (String)null, ex);
    } 
    return logo;
  }
  
  public PdfPTable wholeSection(String sectionTitle, int titleType, PdfPTable table, String text) {
    PdfPTable output = new PdfPTable(1);
    output.setKeepTogether(true);
    output.getDefaultCell().setHorizontalAlignment(0);
    output.getDefaultCell().setVerticalAlignment(5);
    output.setWidthPercentage(100.0F);
    PdfPCell cell = new PdfPCell();
    cell.setBorder(0);
    if (!sectionTitle.isEmpty())
      switch (titleType) {
        case 1:
          cell.addElement((Element)title(sectionTitle));
          break;
        case 2:
          cell.addElement((Element)title2(sectionTitle));
          break;
      }  
    cell.addElement((Element)table);
    if (!text.isEmpty())
      cell.addElement((Element)paragraph(text)); 
    output.addCell(cell);
    return output;
  }
  
  public PdfPTable wholeSection(String sectionTitle, int titleType, ImagePlus image, float zoom, PdfPTable table, String text) {
    PdfPTable output = new PdfPTable(1);
    output.setKeepTogether(true);
    output.getDefaultCell().setHorizontalAlignment(0);
    output.getDefaultCell().setVerticalAlignment(5);
    output.setWidthPercentage(100.0F);
    PdfPCell cell = new PdfPCell();
    cell.setBorder(0);
    if (!sectionTitle.isEmpty())
      switch (titleType) {
        case 1:
          cell.addElement((Element)title(sectionTitle));
          break;
        case 2:
          cell.addElement((Element)title2(sectionTitle));
          break;
      }  
    cell.addElement((Element)imagePlus(image, zoom));
    if (table != null)
      cell.addElement((Element)table); 
    if (!text.isEmpty())
      cell.addElement((Element)paragraph(text)); 
    output.addCell(cell);
    return output;
  }
  
  public Paragraph bigTitle(String title) {
    Paragraph reportTitle = new Paragraph();
    reportTitle.add((Element)new Chunk(title, new Font(Font.FontFamily.HELVETICA, 14.0F, 1)));
    reportTitle.setAlignment(1);
    reportTitle.setLeading(20.0F);
    reportTitle.setSpacingBefore(15.0F);
    reportTitle.setSpacingAfter(15.0F);
    return reportTitle;
  }
  
  public Paragraph title(String title) {
    Paragraph reportTitle = new Paragraph();
    Font font = new Font(Font.FontFamily.HELVETICA, 12.0F, 1);
    font.setStyle(4);
    reportTitle.add((Element)new Chunk(title, font));
    reportTitle.setAlignment(0);
    reportTitle.setSpacingBefore(15.0F);
    return reportTitle;
  }
  
  public Paragraph title2(String title) {
    Paragraph reportTitle = new Paragraph();
    Font font = new Font(Font.FontFamily.HELVETICA, 11.0F, 0);
    font.setStyle(4);
    reportTitle.add((Element)new Chunk(title, font));
    reportTitle.setAlignment(0);
    reportTitle.setSpacingBefore(15.0F);
    return reportTitle;
  }
  
  public Paragraph paragraph(String title) {
    Paragraph paragraph = new Paragraph();
    Font font = new Font(Font.FontFamily.HELVETICA, 10.0F, 0);
    paragraph.add((Element)new Chunk(title, font));
    paragraph.setAlignment(0);
    paragraph.setSpacingBefore(15.0F);
    return paragraph;
  }
  
  public Paragraph paragraphRight(String title) {
    Paragraph paragraph = new Paragraph();
    Font font = new Font(Font.FontFamily.HELVETICA, 10.0F, 0);
    paragraph.add((Element)new Chunk(title, font));
    paragraph.setAlignment(2);
    paragraph.setSpacingBefore(15.0F);
    return paragraph;
  }
  
  public Image imagePlus(ImagePlus image, float zoom) {
    Image img = null;
    if (zoom > 100.0F || zoom < 0.0F)
      zoom = 100.0F; 
    try {
      img = Image.getInstance(image.getImage(), null);
      img.setAlignment(1);
      img.scalePercent(zoom);
      img.setSpacingBefore(15.0F);
      img.setSpacingAfter(15.0F);
    } catch (BadElementException ex) {
      Logger.getLogger(ReportSections.class.getName()).log(Level.SEVERE, (String)null, (Throwable)ex);
    } catch (IOException ex) {
      Logger.getLogger(ReportSections.class.getName()).log(Level.SEVERE, (String)null, ex);
    } 
    return img;
  }
  
  public PdfPTable table(content[][] list, float widthPercentage, boolean choice) {
    PdfPTable table = new PdfPTable((list[0]).length);
    table.getDefaultCell().setHorizontalAlignment(1);
    table.getDefaultCell().setVerticalAlignment(5);
    table.setWidthPercentage(widthPercentage);
    for (int row = 0; row < list.length; row++) {
      for (int col = 0; col < (list[0]).length; col++) {
        Font font;
        int fontSize = 9;
        if (row == 0 || col == 0)
          fontSize = 10; 
        switch ((list[row][col]).status) {
          default:
            font = new Font(Font.FontFamily.HELVETICA, fontSize, 0, BaseColor.BLACK);
            break;
          case 8:
            font = new Font(Font.FontFamily.HELVETICA, fontSize, 0, BaseColor.RED);
            break;
          case 9:
            font = new Font(Font.FontFamily.HELVETICA, fontSize, 0, BaseColor.GREEN);
            break;
          case 10:
            font = new Font(Font.FontFamily.HELVETICA, fontSize, 0, BaseColor.BLUE);
            break;
        } 
        PdfPCell cell = new PdfPCell((Phrase)new Paragraph(new Chunk((list[row][col]).value, font)));
        cell.setRowspan((list[row][col]).cellRowSpan);
        cell.setColspan((list[row][col]).cellColSpan);
        cell.setVerticalAlignment(5);
        cell.setHorizontalAlignment(1);
        switch ((list[row][col]).status) {
          case 0:
            cell.setBackgroundColor(new BaseColor(255, 255, 255, 41));
            table.addCell(cell);
            break;
          case 1:
            cell.setBackgroundColor(new BaseColor(0, 0, 0, 255));
            table.addCell(cell);
            break;
          case 2:
            if (choice) {
              cell.setBackgroundColor(new BaseColor(0, 255, 0, 41));
            } else {
              cell.setBackgroundColor(new BaseColor(255, 255, 255, 41));
            } 
            table.addCell(cell);
            break;
          case 3:
            if (choice) {
              cell.setBackgroundColor(new BaseColor(255, 0, 0, 41));
            } else {
              cell.setBackgroundColor(new BaseColor(255, 255, 255, 41));
            } 
            table.addCell(cell);
            break;
          case 5:
            cell.setHorizontalAlignment(0);
            cell.setBackgroundColor(new BaseColor(255, 255, 255, 41));
            table.addCell(cell);
            break;
          case 6:
            cell.setBackgroundColor(new BaseColor(255, 255, 255, 41));
            table.addCell(cell);
            break;
          case 7:
            cell.setVerticalAlignment(4);
            table.addCell(cell);
            break;
          case 8:
            cell.setBackgroundColor(new BaseColor(255, 255, 255, 41));
            table.addCell(cell);
            break;
          case 9:
            cell.setBackgroundColor(new BaseColor(255, 255, 255, 41));
            table.addCell(cell);
            break;
          case 10:
            cell.setBackgroundColor(new BaseColor(255, 255, 255, 41));
            table.addCell(cell);
            break;
            
        } 
      } 
    } 
    table.setSpacingBefore(15.0F);
    return table;
  }
  
  public PdfPTable imageTable(content[][] list, float widthPercentage, ImagePlus[] image, float zoom, int imageCol, boolean keepFirstRow) {
    PdfPTable table = new PdfPTable((list[0]).length);
    table.getDefaultCell().setHorizontalAlignment(1);
    table.getDefaultCell().setVerticalAlignment(5);
    table.setWidthPercentage(widthPercentage);
    for (int row = 0; row < list.length; row++) {
      for (int col = 0; col < (list[0]).length; col++) {
        Font font = new Font(Font.FontFamily.HELVETICA, 9.0F, 0);
        PdfPCell cell = new PdfPCell();
        if (keepFirstRow) {
          if (col != imageCol || row == 0) {
            cell = new PdfPCell((Phrase)new Paragraph(new Chunk((list[row][col]).value, font)));
            cell.setVerticalAlignment(5);
            cell.setHorizontalAlignment(0);
            cell.setRowspan((list[row][col]).cellRowSpan);
            cell.setColspan((list[row][col]).cellColSpan);
          } else {
            cell = new PdfPCell(imagePlus(image[row - 1], zoom));
            cell.setVerticalAlignment(5);
            cell.setHorizontalAlignment(1);
            cell.setFixedHeight(1.5F * widthPercentage);
          } 
        } else if (col != imageCol) {
          cell = new PdfPCell((Phrase)new Paragraph(new Chunk((list[row][col]).value, font)));
          cell.setVerticalAlignment(5);
          cell.setHorizontalAlignment(0);
          cell.setRowspan((list[row][col]).cellRowSpan);
          cell.setColspan((list[row][col]).cellColSpan);
        } else {
          cell = new PdfPCell(imagePlus(image[row], zoom));
          cell.setVerticalAlignment(5);
          cell.setHorizontalAlignment(1);
          cell.setFixedHeight(1.5F * widthPercentage);
        } 
        if ((list[row][col]).status != 4)
          table.addCell(cell); 
      } 
    } 
    table.setSpacingBefore(15.0F);
    return table;
  }
  
  public PdfPTable singleImageTable(PdfPTable table, ImagePlus image, float zoom) {
    if (zoom > 100.0F || zoom < 0.0F)
      zoom = 100.0F; 
    PdfPTable mainTable = new PdfPTable(2);
    mainTable.setKeepTogether(true);
    mainTable.setWidthPercentage(100.0F);
    PdfPCell cell = new PdfPCell();
    Image img = null;
    try {
      img = Image.getInstance(image.getImage(), null);
      img.setAlignment(0);
      img.setAlignment(5);
      img.scalePercent(zoom);
      img.setSpacingBefore(15.0F);
      img.setSpacingAfter(15.0F);
    } catch (BadElementException|IOException ex) {
      Logger.getLogger(ReportSections.class.getName()).log(Level.SEVERE, (String)null, ex);
    } 
    cell.setImage(img);
    cell.setBorder(0);
    mainTable.addCell(cell);
    cell = new PdfPCell(table);
    cell.setVerticalAlignment(5);
    cell.setHorizontalAlignment(1);
    cell.setBorder(0);
    mainTable.addCell(cell);
    mainTable.setSpacingBefore(15.0F);
    return mainTable;
  }
}
