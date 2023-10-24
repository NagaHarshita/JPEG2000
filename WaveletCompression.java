import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

class WaveletCompression{

    private JFrame frame;   
    private JLabel imageLabel;
	int height = 512;
    int width = 512;

    static class ImageProcessor {
		String path;

		BufferedImage image;
        BufferedImage ycrcbImage;
		int[][] Y;
		int[][] Cr;
		int[][] Cb;
	
		ImageProcessor(String path, int width, int height){
			String[] paths = path.split("/");
			this.path = paths[paths.length - 1];

			this.Y = new int[width][height];
			this.Cr = new int[width][height];
			this.Cb = new int[width][height];

			this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            this.ycrcbImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			this.readImageRGB(width, height, path, this.image);
            this.readImageYCrCb(this.image);
		}

		ImageProcessor(int width, int height){
			this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			this.ycrcbImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		}
	
		private void saveImg(String filename){
			try {
				File outputFile = new File(filename); // Replace with your desired output file path and format
				ImageIO.write(this.image, "png", outputFile); // Use "png" or other image formats as needed
				System.out.println("Image saved successfully.");
			} catch (IOException e) {
				System.err.println("Error: " + e.getMessage());
			}
		}

		private void yCrCb(int[][] newY){
			System.out.println(this.ycrcbImage.getWidth() + " " + this.ycrcbImage.getHeight());
			for(int y = 0; y < newY[0].length; y++){
				for(int x = 0; x < newY.length; x++){
					byte yByte = (byte) newY[x][y];
					int ycrcbPixel = ((yByte & 0xFF));
					this.ycrcbImage.setRGB(x, y, ycrcbPixel);
				}
			}
		}

        private void readImageYCrCb(BufferedImage img){

            for(int y = 0; y < 512; y++){
				for(int x = 0; x < 512; x++){
                    int rgb = img.getRGB(x, y);
		
				    float red = (rgb >> 16) & 0xff;
				    float green = (rgb >> 8) & 0xff;
				    float blue = (rgb)&0xff;      
					int yValue=255, crValue=255, cbValue=255;
				
					// yValue = (int)( 0.299 * red + 0.587 * green + 0.114 * blue);
					// crValue = (int)(-0.16874 * red - 0.33126 * green + 0.50000 * blue) + 128;
					// cbValue = (int)( 0.50000 * red - 0.41869 * green - 0.08131 * blue) + 128;
					yValue = (int)red;
					crValue = (int)green;
					cbValue = (int)blue;
					this.Y[x][y] = yValue;
					this.Cr[x][y] = crValue;
					this.Cb[x][y] = cbValue;

                    byte yByte = (byte) yValue;
                    byte crByte = (byte) crValue;
                    byte cbByte = (byte) cbValue;
	

                    int ycrcbPixel = ((yByte & 0xFF) << 16) | ((crByte & 0xFF) << 8) | ((cbByte & 0xFF));
					
                    this.ycrcbImage.setRGB(x, y, ycrcbPixel);
                }
            }
        }

		private void readImageRGB(int width, int height, String imgPath, BufferedImage img){
			try{
				int frameLength = width*height*3;
				File file = new File(imgPath);
				RandomAccessFile raf = new RandomAccessFile(file, "r");
				raf.seek(0);
	
				long len = frameLength;
				byte[] bytes = new byte[(int) len];
				raf.read(bytes);
	
				int ind = 0;
	
				for(int y = 0; y < height; y++){
					for(int x = 0; x < width; x++){
						byte a = 0;
						byte r = bytes[ind];
						byte g = bytes[ind+height*width];
						byte b = bytes[ind+height*width*2]; 
						int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
						img.setRGB(x,y,pix);
						ind++;
					}
				}
			}
			catch (FileNotFoundException e) 
			{
				// e.printStackTrace();
				System.out.println("File not found, please input the correct path to the image");
			} 
			catch (IOException e) 
			{
				// e.printStackTrace();
				System.out.println("Please input the correct parameters");
			}
		}
	}

	public int[][] passFilter(int[][] values, boolean isLowpass, boolean forRow){
		int w, h;
		if(!forRow){
			h = values[0].length / 2;
			w = values.length;
		}else{
			h = values[0].length;
			w = values.length / 2;
		}

		int[][] result = new int[w][h];
		int filter = isLowpass ? 1 : -1;

		for(int i=0;i<w;i++){
			for(int j=0;j<h;j++){
				if(forRow){
					result[i][j] = (values[2*i][j] +  filter * values[2*i+1][j]) / 2;
				}else{
					result[i][j] = (values[i][2*j] + filter * values[i][2*j+1]) / 2;
				}	
			}
		}
		return result;
	}

	public int[][] applyDWT(int[][] image, int levels) {
		int height = image.length;
		int width = image[0].length;
		int[][] transformedImage = new int[height][width];
	
		int[][] currentImage = image;
	
		for (int level = 1; level <= levels; level++) {
	
			int subWidth = width / (int) Math.pow(2, level); // w/2
			int subHeight = height / (int) Math.pow(2, level); // h/2

			int[][] lowPass = passFilter(currentImage, true, true); 
			int[][] highPass = passFilter(currentImage, false, true); 
	
			int[][] LL, LH, HL, HH;

			LL = passFilter(lowPass, true, false);
			LH = passFilter(lowPass, false, false);
			HL = passFilter(highPass, true, false);
			HH = passFilter(highPass, false, false);

			// Column transformation
			for (int i = 0; i < subWidth; i++) {
				for (int j = 0; j < subHeight; j++) {
					transformedImage[i][j] = LL[i][j];
					transformedImage[i][j + subWidth] = HL[i][j];
					transformedImage[i  + subHeight][j] = LH[i][j];
					transformedImage[i + subHeight][j + subWidth] = HH[i][j];
				}
			}
	
			// Update the currentImage for the next level
			currentImage = LL;
		}

		ImageProcessor dwt1 = new ImageProcessor(transformedImage[0].length, transformedImage.length);
		dwt1.yCrCb(transformedImage);
		this.showIms(dwt1.ycrcbImage);
	
		return transformedImage;
	}

	public int[][] inverseDWT(int[][] transformedImage, int levels) {
		int width = transformedImage.length;
		int height = transformedImage[0].length;
	
		int[][] currentImage = transformedImage;
	
		for (int level = levels; level >= 1; level--) {
			int subWidth = width / (int) Math.pow(2, level);
			int subHeight = height / (int) Math.pow(2, level);
	
			int[][] LL = new int[subWidth][subHeight];
			int[][] LH = new int[subWidth][subHeight];
			int[][] HL = new int[subWidth][subHeight];
			int[][] HH = new int[subWidth][subHeight];
	
			// Extract LL, LH, HL, and HH sub-bands from the currentImage
			for (int i = 0; i < subWidth; i++) {
				for (int j = 0; j < subHeight; j++) {
					LL[i][j] = currentImage[i][j];
					LH[i][j] = currentImage[i][j + subHeight];
					HL[i][j] = currentImage[i + subWidth][j];
					HH[i][j] = currentImage[i + subWidth][j + subHeight];
				}
			}
	
			// int[][] lowPass = passFilter(LL, true, false);
			// int[][] highPass = passFilter(LL, false, false);
	
			// Combine lowPass and highPass to get the previous level image
			int[][] previousLevelImage = new int[2 * subWidth][2 * subHeight];
			for (int i = 0; i < subWidth; i+=2) {
				for (int j = 0; j < subHeight; j++) {
					previousLevelImage[i][j] = LL[i][j] + LH[i+subWidth][j];
				}
			}
	
			// Update the currentImage for the next level
			currentImage = previousLevelImage;
		}
	

		ImageProcessor dwt1 = new ImageProcessor(currentImage[0].length, currentImage.length);
		dwt1.yCrCb(currentImage);
		this.showIms(dwt1.ycrcbImage);
	
		return currentImage;
	}
	

	// public int[][] inverseDWT(int[][] dwt){
	// 	int width = dwt.length;
	// 	int height = dwt[0].length;
	// 	int[][] transformedImage = new int[width][height];
	// 	int[][] currentImage = transformedImage;
		
	// 	for(int level=1;level<=9;level++){
	// 		int w = (int) Math.pow(2, level);
	// 		int h = w;
	// 		int[][] img = new int[h][w];
	// 		int[][] LL = new int[h/2][w/2];
	// 		int[][] HL = new int[h/2][w/2];
	// 		int[][] LH = new int[h/2][w/2];
	// 		int[][] HH = new int[h/2][w/2];

	// 		for(int i=0;i<w/2;i++){
	// 			for(int j=0;j<h/2;j++){
	// 				LL[i][j] = dwt[i][j];
	// 				HL[i][j] = dwt[i+w/2][j];
	// 				LH[i][j] = dwt[i][j+h/2];
	// 				HH[i][j] = dwt[i+w/2][j+h/2];
 	// 			}
	// 		}
			

	// 		int[][] lowPass = passFilter(LL, true, false);
	// 		int[][] highPass = passFilter(LL, false, false);

	// 		int[][] previousLevelImage = new int[w][h];
	// 		for (int i = 0; i < w/2; i++) {
	// 			for (int j = 0; j < h/2; j++) {
	// 				previousLevelImage[i][j] = lowPass[i][j];
	// 				previousLevelImage[i][j + h/2] = highPass[i][j];
	// 				previousLevelImage[i + w/2][j] = LH[i][j];
	// 				previousLevelImage[i + w/2][j + h/2] = HH[i][j];
	// 			}
	// 		}

	// 		// Update the currentImage for the next level
	// 		currentImage = previousLevelImage;
	// 	}
	// 		// for(int i=0;i<w/2;i++){
	// 		// 	for(int j=0;j<h/2;j++){
	// 		// 		transformedImage[i][j] = LL[i][j] + LH[i][j];
	// 		// 		transformedImage[i+1][j] = LL[i][j] - LH[i][j];
	// 		// 	}
	// 		// }


	// 	return transformedImage;
	// }


    public void showIms(BufferedImage image){
        frame = new JFrame("Show Image");
        
        GridBagLayout gLayout = new GridBagLayout();
        frame.getContentPane().setLayout(gLayout);

        imageLabel = new JLabel(new ImageIcon(image));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.weightx = 0.5;
        c.gridx = 10;
        c.gridy = 10;

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 1;
        
        frame.getContentPane().add(imageLabel, c);
        frame.pack();
        frame.setVisible(true);
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0); // Exit the program
			}
		});
    }

    public static void main(String[] args) {
        WaveletCompression jpeg2000 = new WaveletCompression();
		int level = Integer.parseInt(args[1]);

        ImageProcessor rose = new ImageProcessor(args[0], jpeg2000.width, jpeg2000.height);
		int[][] img = jpeg2000.applyDWT(rose.Y, 9);

		// jpeg2000.inverseDWT(img, 2);
    }
}