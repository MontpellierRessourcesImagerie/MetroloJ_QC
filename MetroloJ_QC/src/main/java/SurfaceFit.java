import java.awt.*;
import ij.*;
import ij.gui.*;
import ij.plugin.filter.*;
import ij.process.*;
import ij.measure.*;

// "Jama.jar" should be placed in the /ImageJ/Plugins directory
import Jama.Matrix;

/********************************************************************************
 *      This ImageJ plugin calculates a polynomial surface fit of an image.
 *      It's equivalent to applying a low-spatial-frequency filter to the image.
 *
 *      Dependencies:
 *      1) Requires JAMA.JAR for matrix calculations
 *
 *      Input:
 *      1) Monochrome image.  If the image is a Stack the plugin
 *         fits the average stack frame.
 *      2) The User is asked to select the fitting polynomial order:
 *         currently limited to 0 thru 9th-order.
 *
 *      Output:
 *      1) A new image representing the surface fit
 *
 *      Warnings:
 *      1) Outlier pixels can adversely affect the fit so I recommend
 *         removing severe outliers prior to using this plugin.
 *      2) Using too high a fitting polynomial order will lead to poor fitting.
 * 
 *      Author = Dwight Urban Bartholomew
 *               L3 Communications Infrared Products
 *               February 2014
 ********************************************************************************/
public class SurfaceFit implements PlugInFilter {
    static int PolyOrderX;
    static int PolyOrderY;
    static boolean bSecondTime;
    int iX;
    int iY;
    int Ny;
    int Nx;
    int TheWidth, TheHeight;
    ImagePlus image;
    Rectangle roi;
    double dtemp;
    double ytemp;
    String AStr;

    // Called by ImageJ when the filter is loaded
    public int setup(String arg, ImagePlus imp) {
        this.image = imp;
        return DOES_8G+DOES_16+DOES_32+NO_CHANGES;
    }

    // Called by ImageJ to process the image
    public void run(ImageProcessor ip) {
        int c, p, t, iy, ix, cSlice;
        int powy, powx;

        roi = ip.getRoi();
        Ny = roi.height;
        Nx = roi.width;
        TheWidth = image.getWidth();
        TheHeight = image.getHeight();
        
        // First time through, default to a cubic fit
        if( !bSecondTime ) {
            PolyOrderX = 3;
            PolyOrderY = 3;
            bSecondTime = true;
        }
        
        // Ask User what order of polynomial to use for the surface fit
        if( !getPolyOrder() ) return;

        //#################################################
        //Place the ROI into 2D array
        cSlice = image.getCurrentSlice(); // remember current slice
        double[][] TheImage = new double[Ny][Nx];
        for(iy=0; iy<Ny; iy++) {
            for(ix=0; ix<Nx; ix++) {
                dtemp = ip.getPixelValue((ix+iX),(iy+iY));
                TheImage[iy][ix] += dtemp;
            }
        }
        
        //#################################################
        //Remove mean value to keep the math from blowing up
        double TheImage_mean = Mean2D(TheImage);
        for(iy=0; iy<Ny; iy++) {
            for(ix=0; ix<Nx; ix++) {
                TheImage[iy][ix] -= TheImage_mean;
        	}
        }

        //#################################################
        // Surface Fit of the Region Of Interest (ROI)
        IJ.showStatus("Performing Surface Fit");
        // Perform a 2D Least Squares Surface Fit of the average image
        double[][] SurfFit = SurfaceFit(TheImage);

        // Create an image of the fitted surface
        // Example:                
        //    dtemp = (SurfFit[3][3]*y*y*y + SurfFit[2][3]*y*y + SurfFit[1][3]*y + SurfFit[0][3])*x*x*x;
        //    dtemp += (SurfFit[3][2]*y*y*y + SurfFit[2][2]*y*y + SurfFit[1][2]*y + SurfFit[0][2])*x*x;
        //    dtemp += (SurfFit[3][1]*y*y*y + SurfFit[2][1]*y*y + SurfFit[1][1]*y + SurfFit[0][1])*x;
        //    dtemp += (SurfFit[3][0]*y*y*y + SurfFit[2][0]*y*y + SurfFit[1][0]*y + SurfFit[0][0]);
        double[][] Svh = new double[Ny][Nx];
        for(iy=0; iy<Ny; iy++) {
            for(ix=0; ix<Nx; ix++) {
                dtemp = 0;
                // Determine the value of the fit at pixel iy,ix
                for(powx=PolyOrderX; powx>=0; powx--) {
                    ytemp = 0;
                    for(powy=PolyOrderY; powy>=0; powy--) {
                        ytemp += SurfFit[powy][powx] * Math.pow((double)iy,(double)powy);
                    }
                    dtemp += ytemp * Math.pow((double)ix,(double)powx);
                }
                // Remember to add back the mean image value
                Svh[iy][ix] = dtemp + TheImage_mean;
            }
        }

        // Display the surface fit of the original image.
        AStr = " (Surface Fit x = "+IJ.d2s(PolyOrderX,0)+PostFix(PolyOrderX)+" and y = "+IJ.d2s(PolyOrderY,0)+PostFix(PolyOrderY)+")";
        ImagePlus TheFit_imp = NewImage.createFloatImage(image.getTitle()+AStr, Nx, Ny,1, NewImage.FILL_BLACK);
        ImageProcessor TheFit_ip = TheFit_imp.getProcessor();
        TheFit_imp.setSlice(1);
        double dvalue;
        for(iy=0; iy<Ny; iy++) {
            for(ix=0; ix<Nx; ix++) {
                dvalue = Svh[iy][ix];
                TheFit_ip.putPixelValue(ix,iy,dvalue);
            }
        }
        autoAdjust(TheFit_imp, TheFit_ip);
        TheFit_imp.show();
        TheFit_imp.updateAndDraw();

        IJ.showStatus("Polynomial Surface Fit: Complete");
        //IJ.register(Polynomial_Surface_Fit.class);
    }


// THE FOLLOWING WAS LIFTED FROM A PLUGIN BY TERRY WU at
// http://rsb.info.nih.gov/ij/plugins/download/Background_Correction_.java
//    The following autoAjust() method is a slightly modified version of the autoAjust()
//    from ij.plugin.frame.ContrastAdjuster by Wayne Rasband <wayne@codon.nih.gov>
//    author = Terry Wu, Ph.D.
  void autoAdjust(ImagePlus imp, ImageProcessor ip){
    double min, max;
    Calibration cal = imp.getCalibration();
    imp.setCalibration(null);
    ImageStatistics stats = imp.getStatistics();
    imp.setCalibration(cal);
    int[] histogram = stats.histogram;
    int threshold = stats.pixelCount/5000;
    int i = -1;
    boolean found = false;
    do {
      i++;
      found = histogram[i] > threshold;
    } while (!found && i<255);
    int hmin = i;
    i = 256;
    do {
      i--;
      found = histogram[i] > threshold;
    } while (!found && i>0);
    int hmax = i;
    if (hmax>hmin){
      imp.killRoi();
      min = stats.histMin+hmin*stats.binSize;
      max = stats.histMin+hmax*stats.binSize;
      ip.setMinAndMax(min, max);
    }
  }


//###########################################################################
//  Creates a dialog box to input the Order of the Polynomial Fit
boolean getPolyOrder() {
    int i;
    String [] FitOrders = new String[10];
    for(i=0; i<10; i++) FitOrders[i] = IJ.d2s(i,0);

    GenericDialog gd = new GenericDialog("Polynomial Surface Fit", IJ.getInstance());
    gd.addMessage("Polynomial Orders for the Surface Fit");

    gd.addChoice("Order for X (horizontal direction)", FitOrders, FitOrders[PolyOrderX]);
    gd.addChoice("Order for Y (vertical direction)", FitOrders, FitOrders[PolyOrderY]);

    gd.showDialog();

    if (gd.wasCanceled()) return(false);

    PolyOrderX = gd.getNextChoiceIndex();
    PolyOrderY = gd.getNextChoiceIndex();
    
    return(true);
}
  
//****************************************
// Calculates the mean of an 1D array
double Mean1D ( double[] data )
  {
    int n = data.length;
    if ( n < 1 ) return Double.NaN;
    double avg = data[0];
    double sum = 0;
    for ( int i = 1; i < data.length; i++ ) {
        sum += data[i];
    }
    return ( sum/n );
  }

//****************************************
// Calculates the mean of a 2D array.
double Mean2D ( double[][] data )
  {
    int i1=0, i2=0;
    int N1 = data.length;
    int N2 = data[0].length;
    if ( N1*N2 < 1 ) return Double.NaN;
    double sum=0;
    for(i1=0; i1<N1; i1++) {
        for(i2=0; i2<N2; i2++) {
            sum += data[i1][i2];
        }
    }
    return ( sum / (N1*N2) );
  }

//****************************************
// Create a suffix for a number
String PostFix ( int N )
  {
    if( N==1 )
        return("st");
    else if( N==2 )
        return("nd");
    else if( N==3 )
        return("rd");
    else
        return("th");
  }

//**************************************************************************
// This section computes surface fit coefficients for z.
// For example, if the surface fit is quadratic along the vertical
// and cubic along the horizontal than
// z = (p0 y^2 + p1 y^1 + p2 y^0) x^3
//      + (p3 y^2 + p4 y^1 + p5 y^0) x^2
//      + (p6 y^2 + p7 y^1 + p8 y^0) x^1
//      + (p9 y^2 + p10 y^1 + p11 y^0)x^0
// where:   x = column index
//          y = row index
//          z = image intensity
// Problem expressed in matrix form as [XY]*[P] = [Z].
// Solve for the [P] coefficients using a least squares method
// and written in matrix notation as [P] = inv[XY]*[Z].
// Reference = see, for example, "Numerical Methods in C", by Press, 
//             Teukolsky, Vetterling, & Flannery, 2nd Edition, page 671
double[][] SurfaceFit( double[][] TheImage )
{
    int Nrows = TheImage.length;
    int Ncols = TheImage[0].length;
    int Npixels = Nrows*Ncols;
    int n=0;
    int r, c, cnt, i, j, k, MatVal, nCol;
    int Dim1, Dim2;
    int PO_2xp1 = Math.max((2 * PolyOrderX + 1), (2 * PolyOrderY + 1));
    int MatSize = (PolyOrderX+1)*(PolyOrderY+1);

    // Create the x, y, and z arrays from which the image to be fitted
    double []X = new double[Npixels];
    double []Y = new double[Npixels];
    double []Z = new double[Npixels];
    cnt = 0;
    for(r=0; r<Nrows; r++) {
        for(c=0; c<Ncols; c++) {
            X[cnt] = c;
            Y[cnt] = r;
            Z[cnt] = TheImage[r][c];
            cnt++;
        }
    }

    // Notation:
    //  1)  The matrix [XY] is made up of sums (over all the pixels) of the
    //      row & column indices raised to various powers.  For example,
    //      sum( y^3 * x^2 ).  It turns out, within [XY] there are 
    //      patterns to the powers and these patterns are computed
    //      in the matrices [nnx] and [nny].
    //  2)  [Sxyz] represents all of the possible sums that will be used to
    //      create [XY] and [Z].  We compute all of these sums even though 
    //      some of them might not be utilized... it's just easier.
	double [][]XY_mat = new double[MatSize][MatSize];
    int [][]nnx = new int[MatSize][MatSize];
    int [][]nny = new int[MatSize][MatSize];
    int []aRow = new int[MatSize];

    // Create all the possible sums, Sxyz[][][]
    IJ.showProgress(1,6);
    IJ.showStatus("Preparing sums matrix");
    double[][][] Sxyz = new double[PO_2xp1][PO_2xp1][2];
    double x, y, z;
    double powx, powy, powz;
    int nx, ny, nz;
    // Initialize all of the sums to zero
    for(nx=0; nx<PO_2xp1; nx++) {
        for(ny=0; ny<PO_2xp1; ny++) {
            for(nz=0; nz<2; nz++) {
                Sxyz[nx][ny][nz] = 0.0;
            }
        }
    }
    // Produce the sums
    for( i=0; i<Npixels; i++) {
        x = X[i]; y = Y[i]; z = Z[i];
        for(nx=0; nx<PO_2xp1; nx++) {
            powx = java.lang.Math.pow(x,(double)nx);
            for(ny=0; ny<PO_2xp1; ny++) {
                powy = java.lang.Math.pow(y,(double)ny);
                for(nz=0; nz<2; nz++) {
                    powz = java.lang.Math.pow(z,(double)nz);
                    Sxyz[nx][ny][nz] += powx * powy * powz;
                }
            }
        }
    }

    // Create the patterns of "powers" for the X (horizontal) pixel indices
    IJ.showProgress(2,6);
    int iStart = 2 * PolyOrderX;
    Dim1 = 0;
    while(Dim1<MatSize) {
        for(i=0; i<(PolyOrderY+1); i++) {
            // A row of nnx[][] consists of an integer that starts with a value iStart and
            //  1) is repeated (PolyOrderX+1) times
            //  2) decremented by 1
            //  3) Repeat steps 1 and 2 for a total of (PolyOrderY+1) times
            nCol = 0;
            for(j=0; j<(PolyOrderX+1); j++ ) {
                for(k=0; k<(PolyOrderY+1); k++) {
                    aRow[nCol] = iStart - j;
                    nCol++;
                }
            }
            // Place this row into the nnx matrix
            for(Dim2=0; Dim2<MatSize; Dim2++ ) {
                nnx[Dim1][Dim2] = aRow[Dim2];
            }
            Dim1++;
        }
        iStart--;
    }
    
    // Create the patterns of "powers" for the Y (vertical) pixel indices
    IJ.showProgress(3,6);
    Dim1 = 0;
    while(Dim1<MatSize) {
        iStart = 2 * PolyOrderY;
        for(i=0; i<(PolyOrderY+1); i++) {
            // A row of nny[][] consists of an integer that starts with a value iStart and
            //  1) place in matrix
            //  2) decremented by 1
            //  3) 1 thru 2 are repeated for a total of (PolyOrderX+1) times
            //  4) 1 thru 3 are repeat a total of (PolyOrderY+1) times
            nCol = 0;
            for(j=0; j<(PolyOrderX+1); j++ ) {
                for(k=0; k<(PolyOrderY+1); k++) {
                    aRow[nCol] = iStart - k;
                    nCol++;
                }
            }
            // Place this row into the nnx matrix
            for(Dim2=0; Dim2<MatSize; Dim2++ ) {
                nny[Dim1][Dim2] = aRow[Dim2];
            }
            Dim1++;
            iStart--;
        }
    }

    // Put together the [XY] matrix
	for(r=0; r<MatSize; r++) {
		for(c=0; c<MatSize; c++) {
			nx = nnx[r][c];
			ny = nny[r][c];
			XY_mat[r][c] = Sxyz[nx][ny][0];
		}
	}

    // Put together the [Z] vector
    IJ.showProgress(4,6);
	double[] Z_mat = new double[MatSize];
    c = 0;
    for(i=PolyOrderX; i>=0; i--) {
		for(j=PolyOrderY; j>=0; j--) {
            Z_mat[c] = Sxyz[i][j][1];
            c++;
        }
    }

    // Solve the linear system [XY] [P] = [Z] using the Jama.Matrix routines
	// 	[A_mat] [x_vec] = [b_vec]
	// (see example at   http://math.nist.gov/javanumerics/jama/doc/Jama/Matrix.html)
    IJ.showProgress(5,6);
    IJ.showStatus("Solving linear system of equations");
	Matrix A_mat = new Matrix(XY_mat);
	Matrix b_vec = new Matrix(Z_mat, MatSize);
	Matrix x_vec = A_mat.solve(b_vec);

	// Place the Least Squares Fit results into the array Pfit
	double[] Pfit = new double[MatSize];
	for(i=0; i<MatSize; i++) {
		Pfit[i] = x_vec.get(i, 0);
	}

	// Reformat the results into a 2-D array where the array indices
    // specify the power of pixel indices.  For example,
    // z =    (G[2][3] y^2 + G[1][3] y^1 + G[0][3] y^0) x^3
    //      + (G[2][2] y^2 + G[1][2] y^1 + G[0][2] y^0) x^2
    //      + (G[2][1] y^2 + G[1][1] y^1 + G[0][1] y^0) x^1
    //      + (G[2][0] y^2 + G[1][0] y^1 + G[0][0] y^0) x^0
    double[][] Gfit = new double[PolyOrderY + 1][PolyOrderX + 1];
    c = 0;
    for(i=PolyOrderX; i>=0; i--) {
		for(j=PolyOrderY; j>=0; j--) {
            Gfit[j][i] = Pfit[c];
            c++;
        }
    }
    return ( Gfit );
}


}   // End of the CLASS


