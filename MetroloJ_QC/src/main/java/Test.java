import ij.ImageJ;
import java.io.File;
import loci.plugins.LociImporter;

public class Test {
  public static void main(String[] args) {
    System.getProperties().setProperty("plugins.dir", System.getProperty("user.dir") + File.separator + "src" + File.separator);
    ImageJ ij = new ImageJ();
    String path = "d:\\Users\\Julien Cau\\Desktop\\Maintenance\\PSF\\4C\\40X.czi";
    String params = "location=[Local machine] windowless=true groupFiles=falseopen=[" + path + "] ";
    (new LociImporter()).run(params);
    ij.exitWhenQuitting(true);
  }
}
