package metroloJ_QC.report.utilities;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import ij.ImagePlus;
import ij.IJ;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is used for formatting of various types of pdf report sections
 */
public class ReportSections {
  
  public int TITLE = 1;
  public int TITLE2 = 2;
  
/**
 * Retrieves and returns an Image object representing the RT-MFM logo.
 *This method loads an image file representing the RT-MFM logo, scales it to a 
 * specific size, and aligns it. The image is loaded using the ClassLoader and 
 * the file path "images/logo_RT-MFM.jpg". If the image cannot be loaded or 
 * there are errors during the process, appropriate exceptions are caught 
 * and logged, and null is returned.
 *
 * @return An Image object representing the RT-MFM logo, or null if there 
 * was an error loading the image.
 */
    public Image logoRTMFM() {
    Image logo = null;
    try {
      String fileName = "images/logo_RT-MFM.jpg";      
      logo = Image.getInstance(this.getClass().getClassLoader().getResource(fileName));
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
  
  /**
 * Retrieves and returns an Image object based on the given image file name, 
 * size, and alignment.
 *
 * This method loads an image file specified by the provided name from the 
 "images/" directory, scales it to the specified size, and aligns it. 
 * The image is loaded using the ClassLoader. If the image cannot be loaded 
 * or there are errors during the process, appropriate exceptions are caught 
 * and logged, and null is returned.
 *
 * @param name The file name of the image to be loaded (e.g., "logo.png").
 * @param size The percentage by which to scale the image (e.g., 30.0F for 30% scaling).
 * @param debugMode True if debug mode is enabled, false otherwise.
 * @return An Image object representing the specified image, or null if there was an error loading the image.
 */
    public Image logo(String name, Float size, boolean debugMode) {
    Image logo = null;
    try {
      String fileName = "images/"+name;
      //String fileName = "images/pp.png"; 
      if (debugMode) IJ.log("(in ReportSections>logo) filename "+fileName);
      logo = Image.getInstance(this.getClass().getClassLoader().getResource(fileName));
      logo.setAlignment(1);
      logo.scalePercent(size);
    } catch (BadElementException ex) {
      Logger.getLogger(ReportSections.class.getName()).log(Level.SEVERE, (String)null, (Throwable)ex);
    } catch (MalformedURLException ex) {
      Logger.getLogger(ReportSections.class.getName()).log(Level.SEVERE, (String)null, ex);
    } catch (IOException ex) {
      Logger.getLogger(ReportSections.class.getName()).log(Level.SEVERE, (String)null, ex);
    } 
    return logo;
  }
  
  /**
 * Creates a section within the report consisting of a title, a table, and optional text.
 *
 * This method constructs a section within the report that includes a title 
 * (formatted based on the specified title type), a table, and optional text. 
 * The section is represented as a PdfPTable. The title, table, and text are 
 * added as elements within cells of the PdfPTable, following specified formatting.
 *
 * @param sectionTitle The title of the section.
 * @param titleType The type of title to use (TITLE for primary title, TITLE2 for secondary title).
 * @param table The PdfPTable to include in the section.
 * @param text Optional text to include in the section.
 * @return A PdfPTable representing the entire section with the specified title, table, and text.
 */
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
  /**
 * Creates a section within the report consisting of a title, an image, a table, and optional text.This method constructs a section within the report that includes a title
 (formatted based on the specified title type), an image scaled at a specified size, 
 a table, and optional text.The section is represented as a PdfPTable.
 *
 * The title, table, and text are 
 added as elements within cells of the PdfPTable, following specified formatting.
 *
 * @param sectionTitle The title of the section.
 * @param titleType The type of title to use (TITLE for primary title, TITLE2 for secondary title).
 * @param image : an ImagePlus instance
 * @param zoom The percentage by which to scale the image (e.g., 30.0F for 30% scaling).
 * @param table The PdfPTable to include in the section.
 * @param text Optional text to include in the section.
 * @return A PdfPTable representing the entire section with the specified title, table, and text.
 */
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
    Paragraph output = new Paragraph();
    output.add((Element)new Chunk(title, new Font(Font.FontFamily.HELVETICA, 14.0F, 1)));
    output.setAlignment(1);
    output.setLeading(20.0F);
    output.setSpacingBefore(15.0F);
    output.setSpacingAfter(15.0F);
    return output;
  }
  /**
 * Creates a large, bold title paragraph for the report.
 * This method constructs a paragraph representing a big title with a specified 
 * font style, size, and alignment. The title is typically used to denote major 
 * sections or headings in the report.
 *
 * @param title The text to be displayed as the big title.
 * @return A Paragraph representing the big title.
 */
  public Paragraph title(String title) {
    Paragraph output = new Paragraph();
    Font font = new Font(Font.FontFamily.HELVETICA, 12.0F, 1);
    font.setStyle(4);
    output.add((Element)new Chunk(title, font));
    output.setAlignment(0);
    output.setSpacingBefore(15.0F);
    return output;
  }
  /**
 * Creates a secondary title paragraph for the report.
 *
 * This method constructs a paragraph representing a secondary title 
 * with a specified font style, size, and alignment. 
 * The secondary title is typically used to provide additional information or 
 * details within a section of the report.
 *
 * @param title The text to be displayed as the secondary title.
 * @return A Paragraph representing the secondary title.
 */
  public Paragraph title2(String title) {
    Paragraph output = new Paragraph();
    Font font = new Font(Font.FontFamily.HELVETICA, 11.0F, 0);
    font.setStyle(4);
    output.add((Element)new Chunk(title, font));
    output.setAlignment(0);
    output.setSpacingBefore(15.0F);
    return output;
  }
 /**
 * Creates a paragraph with a title followed by its corresponding value.
 * This method constructs a paragraph containing a title followed by its associated value,
 * formatted with specified font styles and alignment.
 * @param title The text representing the title.
 * @param value The text representing the corresponding value.
 * @return A Paragraph containing the formatted title and value.
 */ public Paragraph titleANDValue(String title, String value) {
    Paragraph output=new Paragraph();
    Phrase phrase=new Phrase();
    Font titleFont = new Font(Font.FontFamily.HELVETICA, 11.0F, 0);
    titleFont.setStyle(4);
    Chunk c1=new Chunk(title, titleFont);
    phrase.add(c1);
    Font valueFont = new Font(Font.FontFamily.HELVETICA, 11.0F, 0);
    valueFont.setStyle(0);
    Chunk c2=new Chunk(value, valueFont);
    phrase.add(c2);
    output.add((Element) phrase);
    output.setAlignment(0);
    return output;
  }
  
/**
 * Creates a paragraph with specified text, left-aligned.
 * This method constructs a paragraph with the provided text, 
 * using a specified font and alignment.
 *
 * @param title The text content for the paragraph.
 * @return A Paragraph object with the provided text, left-aligned.
 */
  public Paragraph paragraph(String title) {
    Paragraph paragraph = new Paragraph();
    Font font = new Font(Font.FontFamily.HELVETICA, 10.0F, 0);
    paragraph.add((Element)new Chunk(title, font));
    paragraph.setAlignment(0);
    paragraph.setSpacingBefore(15.0F);
    return paragraph;
  }
 /**
 * Creates a paragraph with specified text, right-aligned.
 * This method constructs a paragraph with the provided text, 
 * using a specified font and alignment.
 *
 * @param title The text content for the paragraph.
 * @return A Paragraph object with the provided text, right-aligned.
 */
  public Paragraph paragraphRight(String title) {
    Paragraph paragraph = new Paragraph();
    Font font = new Font(Font.FontFamily.HELVETICA, 10.0F, 0);
    paragraph.add((Element)new Chunk(title, font));
    paragraph.setAlignment(2);
    paragraph.setSpacingBefore(15.0F);
    return paragraph;
  }
 /**
 * Creates an Image object from an ImagePlus with specified zoom and alignment.
 * This method constructs an Image object using the provided ImagePlus and 
 * adjusts the zoom and alignment accordingly (center alignment).
 * @param image The ImagePlus object to create an Image from.
 * @param zoom The zoom percentage for the image (0-100).
 * @return An Image object representing the ImagePlus with the 
 * specified zoom and center alignment.
 */
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
 /**
 * Creates a PdfPTable table with specified content and appearance parameters.
 *
 * @param list The content for the table represented as a 2D array of content objects.
 * @param widthPercentage The width percentage of the table relative to the available width.
 * @param choice A boolean indicating the choice to apply specific cell background colors 
 * (according to the content object).
 * @return A PdfPTable with the specified content and appearance.
 */
  public PdfPTable table(content[][] list, float widthPercentage, boolean choice) {
    PdfPTable table = new PdfPTable((list[0]).length);
    table.getDefaultCell().setHorizontalAlignment(1);
    table.getDefaultCell().setVerticalAlignment(5);
    table.setWidthPercentage(widthPercentage);
    for (int row = 0; row < list.length; row++) {
      for (int col = 0; col < (list[0]).length; col++) {
        Font font;
        int fontSize = 9;
        if (row == 0 || col == 0) fontSize = 10; 
        switch ((list[row][col]).status) {
          default:
            font = new Font(Font.FontFamily.HELVETICA, fontSize, 0, BaseColor.BLACK);
            break;
          case content.RED_TEXT:
            font = new Font(Font.FontFamily.HELVETICA, fontSize, 0, BaseColor.RED);
            break;
          case content.GREEN_TEXT:
            font = new Font(Font.FontFamily.HELVETICA, fontSize, 0, BaseColor.GREEN);
            break;
          case content.BLUE_TEXT:
            font = new Font(Font.FontFamily.HELVETICA, fontSize, 0, BaseColor.BLUE);
            break;
        } 
        PdfPCell cell = new PdfPCell((Phrase)new Paragraph(new Chunk((list[row][col]).value, font)));
        cell.setRowspan((list[row][col]).cellRowSpan);
        cell.setColspan((list[row][col]).cellColSpan);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        switch ((list[row][col]).status) {
          case content.TEXT:
            cell.setBackgroundColor(new BaseColor(255, 255, 255, 41));
            table.addCell(cell);
            break;
          case content.BLANK:
            cell.setBackgroundColor(new BaseColor(0, 0, 0, 255));
            table.addCell(cell);
            break;
          case content.PASSED:
            if (choice) {
              cell.setBackgroundColor(new BaseColor(0, 255, 0, 41));
            } else {
              cell.setBackgroundColor(new BaseColor(255, 255, 255, 41));
            } 
            table.addCell(cell);
            break;
          case content.FAILED:
            if (choice) {
              cell.setBackgroundColor(new BaseColor(255, 0, 0, 41));
            } else {
              cell.setBackgroundColor(new BaseColor(255, 255, 255, 41));
            } 
            table.addCell(cell);
            break;
          case content.RIGHTTEXT:
            cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            cell.setBackgroundColor(new BaseColor(255, 255, 255, 41));
            table.addCell(cell);
            break;  
            
          case content.LEFTTEXT:
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            cell.setBackgroundColor(new BaseColor(255, 255, 255, 41));
            table.addCell(cell);
            break;
          case content.FORM:
            cell.setBackgroundColor(new BaseColor(255, 255, 255, 41));
            table.addCell(cell);
            break;
          case content.MIDDLETOP:
            cell.setVerticalAlignment(Element.ALIGN_TOP);
            table.addCell(cell);
            break;
          case content.RED_TEXT:
            cell.setBackgroundColor(new BaseColor(255, 255, 255, 41));
            table.addCell(cell);
            break;
          case content.GREEN_TEXT:
            cell.setBackgroundColor(new BaseColor(255, 255, 255, 41));
            table.addCell(cell);
            break;
          case content.BLUE_TEXT:
            cell.setBackgroundColor(new BaseColor(255, 255, 255, 41));
            table.addCell(cell);
            break;
            
        } 
      } 
    } 
    table.setSpacingBefore(15.0F);
    return table;
  }
 /**
 * Creates a PdfPTable with specified content and images in designated columns, 
 * applying zoom and appearance parameters.
 * @param list The content for the table represented as a 2D array of content objects.
 * @param widthPercentage The width percentage of the table relative to the available width.
 * @param image An array of ImagePlus objects to be included in the table.
 * @param zoom The zoom percentage to apply to the image.
 * @param imageCol The column index for inserting image.
 * @param keepFirstRow A boolean indicating whether to keep the first row unchanged (for header purposes).
 * @return A PdfPTable with the specified content, images, and appearance.
 */
  public PdfPTable imageTable(content[][] list, float widthPercentage, ImagePlus[] image, float zoom, int imageCol, boolean keepFirstRow) {
    PdfPTable table = new PdfPTable((list[0]).length);
    table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
    table.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
    table.setWidthPercentage(widthPercentage);
    for (int row = 0; row < list.length; row++) {
      for (int col = 0; col < (list[0]).length; col++) {
        Font font = new Font(Font.FontFamily.HELVETICA, 9.0F, 0);
        PdfPCell cell = new PdfPCell();
        if (keepFirstRow) {
          if (col != imageCol || row == 0) {
            cell = new PdfPCell((Phrase)new Paragraph(new Chunk((list[row][col]).value, font)));
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setHorizontalAlignment(Element.ALIGN_LEFT);
            cell.setRowspan((list[row][col]).cellRowSpan);
            cell.setColspan((list[row][col]).cellColSpan);
          } else {
            cell = new PdfPCell(imagePlus(image[row - 1], zoom));
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setFixedHeight(1.5F * widthPercentage);
          } 
        } else if (col != imageCol) {
          cell = new PdfPCell((Phrase)new Paragraph(new Chunk((list[row][col]).value, font)));
          cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
          cell.setHorizontalAlignment(Element.ALIGN_LEFT);
          cell.setRowspan((list[row][col]).cellRowSpan);
          cell.setColspan((list[row][col]).cellColSpan);
        } else {
          cell = new PdfPCell(imagePlus(image[row], zoom));
          cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
          cell.setHorizontalAlignment(Element.ALIGN_CENTER);
          cell.setFixedHeight(1.5F * widthPercentage);
        } 
        if ((list[row][col]).status != 4)
          table.addCell(cell); 
      } 
    } 
    table.setSpacingBefore(15.0F);
    return table;
  }

/**
 * Creates a borderless PdfPTable with an ImagePlus and another PdfPTable, 
 * displaying the image and content side by side.
 *
 * @param table The PdfPTable containing content to be displayed alongside the image.
 * @param image The ImagePlus object to be displayed in the main borderlesstable.
 * @param zoom The zoom percentage to apply to the image.
 * @return A PdfPTable with the specified image and content, displayed side by side.
 */
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
/**
 * Creates a (main) borderless PdfPTable containing two sub-tables side by side, 
 * each displaying the provided content.
 * @param list1 The content for the first sub-table.
 * @param width1 The relative width of the first sub-table.
 * @param list2 The content for the second sub-table.
 * @param width2 The relative width of the second sub-table.
 * @param widthPercentage The width percentage of the main table.
 * @return A PdfPTable containing the two sub-tables displaying the specified content side by side.
 */
  public PdfPTable tableOfTables(content[][] list1, float width1, content[][] list2, float width2, float widthPercentage) throws DocumentException {
    PdfPTable mainTable = new PdfPTable(2);
    mainTable.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
    mainTable.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);
    mainTable.setKeepTogether(true);
    mainTable.setWidthPercentage(100.0F);
    PdfPCell cell = new PdfPCell();
    float tempWidth=(widthPercentage*width1)/(width1+width2);
    PdfPTable table1=table(list1, tempWidth, false);
    tempWidth=(widthPercentage*width2)/(width1+width2);
    PdfPTable table2=table(list2, tempWidth, false);
    cell = new PdfPCell(table1);
    cell.setVerticalAlignment(Element.ALIGN_CENTER);
    cell.setHorizontalAlignment(Element.ALIGN_MIDDLE);
    cell.setBorder(0);
    mainTable.addCell(cell);
    cell = new PdfPCell(table2);
    cell.setVerticalAlignment(Element.ALIGN_CENTER);
    cell.setHorizontalAlignment(Element.ALIGN_MIDDLE);
    cell.setBorder(0);
    mainTable.addCell(cell);   
    return mainTable;
  }
 /**
 * Creates a PdfPTable for displaying panel XY, XZ and ZY views in a specified format.
 * @param panelViews An array of ImagePlus objects representing the panel views.
 * panelviews[0] contains the XY view, panelviews[1] contains the YZ view, 
 * panelviews[2] contains the XZ view, 
 * @param type The type of the panel view, e.g., "coa" or "pp".
 * @return A PdfPTable displaying the panel views in the specified format.
 * @throws DocumentException If there's an error while creating the PDF document.
 */
  public PdfPTable panelViewTable(ImagePlus[] panelViews, String type) throws DocumentException {
    PdfPTable table = new PdfPTable(4);
    table.getDefaultCell().setHorizontalAlignment(Element.ALIGN_CENTER);
    table.getDefaultCell().setVerticalAlignment(Element.ALIGN_MIDDLE);

    table.setKeepTogether(true);
    Font font = new Font(Font.FontFamily.HELVETICA, 9.0F, 0);
    PdfPCell cell = new PdfPCell();
    cell.setBorder(0);
    cell = new PdfPCell((Phrase)new Paragraph(new Chunk("XY", font)));
    cell.setVerticalAlignment(Element.ALIGN_TOP);
    cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
    cell.setBorder(0);
    table.addCell(cell); 
    float zoom = (10000 / Math.max(panelViews[0].getWidth(), panelViews[0].getHeight()));
    cell = new PdfPCell(imagePlus(panelViews[0], zoom));
    cell.setVerticalAlignment(Element.ALIGN_TOP);
    cell.setHorizontalAlignment(Element.ALIGN_LEFT);
    cell.setBorder(0);
    table.addCell(cell); 
    cell = new PdfPCell((Phrase)new Paragraph(new Chunk("YZ", font)));
    cell.setVerticalAlignment(Element.ALIGN_TOP);
    cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
    cell.setBorder(0);
    table.addCell(cell); 
    cell = new PdfPCell(imagePlus(panelViews[1], zoom));
    cell.setVerticalAlignment(Element.ALIGN_TOP);
    cell.setHorizontalAlignment(Element.ALIGN_LEFT);
    cell.setBorder(0);
    table.addCell(cell);  
    table.setSpacingBefore(15.0F);
    cell = new PdfPCell((Phrase)new Paragraph(new Chunk("XZ", font)));
    cell.setVerticalAlignment(Element.ALIGN_TOP);
    cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
    cell.setBorder(0);
    table.addCell(cell); 
    cell = new PdfPCell(imagePlus(panelViews[2], zoom));
    cell.setVerticalAlignment(Element.ALIGN_TOP);
    cell.setHorizontalAlignment(Element.ALIGN_LEFT);
    cell.setBorder(0);
    table.addCell(cell); 
    cell = new PdfPCell((Phrase)new Paragraph(new Chunk("", font)));
    cell.setBorder(0);
    table.addCell(cell);
    table.addCell(cell);
    float[] columnWidths = { 7.0F, 40.0F, 7.0F, 40.0F };
    table.setWidths(columnWidths);
        switch (type) {
        case "coa" : table.setWidthPercentage(50.0F);
        break;
        case "pp" : table.setWidthPercentage(50.0F);
        break;
    }
    return table;
  }
 /**
 * Calculates column widths for a PdfPTable based on specified widths for the first three columns,
 * the number of other columns, and the total desired width.
 *
 * @param firstColumnWidth Width of the first column.
 * @param secondColumnWidth Width of the second column.
 * @param thirdColumnWidth Width of the third column.
 * @param numberOfOtherColumns Number of other columns.
 * @param totalWidth Total desired width for the table.
 * @return An array of float representing the calculated column widths for the table.
 */
  public float[] getColumnWidths(float firstColumnWidth, float secondColumnWidth, float thirdColumnWidth, int numberOfOtherColumns, float totalWidth) {
        float [] output=null;
        int firstOtherColumn=3;
        if (firstColumnWidth!=0.0F && secondColumnWidth!=0.0F && thirdColumnWidth!=0.0F) {
            output= new float [3+numberOfOtherColumns];
            output[0]=firstColumnWidth;
            output[1]=secondColumnWidth;
            output[2]=thirdColumnWidth;
        }
        if ((firstColumnWidth!=0.0F && secondColumnWidth!=0.0F && thirdColumnWidth==0.0F)||(firstColumnWidth!=0.0F && secondColumnWidth==0.0F && thirdColumnWidth!=0.0F)||(firstColumnWidth==0.0F && secondColumnWidth!=0.0F && thirdColumnWidth!=0.0F)) {
            output= new float [2+numberOfOtherColumns];
            firstOtherColumn=2;
            if (firstColumnWidth!=0.0F) {
                output[0]=firstColumnWidth;
                if (secondColumnWidth!=0.0F) output[1]=secondColumnWidth;
                else output[1]=thirdColumnWidth;
            }
            else { 
                output[0]=secondColumnWidth;
                output[1]=thirdColumnWidth;
            }
        }    
        if ((firstColumnWidth!=0.0F && secondColumnWidth==0.0F && thirdColumnWidth==0.0F)||(firstColumnWidth==0.0F && secondColumnWidth!=0.0F && thirdColumnWidth==0.0F)||(firstColumnWidth==0.0F && secondColumnWidth==0.0F && thirdColumnWidth!=0.0F)) {
            output= new float [1+numberOfOtherColumns];
            firstOtherColumn=1;
            if (firstColumnWidth!=0.0F) {
                output[0]=firstColumnWidth;
            }
            else { 
                if (secondColumnWidth!=0.0F) output[0]=secondColumnWidth;
                else output[0]=thirdColumnWidth;
            }
        }
        if (firstColumnWidth==0.0F && secondColumnWidth==0.0F && thirdColumnWidth==0.0F) {
            output= new float [numberOfOtherColumns];
            firstOtherColumn=0;
        }
        float otherWidth= (float) Math.round((totalWidth-firstColumnWidth-secondColumnWidth-thirdColumnWidth)/numberOfOtherColumns);
        for (int i=firstOtherColumn; i<output.length; i++) output[i]=otherWidth;
        return (output);
    }
}
