import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.event.*;

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
			for(int y = 0; y < newY.length; y++){
				for(int x = 0; x < newY[0].length; x++){
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
		h = values.length;
		w = values[0].length;

		if(forRow){
			w = w/2;
		}else{
			h=h/2;
		}

		int[][] result = new int[h][w];
		int filter = isLowpass ? 1 : -1;

		for(int i=0;i<h;i++){
			for(int j=0;j<w;j++){
				if(forRow){
					result[i][j] = (values[i][2*j] + filter * values[i][2*j+1]) / 2;
				}else{
					result[i][j] = (values[2*i][j] +  filter * values[2*i+1][j]) / 2;
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
			for (int i = 0; i < subHeight; i++) {
				for (int j = 0; j < subWidth; j++) {
					transformedImage[i][j] = LL[i][j];
					transformedImage[i][j + subWidth] = HL[i][j];
					transformedImage[i  + subHeight][j] = LH[i][j];
					transformedImage[i + subHeight][j + subWidth] = HH[i][j];
				}
			}
	
			// Update the currentImage for the next level
			currentImage = LL;
		}

		// ImageProcessor dwt1 = new ImageProcessor(transformedImage[0].length, transformedImage.length);
		// dwt1.yCrCb(transformedImage);
		// this.showIms(dwt1.ycrcbImage);
	
		return transformedImage;
	}

	public int[][] inverseDWT(int[][] transformedImage, int levels) {
		// int wk = transformedImage.length;
		// int hk = transformedImage[0].length;
	
		int[][] currentImage = transformedImage;
		int[][] previousLevelImage = new int[height][width];


		for (int level = 1; level <= levels; level++) {
			int w = (int) Math.pow(2, level);
			int h = (int) Math.pow(2, level);

			int subWidth = w/2;
			int subHeight = h/2;
	
			int[][] LL = new int[subHeight][subWidth];
			int[][] LH = new int[subHeight][subWidth];
			int[][] HL = new int[subHeight][subWidth];
			int[][] HH = new int[subHeight][subWidth];

			// Extract LL, LH, HL, and HH sub-bands from the currentImage
			for (int i = 0; i < subHeight; i++) {
				for (int j = 0; j < subWidth; j++) {
					LL[i][j] = currentImage[i][j];
					LH[i][j] = currentImage[i + subHeight][j];
					HL[i][j] = currentImage[i][j + subWidth];
					HH[i][j] = currentImage[i + subWidth][j + subHeight];
				}
			}

			previousLevelImage = new int[2 * subWidth][2 * subHeight];

			// rows 
			for (int i = 0; i < 2 * subHeight; i++) {
				for (int j = 0; j < subWidth; j++) {
					if(i%2==0){
						previousLevelImage[i][j] = LL[i/2][j] + LH[i/2][j];
					}else{
						previousLevelImage[i][j] = LL[i/2][j] - LH[i/2][j];
					}
				}

				for (int j = subWidth; j < 2 * subWidth; j++) {
					if(i%2==0){
						previousLevelImage[i][j] = HL[i/2][j-subWidth] + HH[i/2][j - subWidth];
					}else{
						previousLevelImage[i][j] = HL[i/2][j-subWidth] - HH[i/2][j- subWidth];
					}
				}
			}

			for (int i = 0; i < subHeight; i++) {
				for (int j = 0; j < subWidth; j++) {
					LL[i][j] = previousLevelImage[i][j];
					LH[i][j] = previousLevelImage[i + subHeight][j];
					HL[i][j] = previousLevelImage[i][j + subWidth];
					HH[i][j] = previousLevelImage[i + subHeight][j + subWidth];
				}
			}


			// columns
			for (int j = 0; j < 2 * subWidth; j++) {
				for (int i = 0; i < subHeight; i++) {
					if(j%2==0){
						previousLevelImage[i][j] = LL[i][j/2] + HL[i][j/2];
					}else{
						previousLevelImage[i][j] = LL[i][j/2] - HL[i][j/2];
					}
				}

				for (int i = subHeight; i < 2 * subHeight; i++) {
					if(j%2==0){
						previousLevelImage[i][j] = LH[i-subHeight][j/2] + HH[i-subHeight][j/2];
					}else{
						previousLevelImage[i][j] = LH[i-subHeight][j/2] - HH[i-subHeight][j/2];
					}
				}
			}

			for(int i=0;i<h;i++){
				for(int j=0;j<w;j++){
					currentImage[i][j] = previousLevelImage[i][j];
				}
			}
		}

		ImageProcessor dwt1 = new ImageProcessor(currentImage[0].length, currentImage.length);
		dwt1.yCrCb(currentImage);
		this.showIms(dwt1.ycrcbImage);
	
		return currentImage;
	}
	


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

		try {
			Timer timer = new Timer(5000, new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					frame.dispose(); // Close the window after 5 seconds
				}
			});
			timer.setRepeats(false); // Set the timer to run only once
			timer.start();
			Thread.sleep(5000);

		}catch (Exception e) {
           
            // catching the exception
            System.out.println(e);
        }
    }

	public int[][] zeroCoef(int[][] img, int level){
		int zeroW = (int)Math.pow(2, level);
		int zeroH = (int)Math.pow(2, level);

		int[][] dwt = new int[height][width];
		for(int i=0;i<height;i++){
			for(int j=0;j<width;j++){
				if(i<zeroH && j<zeroW){
					dwt[i][j] = img[i][j];
				}else{
					dwt[i][j] = 0;
				}
			}
		}

		return dwt;
	}

    public static void main(String[] args) {
        WaveletCompression jpeg2000 = new WaveletCompression();
		int level = Integer.parseInt(args[1]);
        ImageProcessor rose = new ImageProcessor(args[0], jpeg2000.width, jpeg2000.height);

		int[][] img = jpeg2000.applyDWT(rose.Y, 9);

		System.out.println("Compression Complete");

		if(level > 0){
			int[][] dwt = jpeg2000.zeroCoef(img, level);
			int[][] dec = jpeg2000.inverseDWT(dwt, 9);
		}

		if(level == -1){
			for(int i=1;i<=9;i++){
				int[][] dwt = jpeg2000.zeroCoef(img, i);
				int[][] dec = jpeg2000.inverseDWT(dwt, 9);
			}
		}

    }
}