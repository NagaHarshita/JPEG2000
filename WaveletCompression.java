import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.awt.event.*;

class WaveletCompression{

    private JFrame frame;   
    private JLabel imageLabel;
	int height = 512;
    int width = 512;

    static class ImageProcessor {
		String path;

		BufferedImage image;
		float[][] R;
		float[][] G;
		float[][] B;
	
		ImageProcessor(String path, int width, int height){
			String[] paths = path.split("/");
			this.path = paths[paths.length - 1];

			this.R = new float[width][height];
			this.G = new float[width][height];
			this.B = new float[width][height];

			this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			this.readImageRGB(width, height, path, this.image);
		}

		ImageProcessor(int width, int height){
			this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		}

		private void setImageRGB(float[][] newR, float[][] newG, float[][] newB){
			this.R = newR;
			this.G = newG;
			this.B = newB;
			for(int y = 0; y < newR.length; y++){
				for(int x = 0; x < newR[0].length; x++){
					byte rByte = (byte) (int)newR[x][y];
					byte gByte = (byte) (int)newG[x][y];
					byte bByte = (byte) (int)newB[x][y];
					float rgbPixel = 0xff000000 | ((rByte & 0xff) << 16) | ((gByte & 0xff) << 8) | ((bByte & 0xff));
					this.image.setRGB(x, y, (int)rgbPixel);
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
						byte r = bytes[ind];
						byte g = bytes[ind+height*width];
						byte b = bytes[ind+height*width*2]; 
						int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
						img.setRGB(x,y,pix);
						Color c = new Color(image.getRGB(x,y));
						this.R[x][y] = c.getRed();
						this.G[x][y] = c.getGreen();
						this.B[x][y] =  c.getBlue();
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

	public float[][] passFilter(float[][] values, boolean isLowpass, boolean forRow){
		int w, h;
		h = values.length;
		w = values[0].length;

		if(forRow){
			w = w/2;
		}else{
			h=h/2;
		}

		float[][] result = new float[h][w];
		float filter = isLowpass ? 1 : -1;

		for(int i=0;i<h;i++){
			for(int j=0;j<w;j++){
				if(forRow){
					result[i][j] = (float)((values[i][2*j] + filter * values[i][2*j+1]) / (float)2);
				}else{
					result[i][j] = (float)((values[2*i][j] +  filter * values[2*i+1][j]) / (float)2);
				}	
			}
		}
		return result;
	}

	public float[][] applyDWT(float[][] image, int levels) {
		int height = image.length;
		int width = image[0].length;
		float[][] transformedImage = new float[height][width];
	
		float[][] currentImage = image;
	
		for (int level = 1; level <= levels; level++) {
	
			int subWidth = width / (int) Math.pow(2, level); // w/2
			int subHeight = height / (int) Math.pow(2, level); // h/2

			float[][] lowPass = passFilter(currentImage, true, true); 
			float[][] highPass = passFilter(currentImage, false, true); 
	
			float[][] LL, LH, HL, HH;

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
		return transformedImage;
	}

	public float[][] inverseDWT(float[][] transformedImage, int levels) {
	
		float[][] currentImage = transformedImage;
		float[][] previousLevelImage = new float[height][width];


		for (int level = 1; level <= levels; level++) {
			int w = (int) Math.pow(2, level);
			int h = (int) Math.pow(2, level);

			int subWidth = w/2;
			int subHeight = h/2;
	
			float[][] LL = new float[subHeight][subWidth];
			float[][] LH = new float[subHeight][subWidth];
			float[][] HL = new float[subHeight][subWidth];
			float[][] HH = new float[subHeight][subWidth];

			// Extract LL, LH, HL, and HH sub-bands from the currentImage
			for (int i = 0; i < subHeight; i++) {
				for (int j = 0; j < subWidth; j++) {
					LL[i][j] = currentImage[i][j];
					LH[i][j] = currentImage[i + subHeight][j];
					HL[i][j] = currentImage[i][j + subWidth];
					HH[i][j] = currentImage[i + subHeight][j + subWidth];
				}
			}

			previousLevelImage = new float[2 * subWidth][2 * subHeight];

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

    }

	public float[][] zeroCoef(float[][] img, int level){
		int zeroW = (int)Math.pow(2, level);
		int zeroH = (int)Math.pow(2, level);

		float[][] dwt = new float[height][width];
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

	public float[][] getIDWT(float[][] img, int level){
		float[][] dwt = this.zeroCoef(img, level);
		return this.inverseDWT(dwt, 9);
	}

    public static void main(String[] args) {
        WaveletCompression jpeg2000 = new WaveletCompression();
		int level = Integer.parseInt(args[1]);
        ImageProcessor rose = new ImageProcessor(args[0], jpeg2000.width, jpeg2000.height);

		float[][] dwtR = jpeg2000.applyDWT(rose.R, 9);
		float[][] dwtG = jpeg2000.applyDWT(rose.G, 9);
		float[][] dwtB = jpeg2000.applyDWT(rose.B, 9);

		System.out.println("Compression Complete");

		float[][] idwtR, idwtG, idwtB;

		if(level > 0){
			idwtR = jpeg2000.getIDWT(dwtR, level);
			idwtG = jpeg2000.getIDWT(dwtG, level);
			idwtB = jpeg2000.getIDWT(dwtB, level);
			rose.setImageRGB(idwtR, idwtG, idwtB);
			jpeg2000.showIms(rose.image);
		}

		if(level == -1){
			for(int i=1;i<=9;i++){
				idwtR = jpeg2000.getIDWT(dwtR, i);
				idwtG = jpeg2000.getIDWT(dwtG, i);
				idwtB = jpeg2000.getIDWT(dwtB, i);
				rose.setImageRGB(idwtR, idwtG, idwtB);
				jpeg2000.showIms(rose.image);
				
				if(i!=9){
					try {
						Timer timer = new Timer(2000, new ActionListener() {
							@Override
							public void actionPerformed(ActionEvent e) {
								jpeg2000.frame.dispose(); // Close the window after 5 seconds
							}
						});
						timer.setRepeats(false); // Set the timer to run only once
						timer.start();
						Thread.sleep(2000);

					}catch (Exception e) {
						// catching the exception
						System.out.println(e);
					}
				}
			}
		}

		System.out.println("DeCompression Complete");
    }
}